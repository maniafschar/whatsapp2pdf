package com.jq.wa2pdf.service;

import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLConnection;
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
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.PatternColor;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
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
import com.itextpdf.layout.element.IElement;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.jq.wa2pdf.entity.Ticket;
import com.jq.wa2pdf.service.AiService.AiSummary;
import com.jq.wa2pdf.service.ExtractService.Attributes;
import com.jq.wa2pdf.service.ExtractService.Statistics;
import com.jq.wa2pdf.service.WordCloudService.Token;
import com.jq.wa2pdf.util.DateHandler;
import com.jq.wa2pdf.util.Utilities;
import com.vdurmont.emoji.EmojiManager;

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

	public enum Type {
		Preview, Summary
	}

	@Async
	public void create(final String id, final String period, final String user, final Type type)
			throws IOException, FontFormatException, ParseException {
		final Path error = ExtractService.getTempDir(id)
				.resolve(ExtractService.filename + "Error"
						+ (type == Type.Preview ? "" : DateHandler.periodSuffix(period)));
		try {
			Files.deleteIfExists(error);
			new PDF(id, period, user, type).create();
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
		private static final int defaultPadding = 10;
		private final Path dir;
		private PdfWriter writer;
		private Document document;
		private String dateFormat;
		private final List<String> outline = new ArrayList<>();
		private final List<Statistics> total = new ArrayList<>();
		private final List<Statistics> wordClouds = new ArrayList<>();
		private final List<Table> content = new ArrayList<>();
		private AiSummary aiSummary = null;
		private final Map<String, java.awt.Color> colors = new HashMap<>();
		private final Color colorDate = PatternColor.createColorWithColorSpace(new float[] { 0.53f, 0.53f, 0.53f });
		private final Color colorChatUser = PatternColor.createColorWithColorSpace(new float[] { 0.7f, 0.9f, 1f });
		private final Color colorChatOther = PatternColor.createColorWithColorSpace(new float[] { 1f, 0.9f, 0.7f });
		private final String period;
		private final String user;
		private final String id;
		private final Type type;
		private final StringBuilder text = new StringBuilder();
		private final Pattern patternEmail = Pattern.compile("((\\.+)(?=.*@)|(\\+.*(?=@)))", Pattern.CASE_INSENSITIVE);
		private final Pattern patternUrl = Pattern.compile(
				"((https?|ftp|gopher|telnet|file|Unsure|http):((//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)",
				Pattern.CASE_INSENSITIVE);

		private PDF(final String id, final String period, final String user, final Type type)
				throws IOException {
			this.dir = ExtractService.getTempDir(id).toAbsolutePath();
			this.period = period;
			this.user = user;
			this.id = id;
			this.type = type;
			fontMessage = PdfFontFactory.createFont(
					IOUtils.toByteArray(
							this.getClass().getResourceAsStream("/font/NotoSans-VariableFont_wdth,wght.ttf")),
					EmbeddingStrategy.FORCE_EMBEDDED);
		}

		private void create() throws IOException, FontFormatException, ParseException {
			this.dateFormat = new ObjectMapper().readValue(
					ExtractService.getTempDir(this.id).resolve(ExtractService.filename + "Attributes").toFile(),
					Attributes.class).getDateFormat();
			final String filename = ExtractService.filename
					+ (this.type == Type.Preview ? "" : DateHandler.periodSuffix(this.period));
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
			this.addHeader();
			this.addAISummary();
			this.addWordCloud();
			this.addMetaData();
			this.addChart();
			this.writeContent();
			this.document.flush();
			this.document.close();
			Files.move(this.dir.resolve(filename + ".tmp"), this.dir.resolve(filename + ".pdf"));
		}

		private void addHeader() {
			final Table header = new Table(1);
			header.setWidth(UnitValue.createPercentValue(100f));
			header.setHeight(UnitValue.createPointValue(80));
			header.addCell(new Paragraph(this.createText("https://wa2pdf.com", fontMessage)));
			header.getCell(0, 0).setPadding(0);
			header.getCell(0, 0).setFontColor(this.colorDate);
			header.getCell(0, 0).setPaddingTop(33);
			header.getCell(0, 0).setBorder(Border.NO_BORDER);
			((Paragraph) header.getCell(0, 0).getChildren().get(0)).setTextAlignment(TextAlignment.RIGHT);
			this.document.add(header);
		}

		private void parseChats() throws IOException {
			try (final BufferedReader input = new BufferedReader(
					new FileReader(PdfService.this.extractService.getFilenameChat(this.id).toFile()))) {
				final Pattern patternMedia = Pattern.compile(PdfService.this.extractService.getPatternMadia(this.id));
				final Pattern patternStart = Pattern.compile(PdfService.this.extractService.getPatternStart(null));
				final Pattern patternMonth = Pattern.compile(
						PdfService.this.extractService.getPatternStart(this.period.replace("\\d", "\\d{1,2}")));
				boolean foundMonth = false;
				String line, lastChat = null, user = null, date = null, separator = null;
				int i = 0;
				while ((line = input.readLine()) != null) {
					line = line.replaceAll("\u200E", "");
					if (line.trim().length() > 0 && patternStart.matcher(line).matches()) {
						final boolean inMonth = patternMonth.matcher(DateHandler.replaceStrangeWhitespace(line))
								.matches();
						if (foundMonth && !inMonth)
							break;
						foundMonth = inMonth;
						if (foundMonth) {
							i++;
							this.text.append(line).append('\n');
							if (lastChat != null)
								this.addMessage(user, date, lastChat);
							if (separator == null)
								separator = line.startsWith("[") || line.substring(1).startsWith("[") ? "]" : "-";
							user = Utilities.extractUser(line, separator);
							date = DateHandler.replaceStrangeWhitespace(line);
							date = date.substring(0, date.indexOf(separator)).trim();
							line = line.substring(line.indexOf(":", line.indexOf(separator) + separator.length()) + 1)
									.trim();
							final Matcher m = patternMedia.matcher(line);
							if (m.matches()) {
								lastChat = null;
								this.addMessage(user, date, m.group(1), true);
								i += 14;
							} else
								lastChat = line;
						}
					} else if (foundMonth && lastChat != null)
						lastChat += "\n" + line;
					if (this.type == Type.Preview && i > 40)
						break;
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
			this.document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
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
							final String path = ExtractService.getTempDir(this.id).resolve(mediaId).toFile()
									.getAbsolutePath();
							final PdfFileSpec fileSpec = PdfFileSpec.createEmbeddedFileSpec(
									this.document.getPdfDocument(), path, null, "My Video", null);
							final Rectangle annotationRect = new Rectangle(100, 400, 200, 200);
							final PdfLinkAnnotation linkAnnotation = new PdfLinkAnnotation(annotationRect);
							final PdfAction renditionAction = PdfAction.createRendition(path, fileSpec,
									"video/mp4", linkAnnotation);
							linkAnnotation.setAction(renditionAction);
							table.setAction(renditionAction);
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
				if (this.type == Type.Preview && this.document.getPdfDocument().getNumberOfPages() > 4)
					break;
			}
			if (this.type == Type.Preview)
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

		private void addMessage(final String user, String date, final String message, final boolean... media) {
			if (message == null || message.isBlank())
				return;
			date = date.replace("[", "").replace(",", "").trim();
			this.addDate(date.split(" ")[0]);
			final Cell cellMessage = this.createCell(message, media);

			final Cell cellTime = this.createCell(date == null ? "" : date + " · " + user);
			cellTime.setFontSize(8.5f);
			cellTime.setFontColor(this.colorDate);
			cellTime.setPaddingBottom(0);

			final Cell empty = this.createCell("");
			empty.setPadding(0);

			final boolean hasText = cellMessage.getChildren().get(0) instanceof Paragraph
					&& ((Paragraph) cellMessage.getChildren().get(0)).getChildren().stream()
							.anyMatch(e -> e instanceof Text);
			final Table table;
			if (user.equals(this.user)) {
				table = new Table(UnitValue.createPercentArray(new float[] { 15f, 85f }));
				if (hasText)
					cellMessage.setBackgroundColor(this.colorChatUser, 0.3f);
				table.addCell(empty);
				table.addCell(cellTime);
				table.addCell(empty);
				table.addCell(cellMessage);
			} else {
				table = new Table(UnitValue.createPercentArray(new float[] { 85f, 15f }));
				if (hasText)
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
				Matcher m = this.patternUrl.matcher(s);
				int i = 0;
				while (m.find()) {
					s = s.replaceAll(m.group(i), "").trim();
					i++;
				}
				m = this.patternEmail.matcher(s);
				i = 0;
				while (m.find()) {
					s = s.replaceAll(m.group(i), "").trim();
					i++;
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
			table.setMarginTop(15);
			final List<Statistics> totalSumUp = new ArrayList<>();
			for (int i = 0; i < this.total.size(); i++) {
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
			if (this.total.size() == 0)
				return;
			final Table table = new Table(1);
			table.setWidth(UnitValue.createPercentValue(100f));
			table.setKeepTogether(true);
			final String idChart = ExtractService.filename + UUID.randomUUID().toString() + ".png";
			final String error = PdfService.this.chartService.createImage(
					this.total, this.dir.resolve(idChart), this.colors, this.dateFormat);
			if (error != null)
				PdfService.this.adminService.createTicket(new Ticket(Ticket.ERROR + "ChartService\n" + error));
			final Cell cellChart = this.createCell(idChart, true);
			((Image) cellChart.getChildren().get(0)).setAutoScaleWidth(true);
			cellChart.setPadding(0);
			cellChart.setWidth(UnitValue.createPercentValue(100f));
			table.addCell(cellChart);
			this.document.add(table);
		}

		private void addWordCloud() throws IOException {
			if (this.wordClouds.size() == 0)
				return;
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
				final List<Token> token = PdfService.this.wordCloudService.extract(wordCloud.text.toString());
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
				final String name = names.remove(0);
				cell = this.createCell(name, TextAlignment.CENTER);
				cell.setPaddingTop(0);
				cell.setStrokeWidth(2);
				cellTable.addCell(cell);
				if (this.aiSummary != null) {
					final String n = name;
					if (this.aiSummary.adjectives.containsKey(n)) {
						cell = this.createCell(this.aiSummary.adjectives.get(n).stream()
								.collect(Collectors.joining("\u00a0 \u00a0")), TextAlignment.CENTER);
						cell.setPaddingTop(0);
						cellTable.addCell(cell);
					}
					if (this.aiSummary.emojis.containsKey(n)) {
						cell = this.createCell(this.aiSummary.emojis.get(n).stream()
								.collect(Collectors.joining("\u00a0 \u00a0")), TextAlignment.CENTER);
						this.resizeEmojis((Paragraph) cell.getChildren().get(0));
						cellTable.addCell(cell);
					}
				}
				cell = new Cell();
				cell.setBorder(Border.NO_BORDER);
				cell.setPadding(0);
				cell.setMargin(0);
				cell.add(cellTable);
				table.addCell(cell);
			}
			this.document.add(table);
			final Table empty = new Table(1);
			empty.setWidth(UnitValue.createPercentValue(100f));
			empty.setHeight(UnitValue.createPointValue(20));
			empty.addCell(this.createCell(""));
			this.document.add(empty);
		}

		private void addAISummary() throws IOException {
			if (this.type == Type.Preview) {
				this.aiSummary = new AiSummary();
				this.aiSummary.text = ("This is an example of a AI summary, including the generaated image:\n\n"
						+ "The WhatsApp chat between {user1} and {user2} spans several days and is filled with a complex mix of flirtatious banter, discussions about work, personal struggles, and the lingering feelings of a past or complicated relationship.\n\n"
						+ "{user1} initiates the conversation with cheerful greetings, but quickly expresses feeling unwell and uncertain about his situation at work and his impending departure. {user2} responds with concern and playful teasing, sometimes direct and suggestive, sometimes offering support. {user1} shares his anxieties about his general state of mind. {user2}, while sometimes teasing {user1} about his dramatic expressions, also offers reassurance and support, though she's overwhelmed by \"this whole nonsense\".")
						.replace("{user1}", this.wordClouds.size() > 0 ? this.wordClouds.get(0).user : "Romeo")
						.replace("{user2}", this.wordClouds.size() > 1 ? this.wordClouds.get(1).user : "Julia");
				this.aiSummary.image = IOUtils.toByteArray(this.getClass().getResourceAsStream("/image/aiExample.png"));
				this.wordClouds.stream().forEach(e -> {
					this.aiSummary.adjectives.put(e.user, Utilities.createAdjectives(false));
					this.aiSummary.emojis.put(e.user, Utilities.createAdjectives(true));
				});
			} else if (this.type == Type.Summary) {
				final Set<String> users = new HashSet<>();
				this.total.stream().forEach(e -> users.add(e.user));
				this.aiSummary = PdfService.this.aiService.summerize(this.text.toString(), users);
			}
			if (this.aiSummary != null) {
				if (this.aiSummary.text != null && !this.aiSummary.text.isBlank()) {
					final Table summary = new Table(1);
					final Cell cell = this.createCell("Summary", TextAlignment.CENTER);
					cell.setBackgroundColor(this.colorDate, 0.2f);
					summary.addCell(cell);
					summary.setWidth(UnitValue.createPercentValue(100f));
					summary.addCell(this.createCell(this.aiSummary.text));
					this.document.add(summary);
				}
				if (this.aiSummary.image != null) {
					String mediaId = ExtractService.filename + UUID.randomUUID().toString() + ".png";
					IOUtils.write(this.aiSummary.image,
							new FileOutputStream(this.dir.resolve(mediaId).toAbsolutePath().toFile()));
					mediaId = this.scaleImage(mediaId);
					final Table image = new Table(1);
					image.setWidth(UnitValue.createPercentValue(100f));
					image.addCell(this.createCell(mediaId, true));
					image.setMarginTop(15);
					image.setKeepTogether(true);
					image.getCell(0, 0).setPaddingLeft(80);
					image.getCell(0, 0).setPaddingRight(80);
					this.document.add(image);
				}
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
			cell.setBorder(Border.NO_BORDER);
			cell.setFontSize(11f);
			cell.setMargin(0);
			if (!media) {
				cell.setPaddingTop(
						padding != null && padding.length > 0 ? padding[0] : defaultPadding / 2);
				cell.setPaddingLeft(padding != null && padding.length > 1 ? padding[1] : defaultPadding);
				cell.setPaddingBottom(
						padding != null && padding.length > 2 ? padding[2] : defaultPadding / 2);
				cell.setPaddingRight(padding != null && padding.length > 3 ? padding[3] : defaultPadding);
			}
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
					cell.add(paragraph);
				} else {
					final boolean internalMedia = mediaId.matches(ExtractService.filename
							+ "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.png");
					if (!internalMedia)
						mediaId = this.scaleImage(mediaId);
					final Image image = new Image(ImageDataFactory
							.create(this.dir.resolve(mediaId).toAbsolutePath().toFile().getAbsolutePath()));
					if (!internalMedia) {
						image.setAutoScale(true);
						if (originalImage.getHeight() > 320)
							cell.setMinHeight(320);
						cell.setMaxHeight(550);
						cell.setMaxWidth(550);
					}
					cell.add(image);
				}
			} catch (final IOException ex) {
				// throw new RuntimeException(ex);
				cell.add(new Paragraph(mediaId + ":\n" + ex.getMessage()));
			}
		}

		private String scaleImage(String mediaId) throws IOException {
			final BufferedImage originalImage = ImageIO
					.read(ExtractService.getTempDir(this.id).resolve(mediaId).toUri().toURL());
			final double max = 800;
			final int w = originalImage.getWidth(), h = originalImage.getHeight();
			if (mediaId.toLowerCase().endsWith(".webp") || w > max || h > max) {
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
			return mediaId;
		}

		private void fillText(final Cell cell, String text, final TextAlignment alignment) {
			final Paragraph paragraph = new Paragraph();
			boolean hasEmoji = false;
			boolean hasText = false;
			String s = "";
			while (text.length() > 0) {
				final String id = Utilities.getEmojiId(text);
				if (id == null) {
					s += text.substring(0, 1);
					if (EmojiManager.isEmoji(s)) {
						String code = "";
						for (int i = 0; i < s.length(); i++) {
							code += '_' + s.codePointAt(i);
							if (s.codePointAt(i) > 65536)
								i++;
						}
						PdfService.this.adminService.createTicket(
								new Ticket(Ticket.ERROR + "emoji not found: " + code.substring(1)));
					}
					if (text.length() < 2) {
						if (s.length() > 0)
							paragraph.add(this.createText(s, fontMessage));
						text = "";
					} else
						text = text.substring(1);
				} else {
					if (s.length() > 0) {
						paragraph.add(this.createText(s, fontMessage));
						hasText = true;
						s = "";
					}
					try {
						final Image image = new Image(ImageDataFactory
								.create(IOUtils.toByteArray(PdfService.class.getResource("/emoji/" + id + ".png"))));
						image.setHeight(15f);
						image.setMarginBottom(-2f);
						paragraph.add(image);
						hasEmoji = true;
					} catch (final IOException e) {
						throw new RuntimeException(e);
					}
					final String[] ids = id.split("_");
					final int lastCodePoint = Integer.parseInt(ids[ids.length - 1], 16);
					for (int i = 0; i < text.length(); i++) {
						if (text.codePointAt(0) == lastCodePoint)
							break;
						text = text.substring(1);
					}
					text = text.substring(lastCodePoint > 65536 ? 2 : 1);
				}
			}
			if (!hasEmoji && this.fillLinkPreview(cell, text))
				cell.setPadding(0);
			if (!hasText && !paragraph.getChildren().isEmpty() && paragraph.getChildren().get(0) instanceof Image)
				this.resizeEmojis(paragraph);
			paragraph.setTextAlignment(alignment);
			cell.add(paragraph);
		}

		private void resizeEmojis(final Paragraph paragraph) {
			for (final IElement e : paragraph.getChildren()) {
				if (e instanceof Image) {
					final Image image = (Image) e;
					image.setHeight(32);
					image.setWidth(32f * image.getImageWidth() / image.getImageHeight());
					image.setMarginBottom(0);
				}
			}
		}

		private boolean fillLinkPreview(final Cell cell, final String text) {
			if (text.startsWith("https://") && !text.contains(" ") && !text.contains("\n")) {
				String uri = null;
				try {
					final URLConnection urlConnection = new URI(text).toURL().openConnection();
					urlConnection.setReadTimeout(1000);
					urlConnection.setConnectTimeout(1000);
					try (final BufferedReader input = new BufferedReader(
							new InputStreamReader(urlConnection.getInputStream()))) {
						String line;
						final Pattern content = Pattern.compile("content=\"([^\"].*?)\"");
						while ((line = input.readLine()) != null) {
							final int i = line.indexOf("property=\"og:image\"");
							if (i > -1) {
								line = line.substring(line.lastIndexOf('<', i));
								String s;
								while (!line.contains(">") && (s = input.readLine()) != null)
									line += " " + s;
								if (!line.contains(">"))
									break;
								line = line.substring(0, line.indexOf('>'));
								final Matcher matcher = content.matcher(line);
								if (matcher.find()) {
									uri = matcher.group(1);
									final File f = this.dir
											.resolve(ExtractService.filename + UUID.randomUUID().toString() + ".jpg")
											.toAbsolutePath().toFile();
									IOUtils.write(IOUtils.toByteArray(new URI(uri).toURL().openStream()),
											new FileOutputStream(f));
									this.fillMedia(cell, f.getName());
									return true;
								}
							}
						}
					}
				} catch (final IOException ex) {
					// no preview available, continue
				} catch (final Exception ex) {
					PdfService.this.adminService.createTicket(new Ticket(Ticket.ERROR + "fillLinkPreview\n" +
							text + "\n" + (uri == null ? "" : uri + "\n") + Utilities.stackTraceToString(ex)));
				}
			}
			return false;
		}

		private Text createText(final String text, final PdfFont font) {
			final Text t = new Text(text);
			t.setFont(font);
			return t;
		}
	}
}
