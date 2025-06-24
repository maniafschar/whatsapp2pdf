package com.jq.wa2pdf.service;

import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.PatternColor;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfDocumentInfo;
import com.itextpdf.kernel.pdf.PdfOutline;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.annot.PdfLinkAnnotation;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEvent;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEventHandler;
import com.itextpdf.kernel.pdf.event.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.filespec.PdfFileSpec;
import com.itextpdf.kernel.pdf.navigation.PdfNamedDestination;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.jq.wa2pdf.entity.Ticket;
import com.jq.wa2pdf.service.ExtractService.Attributes;
import com.jq.wa2pdf.service.WordCloudService.Token;
import com.vdurmont.emoji.EmojiParser;

@Component
public class PdfService {
	public static final String filename = "wa";

	@Autowired
	private ExtractService extractService;

	@Autowired
	private ChartService chartService;

	@Autowired
	private WordCloudService wordCloudService;

	@Autowired
	private AdminService adminService;

	@Async
	public void create(final String id, final String period, final String user, final boolean preview)
			throws Exception {
		final Path error = ExtractService.getTempDir(id).resolve(PdfService.filename + "Error");
		try {
			Files.deleteIfExists(error);
			new PDF(id, period, user, preview).create();
		} catch (final Exception ex) {
			ex.printStackTrace();
			try (final FileOutputStream filename = new FileOutputStream(error.toFile())) {
				filename.write(ex.getMessage().getBytes(StandardCharsets.UTF_8));
			}
			throw ex;
		}
	}

	public Path get(final String id, final String period) throws IOException {
		final Path pdfPath = ExtractService.getTempDir(id)
				.resolve(this.getFilename(period) + ".pdf");
		if (Files.exists(pdfPath))
			return pdfPath;
		return null;
	}

	private String getFilename(final String period) {
		return filename + (period == null ? ""
				: "_" + period.replace("-\\d\\d", "").replace("/\\d\\d", "").replace("\\d\\d.", ""));
	}

	public static class Statistics {
		String user;
		String period;
		int chats = 0;
		int words = 0;
		int letters = 0;
		StringBuilder text;

		public int getChats() {
			return this.chats;
		}

		public int getWords() {
			return this.words;
		}

		public int getLetters() {
			return this.letters;
		}

		public String getUser() {
			return this.user;
		}

		public String getPeriod() {
			return this.period;
		}
	}

	private class PDF {
		private static PdfFont fontMessage;
		private final Path dir;
		private PdfWriter writer;
		private Document document;
		private final List<String> outline = new ArrayList<>();
		private final List<Statistics> total = new ArrayList<>();
		private final List<Statistics> wordClouds = new ArrayList<>();
		private final List<Table> content = new ArrayList<>();
		private final Color colorDate = PatternColor.createColorWithColorSpace(new float[] { 0.53f, 0.53f, 0.53f });
		private final Color colorChatUser = PatternColor.createColorWithColorSpace(new float[] { 0.7f, 0.9f, 1f });
		private final Color colorChatOther = PatternColor.createColorWithColorSpace(new float[] { 1f, 0.9f, 0.7f });
		private final String period;
		private final String user;
		private final String id;
		private final boolean preview;
		private final boolean groupChat;

		private PDF(final String id, final String period, final String user, final boolean preview)
				throws IOException {
			this.dir = ExtractService.getTempDir(id).toAbsolutePath();
			this.period = period;
			this.user = user;
			this.id = id;
			this.preview = preview;
			fontMessage = PdfFontFactory.createFont(StandardFonts.HELVETICA);
			this.groupChat = new ObjectMapper().readValue(
					ExtractService.getTempDir(id).resolve(PdfService.filename + "Attributes").toFile(),
					Attributes.class).getUsers().size() > 2;
		}

		private void create() throws IOException, FontFormatException, ParseException {
			final String filename = PdfService.this.getFilename(this.preview ? null : this.period);
			Files.deleteIfExists(this.dir.resolve(filename + ".tmp"));
			Files.deleteIfExists(this.dir.resolve(filename + ".pdf"));
			this.writer = new PdfWriter(
					this.dir.resolve(filename + ".tmp").toAbsolutePath().toFile().getAbsoluteFile());
			this.document = new Document(new PdfDocument(this.writer));
			this.document.getPdfDocument().addEventHandler(PdfDocumentEvent.START_PAGE,
					new AbstractPdfDocumentEventHandler() {
						@Override
						protected void onAcceptedEvent(final AbstractPdfDocumentEvent event) {
							try {
								final PdfPage page = PDF.this.document.getPdfDocument()
										.getPage(PDF.this.document.getPdfDocument().getNumberOfPages());
								final PdfCanvas canvas = new PdfCanvas(page);
								canvas.addImageFittedIntoRectangle(ImageDataFactory.create(this.getClass()
										.getResource("/image/background/000001.png").toExternalForm()),
										page.getPageSize(), false);
								if (PDF.this.document.getPdfDocument().getNumberOfPages() == 1) {
									final ImageData image = ImageDataFactory.create(this.getClass()
											.getResource("/image/heartBG.jpg").toExternalForm());
									final int height = 90;
									final Rectangle rect = page.getPageSize();
									rect.setX(0);
									rect.setY(rect.getHeight() - height);
									rect.setHeight(height);
									rect.setWidth(height * image.getWidth() / image.getHeight());
									canvas.addImageFittedIntoRectangle(image, rect, false);
								}
							} catch (final Exception e) {
								throw new RuntimeException(e);
							}
						}
					});
			this.parseChats();
			this.addMetaData();
			this.addChart();
			this.addWordCloud();
			this.writeContent();
			this.document.flush();
			this.document.close();
			Files.move(this.dir.resolve(filename + ".tmp"), this.dir.resolve(filename + ".pdf"));
		}

		private void parseChats() throws IOException {
			try (final BufferedReader chat = new BufferedReader(
					new FileReader(this.dir.resolve("_chat.txt").toFile()))) {
				final Pattern patternStart = Pattern
						.compile(
								"^.?\\[" + this.period.replaceAll("[0-9]", "\\\\d")
										+ ", \\d\\d:\\d\\d:\\d\\d\\] ([^:].*?)");
				final Pattern patternMonth = Pattern
						.compile("^.?\\[" + this.period + ", \\d\\d:\\d\\d:\\d\\d\\] ([^:].*?)");
				final Set<String> users = new HashSet<>();
				boolean foundMonth = false;
				String line, lastChat = null, user = null, date = null, time = null;
				while ((line = chat.readLine()) != null) {
					line = line.replaceAll("\u200E", "");
					if (line.trim().length() > 0 && patternStart.matcher(line).matches()) {
						final boolean inMonth = patternMonth.matcher(line).matches();
						if (foundMonth && !inMonth)
							break;
						foundMonth = inMonth;
						if (foundMonth) {
							if (lastChat != null) {
								final String s = user, d = date;
								Statistics u = this.total.stream()
										.filter(e -> e.user.equals(s) && e.period.equals(d))
										.findFirst().orElse(null);
								if (u == null) {
									u = new Statistics();
									u.user = user;
									u.period = date;
									this.total.add(u);
								}
								this.addMessage(user, time, lastChat);
								u.chats++;
								if (lastChat != null) {
									lastChat = lastChat.replaceAll("\t", " ");
									lastChat = lastChat.replaceAll("\r", " ");
									lastChat = lastChat.replaceAll("\n", " ");
									while (lastChat.indexOf("  ") > -1)
										lastChat = lastChat.replaceAll("  ", " ");
									u.words += lastChat.split(" ").length;
									u.letters += lastChat.replaceAll(" ", "").length();
								}
							}
							this.addDate(date = line.split(" ")[0].replace("[", "").replace(",", "").trim());
							user = line.substring(line.indexOf("]") + 1, line.indexOf(":", line.indexOf("]"))).trim();
							time = line.substring(line.indexOf(' '), line.indexOf(']')).trim();
							users.add(user);
							if (line.indexOf("<Anhang: ") < 0 || !line.endsWith(">"))
								lastChat = line.substring(line.indexOf(": ") + 2);
							else {
								lastChat = null;
								this.addMessage(user, time,
										line.substring(line.indexOf("<Anhang: ") + 9, line.length() - 1).trim(), true);
							}
						}
					} else if (foundMonth)
						lastChat += "\n" + line;
				}
				this.addMessage(user, time, lastChat);
				this.addDate(null);
			}
		}

		private void writeContent() throws IOException {
			int i = 0;
			final PdfOutline root = this.document.getPdfDocument().getOutlines(true);
			for (final Table e : this.content) {
				this.document.add(e);
				if (e.getNumberOfColumns() == 1 && e.getHeight() == null)
					root.addOutline(this.outline.get(i++))
							.addDestination(new PdfNamedDestination(
									this.sanitizeDestination(((Text) ((Paragraph) e.getCell(0, 0).getChildren().get(0))
											.getChildren().get(0)).getText())));
				if (this.preview && this.document.getPdfDocument().getNumberOfPages() > 4)
					break;
			}
			if (this.preview)
				this.addPreviewInfo();
		}

		private String sanitizeDestination(final String id) {
			return id.replaceAll("[\\.\\-/]", "_");
		}

		private void addPreviewInfo() {
			final Paragraph paragraph = new Paragraph();
			paragraph.setMarginTop(20);
			paragraph.setFont(fontMessage);
			paragraph.add(
					"\n\n\n\n\nThis is a preview of your chat.\nYou may download the whole chat on:\nhttps://wa2pdf.com");
			paragraph.setTextAlignment(TextAlignment.CENTER);
			this.document.add(paragraph);
		}

		private void addDate(final String date) {
			final String lastDate = this.total.size() > 0 ? this.total.get(this.total.size() - 1).period : null;
			if (lastDate != null && !lastDate.equals(date)) {
				String s = this.total.get(this.total.size() - 1).period;
				for (final Statistics statistics : this.total.stream().filter(e -> lastDate.equals(e.getPeriod()))
						.collect(Collectors.toList()))
					s += " · " + statistics.user + " " + statistics.chats;
				this.outline.add(s);
			}
			if (date != null && !date.equals(lastDate)) {
				final Cell cell = this.createCell(date, TextAlignment.CENTER);
				cell.setBackgroundColor(this.colorDate, 0.2f);

				final Table table = new Table(1);
				table.setDestination(this.sanitizeDestination(date));
				table.setWidth(UnitValue.createPercentValue(100.0f));
				table.addCell(cell);
				this.content.add(table);
				this.addEmptyLine();
			}
		}

		private void addMessage(final String user, final String time, final String message, final boolean... media) {
			final Cell cellMessage = this.createCell(message, media);

			final Cell cellTime = this.createCell(time + (this.groupChat ? " · " + user : ""));
			cellTime.setFontSize(8.5f);
			cellTime.setFontColor(this.colorDate);
			cellTime.setPaddingBottom(0);

			final Cell empty = this.createCell("");
			empty.setPadding(0);

			final Table table;
			if (user.equals(this.user)) {
				table = new Table(UnitValue.createPercentArray(new float[] { 15f, 85f }));
				cellMessage.setBackgroundColor(this.colorChatUser, 0.3f);
				table.addCell(empty);
				table.addCell(cellTime);
				table.addCell(empty);
				table.addCell(cellMessage);
			} else {
				table = new Table(UnitValue.createPercentArray(new float[] { 85f, 15f }));
				cellMessage.setBackgroundColor(this.colorChatOther, 0.3f);
				table.addCell(cellTime);
				table.addCell(empty);
				table.addCell(cellMessage);
				table.addCell(empty);
			}
			table.setWidth(UnitValue.createPercentValue(100f));
			table.setKeepTogether(true);
			this.content.add(table);
			this.addEmptyLine();
			if (media == null || media.length == 0 || !media[0]) {
				Statistics wordCloud = this.wordClouds.stream().filter(e -> user.equals(e.getUser())).findFirst()
						.orElse(null);
				if (wordCloud == null) {
					wordCloud = new Statistics();
					wordCloud.user = user;
					wordCloud.text = new StringBuilder();
					this.wordClouds.add(wordCloud);
				}
				wordCloud.text.append(message).append(" ");
			}
		}

		private void addEmptyLine() {
			final Table empty = new Table(1);
			empty.setWidth(UnitValue.createPercentValue(100f));
			empty.setHeight(UnitValue.createPointValue(3));
			empty.addCell(this.createCell(""));
			this.content.add(empty);
		}

		private void addMetaData() throws IOException {
			final Table header = new Table(1);
			header.setWidth(UnitValue.createPercentValue(100f));
			header.setHeight(UnitValue.createPointValue(80));
			header.addCell(this.createCell("https://wa2pdf.com"));
			header.getCell(0, 0).setPadding(0);
			header.getCell(0, 0).setFontColor(this.colorDate);
			header.getCell(0, 0).setPaddingTop(36);
			((Paragraph) header.getCell(0, 0).getChildren().get(0)).setTextAlignment(TextAlignment.RIGHT);
			this.document.add(header);

			final PdfDocumentInfo catalog = this.document.getPdfDocument().getDocumentInfo();
			catalog.setTitle("PDF of exported WhatsApp Conversation");
			catalog.setSubject(PdfService.this.extractService.getFilename(this.id));
			catalog.setKeywords("WhatsApp PDF Converter");
			catalog.setAuthor(this.user);
			catalog.setCreator("https://wa2pdf.com");

			final Table table = new Table(4);
			table.setWidth(UnitValue.createPercentValue(80f));
			table.setKeepTogether(true);
			table.addCell(this.createCell(""));
			table.addCell(this.createCell("Chats", TextAlignment.RIGHT, 0, 0, 0, 0));
			table.addCell(this.createCell("Words", TextAlignment.RIGHT, 0, 0, 0, 0));
			table.addCell(this.createCell("Letters", TextAlignment.RIGHT, 0, 0, 0, 0));
			final List<Statistics> totalSumUp = new ArrayList<>();
			for (int i = 0; i < this.total.size() && (!this.preview || i < 8); i++) {
				final Statistics statistics = this.total.get(i);
				Statistics statisticsTotal = totalSumUp.stream().filter(e2 -> e2.user.equals(statistics.user))
						.findFirst().orElse(null);
				if (statisticsTotal == null) {
					statisticsTotal = new Statistics();
					statisticsTotal.user = statistics.user;
					totalSumUp.add(statisticsTotal);
				}
				statisticsTotal.chats += statistics.chats;
				statisticsTotal.words += statistics.words;
				statisticsTotal.letters += statistics.letters;
			}
			final Statistics userStatistics = totalSumUp.stream().filter(e -> e.user.equals(this.user)).findFirst()
					.orElse(null);
			if (userStatistics != null) {
				totalSumUp.remove(userStatistics);
				totalSumUp.add(userStatistics);
			}
			for (int i = 0; i < totalSumUp.size(); i++) {
				final Statistics statistics = totalSumUp.get(i);
				final Cell cell = this.createCell(statistics.user, TextAlignment.RIGHT, 0, 0, 0, 0);
				final java.awt.Color color = PdfService.this.chartService.nextColor(i);
				cell.setFontColor(
						PatternColor.createColorWithColorSpace(new float[] { ((float) color.getRed()) / 255,
								((float) color.getGreen()) / 255, ((float) color.getBlue()) / 255 }));
				table.addCell(cell);
				table.addCell(this.createCell(String.format("%,d", statistics.chats), TextAlignment.RIGHT, 0, 0, 0, 0));
				table.addCell(this.createCell(String.format("%,d", statistics.words), TextAlignment.RIGHT, 0, 0, 0, 0));
				table.addCell(
						this.createCell(String.format("%,d", statistics.letters), TextAlignment.RIGHT, 0, 0, 0, 0));
			}
			table.setMarginBottom(20f);
			this.document.add(table);
		}

		private void addChart() throws IOException, ParseException {
			final Table table = new Table(1);
			table.setWidth(UnitValue.createPercentValue(100f));
			table.setKeepTogether(true);
			final String idChart = filename + UUID.randomUUID().toString() + ".png";
			PdfService.this.chartService.createImage(this.total, this.dir.resolve(idChart), this.preview, this.user);
			final Cell cellChart = this.createCell(idChart, true);
			cellChart.setPadding(0);
			cellChart.setWidth(UnitValue.createPercentValue(100f));
			table.addCell(cellChart);
			this.document.add(table);
		}

		private void addWordCloud() throws IOException {
			final Statistics wordCloudStatistics = this.wordClouds.stream().filter(e -> e.user.equals(this.user))
					.findFirst().orElse(null);
			if (wordCloudStatistics != null) {
				this.wordClouds.remove(wordCloudStatistics);
				this.wordClouds.add(wordCloudStatistics);
			}
			table.setMarginTop(20f);
			final int maxColumns = 3;
			final Table table = new Table(Math.min(this.wordClouds.size(), maxColumns));
			final List<List<Token>> tokens = new ArrayList<>();
			final List<String> names = new ArrayList<>();
			int max = 0, min = Integer.MAX_VALUE;
			for (final Statistics wordCloud : this.wordClouds) {
				final List<Token> token = PdfService.this.wordCloudService.extract(wordCloud.text.substring(0,
						this.preview && wordCloud.text.indexOf(" ") > 0 && wordCloud.text.length() > 700
								? wordCloud.text.lastIndexOf(" ", (int) (0.1 * wordCloud.text.length()))
								: wordCloud.text.length()));
				while (token.size() > 50)
					token.remove(50);
				if (max < token.get(0).getCount())
					max = token.get(0).getCount();
				if (min > token.get(token.size() - 1).getCount())
					min = token.get(token.size() - 1).getCount();
				tokens.add(token);
				names.add(wordCloud.getUser());
			}
			for (int i = 0; i < tokens.size(); i++) {
				if (i > 0 && i % maxColumns == 0)
					this.addWordCloudNames(names, maxColumns, table);
				final List<Token> token = tokens.get(i);
				final String idWordCloud = filename + UUID.randomUUID().toString() + ".png";
				PdfService.this.wordCloudService.createImage(token, max, min, this.dir.resolve(idWordCloud));
				final Cell cell = this.createCell(idWordCloud, true);
				cell.setPadding(0);
				cell.setWidth(UnitValue.createPercentValue(100f / tokens.size()));
				table.addCell(cell);
			}
			if (tokens.size() > maxColumns) {
				for (int i = 0; i < maxColumns - tokens.size() % maxColumns; i++)
					table.addCell(this.createCell(""));
				table.setMarginBottom(40);
			}
			this.addWordCloudNames(names, maxColumns, table);
			this.document.add(table);
			if (PDF.this.document.getPdfDocument().getNumberOfPages() == 1)
				this.document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
		}

		private void addWordCloudNames(final List<String> names, final int maxColumns, final Table table) {
			for (int i = 0; i < maxColumns && names.size() > 0; i++) {
				final Cell cell = this.createCell(names.remove(0), TextAlignment.CENTER);
				cell.setPaddingTop(0);
				table.addCell(cell);
			}
		}

		private Cell createCell(final String text, final boolean... media) {
			return this.createCell(text, TextAlignment.LEFT, media == null || media.length == 0 ? false : media[0]);
		}

		private Cell createCell(final String text, final TextAlignment alignment, final float... padding) {
			return this.createCell(text, alignment, false, padding);
		}

		private Cell createCell(final String text, final TextAlignment alignment, final boolean media,
				final float... padding) {
			final Cell cell = new Cell();
			final int defaultPadding = 10;
			cell.setBorder(Border.NO_BORDER);
			cell.setFontSize(11f);
			cell.setMargin(0);
			cell.setPaddingTop(padding != null && padding.length > 0 ? padding[0] : defaultPadding / (media ? 1 : 2));
			cell.setPaddingLeft(padding != null && padding.length > 1 ? padding[1] : defaultPadding);
			cell.setPaddingBottom(
					padding != null && padding.length > 2 ? padding[2] : defaultPadding / (media ? 1 : 2));
			cell.setPaddingRight(padding != null && padding.length > 3 ? padding[3] : defaultPadding);
			if (media)
				this.fillMedia(cell, text);
			else
				this.fillText(cell, text, alignment);
			return cell;
		}

		private void fillMedia(final Cell cell, String mediaId) {
			try {
				if (mediaId.endsWith(".mp4")) {
					System.out.println(ExtractService.getTempDir(this.id).resolve(mediaId).toUri().toURL());
					final PdfFileSpec pdfFileSpec = PdfFileSpec.createEmbeddedFileSpec(this.document.getPdfDocument(),
							IOUtils.toByteArray(ExtractService.getTempDir(this.id).resolve(mediaId)
									.toUri().toURL()),
							"", null);
					this.document.getPdfDocument().addFileAttachment(mediaId, pdfFileSpec);
					final Paragraph paragraph = new Paragraph("");
					paragraph.setDestination(mediaId);
					paragraph.setAction(PdfAction.createRendition(mediaId, pdfFileSpec, "video/mp4",
							new PdfLinkAnnotation(new Rectangle(200, 200))));
					cell.setMinHeight(200f);
					cell.add(paragraph);
				} else {
					final BufferedImage originalImage = ImageIO
							.read(ExtractService.getTempDir(this.id).resolve(mediaId).toUri().toURL());
					final double max = 800;
					final int w = originalImage.getWidth(), h = originalImage.getHeight();
					if (!mediaId.endsWith(".png") && (mediaId.toLowerCase().endsWith(".webp") || w > max || h > max)) {
						final double factor = w > h ? (w > max ? max / w : 1) : (h > max ? max / h : 1);
						final BufferedImage image = new BufferedImage((int) (factor * w), (int) (factor * h),
								BufferedImage.TYPE_4BYTE_ABGR);
						final Graphics2D g = image.createGraphics();
						g.drawImage(originalImage, 0, 0, image.getWidth(), image.getHeight(), 0, 0, w, h, null);
						image.flush();
						g.dispose();
						mediaId = mediaId.substring(0, mediaId.lastIndexOf('.')) + "_scaled.png";
						ImageIO.write(image, "png", this.dir.resolve(mediaId).toAbsolutePath().toFile());
					}
					final Image image = new Image(ImageDataFactory
							.create(this.dir.resolve(mediaId).toAbsolutePath().toFile().getAbsolutePath()));
					if (w > h)
						image.setAutoScaleWidth(true);
					else
						image.setAutoScaleHeight(true);
					cell.add(image);
				}
			} catch (final IOException ex) {
				throw new RuntimeException(ex);
			}
		}

		private void fillText(final Cell cell, String text, final TextAlignment alignment) {
			final Paragraph paragraph = new Paragraph();
			final List<String> emojis = EmojiParser.extractEmojis(text);
			for (final String emoji : emojis) {
				if (text.substring(0, text.indexOf(emoji)).trim().length() > 0)
					paragraph.add(this.createText(text.substring(0, text.indexOf(emoji)), fontMessage));
				String id = "";
				for (int i = 0; i < emoji.length(); i++) {
					if (emoji.codePointAt(i) != 56614)
						id += "_" + Integer.toHexString(emoji.codePointAt(i));
				}
				if (text.indexOf("\ufe0f") < 9) {
					for (int i = emoji.length(); i <= text.indexOf("\ufe0f"); i++)
						id += "_" + Integer.toHexString(text.codePointAt(i));
				}
				id = id.substring(1);
				InputStream s = PdfService.class.getResourceAsStream("/emoji/" + id + ".png");
				if (s == null && id.contains("_"))
					s = PdfService.class.getResourceAsStream("/emoji/" + id.split("_")[0] + ".png");
				if (s == null && !id.contains("_"))
					s = PdfService.class.getResourceAsStream("/emoji/" + id + "_fe0f.png");
				if (s == null) {
					final Ticket ticket = new Ticket();
					ticket.setNote("Emoji " + emoji + " (" + id + ") not found!");
					PdfService.this.adminService.createTicket(ticket);
				} else {
					try {
						final Image image = new Image(ImageDataFactory.create(IOUtils.toByteArray(s)));
						image.setHeight(15f);
						image.setMarginBottom(-2f);
						paragraph.add(image);
					} catch (final IOException e) {
						throw new RuntimeException(e);
					}
				}
				text = text.substring(text.indexOf(emoji) + emoji.length());
			}
			if (text.length() > 4 || text.length() > 0 && text.codePointAt(text.length() - 1) != 65039)
				paragraph.add(this.createText(text, fontMessage));
			else if (paragraph.getChildren().size() == 1 && paragraph.getChildren().get(0) instanceof Image) {
				final Image image = (Image) paragraph.getChildren().get(0);
				image.setHeight(36);
				image.setWidth(36f * image.getImageWidth() / image.getImageHeight());
				image.setMarginBottom(0);
			}
			paragraph.setTextAlignment(alignment);
			cell.add(paragraph);
		}

		private Text createText(final String text, final PdfFont font) {
			final Text t = new Text(text);
			t.setFont(font);
			return t;
		}
	}
}
