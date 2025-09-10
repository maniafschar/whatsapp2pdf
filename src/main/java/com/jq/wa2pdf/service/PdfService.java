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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
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
import com.itextpdf.layout.element.IElement;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.jq.wa2pdf.entity.Ticket;
import com.jq.wa2pdf.service.ExtractService.Attributes;
import com.jq.wa2pdf.service.ExtractService.Statistics;
import com.jq.wa2pdf.service.WordCloudService.Token;
import com.jq.wa2pdf.util.DateHandler;
import com.vdurmont.emoji.EmojiParser;

@Component
public class PdfService {
	@Autowired
	private ExtractService extractService;

	@Autowired
	private ChartService chartService;

	@Autowired
	private WordCloudService wordCloudService;

	@Autowired
	private AiService aiService;

	@Autowired
	private AdminService adminService;

	@Async
	public void create(final String id, final String period, final String user, final boolean preview)
			throws IOException, FontFormatException, ParseException {
		final Path error = ExtractService.getTempDir(id)
				.resolve(ExtractService.filename + "Error" + (preview ? "" : DateHandler.periodSuffix(period)));
		try {
			Files.deleteIfExists(error);
			new PDF(id, period, user, preview).create();
		} catch (final Throwable ex) {
			try (final FileOutputStream filename = new FileOutputStream(error.toFile())) {
				filename.write(ex.getMessage().getBytes(StandardCharsets.UTF_8));
			}
			throw ex;
		}
	}

	public Path get(final String id, final String period) throws IOException {
		final Path pdfPath = ExtractService.getTempDir(id)
				.resolve(ExtractService.filename + DateHandler.periodSuffix(period) + ".pdf");
		if (Files.exists(pdfPath))
			return pdfPath;
		return null;
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
		private final Map<String, java.awt.Color> colors = new HashMap<>();
		private final Color colorDate = PatternColor.createColorWithColorSpace(new float[] { 0.53f, 0.53f, 0.53f });
		private final Color colorChatUser = PatternColor.createColorWithColorSpace(new float[] { 0.7f, 0.9f, 1f });
		private final Color colorChatOther = PatternColor.createColorWithColorSpace(new float[] { 1f, 0.9f, 0.7f });
		private final String period;
		private final String user;
		private final String id;
		private final boolean preview;
		private final boolean groupChat;
		private final StringBuilder chat = new StringBuilder();

		private PDF(final String id, final String period, final String user, final boolean preview)
				throws IOException {
			this.dir = ExtractService.getTempDir(id).toAbsolutePath();
			this.period = period;
			this.user = user;
			this.id = id;
			this.preview = preview;
			fontMessage = PdfFontFactory.createFont(StandardFonts.HELVETICA);
			this.groupChat = new ObjectMapper().readValue(
					ExtractService.getTempDir(id).resolve(ExtractService.filename + "Attributes").toFile(),
					Attributes.class).getUsers().size() > 2;
		}

		private void create() throws IOException, FontFormatException, ParseException {
			final String filename = ExtractService.filename
					+ (this.preview ? "" : DateHandler.periodSuffix(this.period));
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
			this.addAISummery();
			this.writeContent();
			this.document.flush();
			this.document.close();
			Files.move(this.dir.resolve(filename + ".tmp"), this.dir.resolve(filename + ".pdf"));
		}

		private void parseChats() throws IOException {
			try (final BufferedReader chat = new BufferedReader(
					new FileReader(PdfService.this.extractService.getFilenameChat(this.id).toFile()))) {
				final Pattern patternMedia = Pattern.compile(PdfService.this.extractService.getPatternMadia(this.id));
				final Pattern patternStart = Pattern
						.compile(PdfService.this.extractService.getPatternStart().replace("{date}",
								this.period.replaceAll("[0-9]", "\\\\d")));
				final Pattern patternMonth = Pattern
						.compile(PdfService.this.extractService.getPatternStart().replace("{date}", this.period));
				final Set<String> users = new HashSet<>();
				boolean foundMonth = false;
				String line, lastChat = null, user = null, date = null, separator = null;
				while ((line = chat.readLine()) != null) {
					line = line.replaceAll("\u200E", "");
					if (line.trim().length() > 0 && patternStart.matcher(line).matches()) {
						final boolean inMonth = patternMonth.matcher(line).matches();
						if (foundMonth && !inMonth)
							break;
						foundMonth = inMonth;
						if (foundMonth) {
							this.chat.append(line).append('\n');
							if (lastChat != null)
								this.addMessage(user, date, lastChat);
							this.addDate(line.split(" ")[0].replace("[", "").replace(",", "").trim());
							if (separator == null)
								separator = line.startsWith("[") || line.substring(1).startsWith("[") ? "]" : "-";
							user = line
									.substring(line.indexOf(separator) + 1, line.indexOf(":", line.indexOf(separator)))
									.trim();
							date = line.substring(0, line.indexOf(separator)).trim();
							users.add(user);
							line = line.substring(line.indexOf(": ") + 2);
							final Matcher m = patternMedia.matcher(line);
							if (m.matches()) {
								lastChat = null;
								this.addMessage(user, date, m.group(1), true);
							} else
								lastChat = line;
						}
					} else if (foundMonth && lastChat != null)
						lastChat += "\n" + line;
				}
				this.addMessage(user, date, lastChat);
				this.addDate(null);
			}
		}

		private Statistics getUserStatistics(final String user, final String date) {
			Statistics u = this.total.stream()
					.filter(e -> e.user.equals(user) && e.period.equals(date))
					.findFirst().orElse(null);
			if (u == null) {
				u = new Statistics();
				u.user = user;
				u.period = date;
				this.total.add(u);
			}
			if (!this.colors.containsKey(user)) {
				final java.awt.Color[] COLORS = { java.awt.Color.RED, java.awt.Color.BLUE, java.awt.Color.BLACK,
						java.awt.Color.MAGENTA, java.awt.Color.DARK_GRAY };
				this.colors.put(user, COLORS[this.colors.size() % COLORS.length]);
			}
			return u;
		}

		private void writeContent() throws IOException {
			int i = 0;
			final PdfOutline root = this.document.getPdfDocument().getOutlines(true);
			for (final Table table : this.content) {
				if (table.getNumberOfRows() == 2 && table.getNumberOfColumns() == 2) {
					final IElement element = table
							.getCell(1, table.getColumnWidth(0).getValue() < 50 ? 1 : 0).getChildren().get(0);
					if (element instanceof Paragraph && ((Paragraph) element).getChildren().size() > 0
							&& ((Paragraph) element).getChildren().get(0) instanceof Text) {
						final Text text = (Text) ((Paragraph) element).getChildren().get(0);
						if (text.getText().startsWith("media://")) {
							final String mediaId = text.getText().substring(8);
							final PdfFileSpec pdfFileSpec = PdfFileSpec.createEmbeddedFileSpec(
									this.document.getPdfDocument(),
									IOUtils.toByteArray(
											ExtractService.getTempDir(this.id).resolve(mediaId).toUri().toURL()),
									"", null);
							this.document.getPdfDocument().addFileAttachment(mediaId, pdfFileSpec);
							System.out.println(text.getText());
							// table.setAction(PdfAction.createRendition("abc.mp4", pdfFileSpec,
							// "video/mp4",
							// PdfAnnotation.makeAnnotation(null)));
							text.setText("click");
						}
					}
				}
				this.document.add(table);
				if (table.getNumberOfColumns() == 1 && table.getHeight() == null)
					root.addOutline(this.outline.get(i++))
							.addDestination(new PdfNamedDestination(
									this.sanitizeDestination(
											((Text) ((Paragraph) table.getCell(0, 0).getChildren().get(0))
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

		private void addMessage(final String user, final String date, final String message, final boolean... media) {
			final Cell cellMessage = this.createCell(message, media);

			final Cell cellTime = this.createCell(date.split(" ")[1] + (this.groupChat ? " · " + user : ""));
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
			if (media != null && media.length > 0 && media[0]) {
				cellMessage.setPadding(0);
				cellMessage.setBackgroundColor(null);
			}
			table.setWidth(UnitValue.createPercentValue(100f));
			table.setKeepTogether(true);
			this.content.add(table);
			this.addEmptyLine();
			final Statistics u = this.getUserStatistics(user,
					date.split(" ")[0].replace("[", "").replace(",", "").trim());
			u.chats++;
			if (media == null || media.length == 0 || !media[0]) {
				String s = message.replaceAll("\t", " ").replaceAll("\r", " ").replaceAll("\n", " ");
				while (s.indexOf("  ") > -1)
					s = s.replaceAll("  ", " ");
				s = s.trim();
				if (s.endsWith(">") && s.contains("<")
						&& !s.substring(s.lastIndexOf("<"))
								.matches(".*[0-9\\.\\\\/\\?\\!\"'+\\-_=&%#@\\,:;\\{}\\()\\[\\]].*"))
					s = s.substring(0, s.lastIndexOf("<")).trim();
				u.words += s.split(" ").length;
				u.letters += s.replaceAll(" ", "").length();
				Statistics wordCloud = this.wordClouds.stream().filter(e -> user.equals(e.getUser())).findFirst()
						.orElse(null);
				if (wordCloud == null) {
					wordCloud = new Statistics();
					wordCloud.user = user;
					wordCloud.text = new StringBuilder();
					this.wordClouds.add(wordCloud);
				}
				wordCloud.text.append(s).append(" ");
			} else
				u.media++;
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
				final java.awt.Color color = this.colors.get(statistics.user);
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
			final String idChart = ExtractService.filename + UUID.randomUUID().toString() + ".png";
			PdfService.this.chartService.createImage(this.total, this.dir.resolve(idChart), this.preview, this.colors);
			final Cell cellChart = this.createCell(idChart, true);
			((Image) cellChart.getChildren().get(0)).setAutoScaleWidth(true);
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
			final int maxColumns = 3;
			final int columns = Math.min(this.wordClouds.size(), maxColumns);
			final UnitValue[] columnWidths = new UnitValue[columns];
			for (int i = 0; i < columnWidths.length; i++)
				columnWidths[i] = UnitValue.createPercentValue(1.0f / columns);
			final Table table = new Table(columnWidths);
			final List<List<Token>> tokens = new ArrayList<>();
			final List<String> names = new ArrayList<>();
			int max = 0, min = Integer.MAX_VALUE;
			table.setMarginTop(20f);
			for (final Statistics wordCloud : this.wordClouds) {
				final List<Token> token = PdfService.this.wordCloudService.extract(wordCloud.text.substring(0,
						this.preview && wordCloud.text.indexOf(" ") > 0 && wordCloud.text.length() > 700
								? wordCloud.text.lastIndexOf(" ", (int) (0.1 * wordCloud.text.length()))
								: wordCloud.text.length()));
				while (token.size() > 50)
					token.remove(50);
				if (token.size() > 0) {
					if (max < token.get(0).getCount())
						max = token.get(0).getCount();
					if (min > token.get(token.size() - 1).getCount())
						min = token.get(token.size() - 1).getCount();
				}
				tokens.add(token);
				names.add(wordCloud.getUser());
			}
			for (int i = 0; i < tokens.size(); i++) {
				final List<Token> token = tokens.get(i);
				final String idWordCloud = ExtractService.filename + UUID.randomUUID().toString() + ".png";
				PdfService.this.wordCloudService.createImage(token, max, min, this.dir.resolve(idWordCloud));
				final Table cellTable = new Table(1);
				cellTable.setWidth(UnitValue.createPercentValue(100f));
				cellTable.setKeepTogether(true);
				cellTable.setBorder(Border.NO_BORDER);
				Cell cell = this.createCell(idWordCloud, true);
				((Image) cell.getChildren().get(0)).setAutoScaleWidth(true);
				cell.setPadding(0);
				cell.setWidth(UnitValue.createPercentValue(100f));
				cellTable.addCell(cell);
				cell = this.createCell(names.remove(0), TextAlignment.CENTER);
				cell.setPaddingTop(0);
				cellTable.addCell(cell);
				cell = new Cell();
				cell.setBorder(Border.NO_BORDER);
				cell.setPadding(0);
				cell.setMargin(0);
				cell.add(cellTable);
				table.addCell(cell);
			}
			this.document.add(table);
			if (PDF.this.document.getPdfDocument().getNumberOfPages() == 1)
				this.document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
			else {
				final Table empty = new Table(1);
				empty.setWidth(UnitValue.createPercentValue(100f));
				empty.setHeight(UnitValue.createPointValue(20));
				empty.addCell(this.createCell(""));
				this.document.add(empty);
			}
		}

		private void addAISummery() {
			final Table header = new Table(1);
			header.setWidth(UnitValue.createPercentValue(100f));
			header.addCell(this.createCell(PdfService.this.aiService.summerize(this.chat.toString())));
			header.getCell(0, 0).setPaddingTop(36);
			this.document.add(header);
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
			if (text != null) {
				if (media)
					this.fillMedia(cell, text);
				else
					this.fillText(cell, text, alignment);
			}
			return cell;
		}

		private void fillMedia(final Cell cell, String mediaId) {
			try {
				final BufferedImage originalImage = ImageIO
						.read(ExtractService.getTempDir(this.id).resolve(mediaId).toUri().toURL());
				if (originalImage == null) {
					final Paragraph paragraph = new Paragraph("media://" + mediaId);
					cell.setMinHeight(200f);
					cell.add(paragraph);
				} else {
					final double max = 550;
					final int w = originalImage.getWidth(), h = originalImage.getHeight();
					if (!mediaId.matches(ExtractService.filename
							+ "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png")
							&& (mediaId.toLowerCase().endsWith(".webp") || w > max || h > max)) {
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
					image.setAutoScale(false);
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
				final int position = text.indexOf("\ufe0f");
				if (position > emoji.length() && position < 9) {
					for (int i = emoji.length(); i <= position; i++)
						id += "_" + Integer.toHexString(text.codePointAt(i));
				}
				id = id.substring(1);
				InputStream s = PdfService.class.getResourceAsStream("/emoji/" + id + ".png");
				if (s == null && id.contains("_"))
					s = PdfService.class.getResourceAsStream("/emoji/" + id.split("_")[0] + ".png");
				if (s == null)
					s = PdfService.class
							.getResourceAsStream("/emoji/" + (id.contains("_") ? id.split("_")[0] : id) + "_fe0f.png");
				if (s == null)
					PdfService.this.adminService
							.createTicket(new Ticket("emoji not found: " + emoji + " " + id + "\n" + text));
				else {
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
