package com.jq.wa2pdf.service;

import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.PatternColor;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.kernel.pdf.PdfCatalog;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfOutline;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfString;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEvent;
import com.itextpdf.kernel.pdf.event.AbstractPdfDocumentEventHandler;
import com.itextpdf.kernel.pdf.event.PdfDocumentEvent;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.jq.wa2pdf.service.WordCloudService.Token;
import com.vdurmont.emoji.EmojiParser;

@Component
public class PdfService {
	public static final String filename = "wa";

	@Autowired
	private ExtractService extractService;

	@Autowired
	private WordCloudService wordCloudService;

	@Async
	public void create(final String id, final String period, final String user, boolean preview) throws Exception {
		final Path error = ExtractService.getTempDir(id).resolve(PdfService.filename + "Error");
		try {
			Files.deleteIfExists(error);
			new PDF(id, period, user, preview).create();
		} catch (Exception ex) {
			ex.printStackTrace();
			try (final FileOutputStream filename = new FileOutputStream(error.toFile())) {
				filename.write(ex.getMessage().getBytes(StandardCharsets.UTF_8));
			}
			throw ex;
		}
	}

	public Path get(final String id, String period) throws IOException {
		final Path pdfPath = ExtractService.getTempDir(id)
				.resolve(getFilename(period) + ".pdf");
		if (Files.exists(pdfPath))
			return pdfPath;
		return null;
	}

	private String getFilename(String period) {
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
			return chats;
		}

		public int getWords() {
			return words;
		}

		public int getLetters() {
			return letters;
		}

		public String getUser() {
			return user;
		}

		public String getPeriod() {
			return period;
		}
	}

	private class PDF {
		private static PdfFont fontEmoji;
		private static PdfFont fontMessage;
		private final Path dir;
		private PdfWriter writer;
		private Document document;
		private final UsersPerDay usersPerDay = new UsersPerDay();
		private final List<String> outline = new ArrayList<>();
		private final List<Statistics> total = new ArrayList<>();
		private final List<Statistics> wordClouds = new ArrayList<>();
		private final List<Table> content = new ArrayList<>();
		private final Color colorDate = PatternColor.createColorWithColorSpace(new float[] { 0.78f, 0.78f, 0.78f });
		private final Color colorChatUser = PatternColor.createColorWithColorSpace(new float[] { 0.7f, 0.9f, 1f });
		private final Color colorChatOther = PatternColor.createColorWithColorSpace(new float[] { 1f, 0.9f, 0.7f });
		private final String period;
		private final String user;
		private final String id;
		private final boolean preview;

		private PDF(final String id, final String period, final String user, boolean preview)
				throws IOException {
			this.dir = ExtractService.getTempDir(id).toAbsolutePath();
			this.period = period;
			this.user = user;
			this.id = id;
			this.preview = preview;
			fontMessage = PdfFontFactory.createFont(StandardFonts.HELVETICA);
			fontEmoji = PdfFontFactory.createFont(
					IOUtils.toByteArray(getClass().getResourceAsStream("/font/NotoEmoji.ttf")),
					EmbeddingStrategy.FORCE_EMBEDDED);
		}

		private class UsersPerDay {
			private final List<Statistics> users = new ArrayList<>();
			private String date = null;
		}

		private void create() throws IOException, FontFormatException {
			final String filename = getFilename(preview ? null : period);
			Files.deleteIfExists(dir.resolve(filename + ".tmp"));
			Files.deleteIfExists(dir.resolve(filename + ".pdf"));
			writer = new PdfWriter(dir.resolve(filename + ".tmp").toAbsolutePath().toFile().getAbsoluteFile());
			document = new Document(new PdfDocument(writer));
			document.getPdfDocument().addEventHandler(PdfDocumentEvent.START_PAGE,
					new AbstractPdfDocumentEventHandler() {
						@Override
						protected void onAcceptedEvent(AbstractPdfDocumentEvent event) {
							try {
								final PdfPage page = document.getPdfDocument()
										.getPage(document.getPdfDocument().getNumberOfPages());
								final PdfCanvas canvas = new PdfCanvas(page);
								canvas.addImageFittedIntoRectangle(ImageDataFactory.create(getClass()
										.getResource("/background/000001.png").toExternalForm()),
										page.getPageSize(), false);
							} catch (Exception e) {
								throw new RuntimeException(e);
							}
						}
					});
			parseChats();
			addMetaData();
			writeContent();
			document.flush();
			document.close();
			Files.move(dir.resolve(filename + ".tmp"), dir.resolve(filename + ".pdf"));
		}

		private void parseChats() throws IOException {
			try (final BufferedReader chat = new BufferedReader(new FileReader(dir.resolve("_chat.txt").toFile()))) {
				boolean foundMonth = false;
				final Pattern patternStart = Pattern
						.compile(
								"^.?\\[" + period.replaceAll("[0-9]", "\\\\d") + ", \\d\\d:\\d\\d:\\d\\d\\] ([^:].*?)");
				final Pattern patternMonth = Pattern
						.compile("^.?\\[" + period + ", \\d\\d:\\d\\d:\\d\\d\\] ([^:].*?)");
				String lastChat = null, line, user = null, time = null;
				while ((line = chat.readLine()) != null) {
					line = line.replaceAll("\u200E", "");
					if (line.trim().length() > 0 && patternStart.matcher(line).matches()) {
						boolean inMonth = patternMonth.matcher(line).matches();
						if (foundMonth && !inMonth)
							break;
						foundMonth = inMonth;
						if (foundMonth) {
							if (lastChat != null) {
								final String s = user;
								Statistics u = usersPerDay.users.stream().filter(e -> e.user.equals(s)).findFirst()
										.orElse(null);
								if (u == null) {
									u = new Statistics();
									u.user = user;
									usersPerDay.users.add(u);
								}
								addMessage(user, time, lastChat);
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
							addDate(line.split(" ")[0].replace("[", "").replace(",", "").trim());
							user = line.substring(line.indexOf("]") + 1, line.indexOf(":", line.indexOf("]"))).trim();
							time = line.substring(line.indexOf(' '), line.indexOf(']')).trim();
							if (line.indexOf("<Anhang: ") < 0 || !line.endsWith(">"))
								lastChat = line.substring(line.indexOf(": ") + 2);
							else {
								lastChat = null;
								addMessage(user, time,
										line.substring(line.indexOf("<Anhang: ") + 9, line.length() - 1).trim(), true);
							}
						}
					} else if (foundMonth)
						lastChat += "\n" + line;
				}
				addMessage(user, time, lastChat);
				addDate(null);
			}
		}

		private void writeContent() throws IOException {
			int i = 0;
			final PdfOutline root = document.getPdfDocument().getOutlines(true);
			for (Table e : content) {
				document.add(e);
				if (e.getNumberOfColumns() == 1
						&& ((UnitValue) e.getCell(0, 0)
								.getProperty(com.itextpdf.layout.properties.Property.FONT_SIZE)).getValue() < 9) {
					final int h = (int) document.getPdfDocument().getLastPage().getPageSize().getHeight();
					root.addOutline(outline.get(i++)).addDestination(PdfExplicitDestination
							.createXYZ(document.getPdfDocument().getFirstPage(), 0f,
									document.getPdfDocument().getWriter().getCurrentPos() % h,
									1f));
				}
				if (preview && document.getPdfDocument().getNumberOfPages() > 4)
					break;
			}
			if (preview)
				addPreviewInfo();
		}

		private void addPreviewInfo() {
			final Paragraph paragraph = new Paragraph();
			paragraph.setMarginTop(20);
			paragraph.setFont(fontMessage);
			paragraph.add(
					"\n\n\n\n\nThis is a preview of your chat.\nYou may download the whole chat on:\nhttps://wa2pdf.com");
			paragraph.setTextAlignment(TextAlignment.CENTER);
			document.add(paragraph);
		}

		private void addDate(final String date) {
			if (usersPerDay.date != null && !usersPerDay.date.equals(date)) {
				String s = usersPerDay.date;
				for (final Statistics statistics : usersPerDay.users) {
					s += " · " + statistics.user + " " + statistics.chats;
					Statistics statisticsTotal = total.stream().filter(e -> e.user.equals(statistics.user)).findFirst()
							.orElse(null);
					if (statisticsTotal == null) {
						statisticsTotal = new Statistics();
						statisticsTotal.user = statistics.user;
						total.add(statisticsTotal);
					}
					statisticsTotal.chats += statistics.chats;
					statisticsTotal.words += statistics.words;
					statisticsTotal.letters += statistics.letters;
				}
				outline.add(s);
			}
			if (date != null && !date.equals(usersPerDay.date)) {
				usersPerDay.date = date;
				usersPerDay.users.clear();

				final Cell cell = createCell(date, TextAlignment.CENTER, 1.5f, 0, 10, 0);
				cell.setFontSize(8.5f);
				cell.setBackgroundColor(colorDate, 0.3f);

				final Table table = new Table(1);
				table.setWidth(UnitValue.createPercentValue(100.0f));
				table.addCell(cell);
				content.add(table);
				addEmptyLine();
			}
		}

		private void addMessage(final String user, final String time, final String message, boolean... media) {
			final Cell cellMessage = createCell(message, media);

			final Cell cellTime = createCell(time);
			cellTime.setPaddingBottom(2);

			final Cell empty = createCell("");
			empty.setPadding(0);

			final Table table;
			if (user.equals(this.user)) {
				table = new Table(UnitValue.createPercentArray(new float[] { 20f, 80f }));
				cellMessage.setBackgroundColor(colorChatUser, 0.3f);
				table.addCell(empty);
				table.addCell(cellTime);
				table.addCell(empty);
				table.addCell(cellMessage);
			} else {
				table = new Table(UnitValue.createPercentArray(new float[] { 80f, 20f }));
				cellMessage.setBackgroundColor(colorChatOther, 0.3f);
				table.addCell(cellTime);
				table.addCell(empty);
				table.addCell(cellMessage);
				table.addCell(empty);
			}
			table.setWidth(UnitValue.createPercentValue(100f));
			table.setKeepTogether(true);
			content.add(table);
			addEmptyLine();
			if (media == null || media.length == 0 || !media[0]) {
				Statistics wordCloud = wordClouds.stream().filter(e -> user.equals(e.getUser())).findFirst()
						.orElse(null);
				if (wordCloud == null) {
					wordCloud = new Statistics();
					wordCloud.user = user;
					wordCloud.text = new StringBuilder();
					wordClouds.add(wordCloud);
				}
				wordCloud.text.append(message).append(" ");
			}
		}

		private void addEmptyLine() {
			final Table empty = new Table(1);
			empty.setWidth(UnitValue.createPercentValue(100f));
			final Cell cell = createCell("");
			cell.setHeight(5f);
			empty.addCell(cell);
			content.add(empty);
		}

		private void addMetaData() throws IOException, FontFormatException {
			final PdfCatalog catalog = document.getPdfDocument().getCatalog();
			catalog.put(PdfName.Title, new PdfString("PDF of exported WhatsApp Conversation"));
			catalog.put(PdfName.Subject, new PdfString(extractService.getFilename(id)));
			catalog.put(PdfName.Keywords, new PdfString("WhatsApp PDF Converter"));
			catalog.put(PdfName.Author, new PdfString(user));
			catalog.put(PdfName.Creator, new PdfString("https://wa2pdf.com"));
			catalog.put(PdfName.CreationDate,
					new PdfString(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));

			final Table table = new Table(4);
			table.setWidth(UnitValue.createPercentValue(100f));
			table.setKeepTogether(true);
			table.addCell(createCell(""));
			table.addCell(createCell("Chats", TextAlignment.RIGHT, 0, 0, 0, 0));
			table.addCell(createCell("Words", TextAlignment.RIGHT, 0, 0, 0, 0));
			table.addCell(createCell("Letters", TextAlignment.RIGHT, 0, 0, 0, 0));
			total.stream().forEach(e -> {
				table.addCell(createCell(e.user, TextAlignment.RIGHT, 0, 0, 0, 0));
				table.addCell(createCell(String.format("%,d", e.chats), TextAlignment.RIGHT, 0, 0, 0, 0));
				table.addCell(createCell(String.format("%,d", e.words), TextAlignment.RIGHT, 0, 0, 0, 0));
				table.addCell(createCell(String.format("%,d", e.letters), TextAlignment.RIGHT, 0, 0, 0, 0));
			});
			table.setMarginBottom(20f);
			document.add(table);

			final Table tableWordCloud = new Table(wordClouds.size());
			tableWordCloud.setWidth(UnitValue.createPercentValue(100f));
			tableWordCloud.setKeepTogether(true);
			final List<List<Token>> tokens = new ArrayList<>();
			int max = 0, min = Integer.MAX_VALUE;
			for (Statistics wordCloud : wordClouds) {
				final List<Token> token = wordCloudService.extract(wordCloud.text.substring(0, wordCloud.text.length() *
						(preview && wordCloud.text.indexOf(" ") > 0 && wordCloud.text.length() > 700
								? wordCloud.text.lastIndexOf(" ", (int) (0.1 * wordCloud.text.length()))
								: 1)));
				while (token.size() > 40)
					token.remove(40);
				if (max < token.get(0).getCount())
					max = token.get(0).getCount();
				if (min > token.get(token.size() - 1).getCount())
					min = token.get(token.size() - 1).getCount();
				tokens.add(token);
			}
			for (List<Token> token : tokens) {
				final String id = filename + UUID.randomUUID().toString() + ".png";
				wordCloudService.createImage(token, max, min, dir.resolve(id));
				tableWordCloud.addCell(createCell(id, true));
			}
			for (Statistics wordCloud : wordClouds)
				tableWordCloud.addCell(createCell(wordCloud.getUser(), TextAlignment.CENTER));
			document.add(tableWordCloud);
		}

		private Cell createCell(final String text, final boolean... media) {
			return createCell(text, TextAlignment.LEFT, media == null || media.length == 0 ? false : media[0]);
		}

		private Cell createCell(final String text, final TextAlignment alignment, final float... padding) {
			return createCell(text, alignment, false, padding);
		}

		private Cell createCell(final String text, final TextAlignment alignment, final boolean media,
				final float... padding) {
			final Cell cell = new Cell();
			final int defaultPadding = 10;
			cell.setBorder(Border.NO_BORDER);
			cell.setFontSize(11f);
			cell.setPaddingTop(media ? defaultPadding : padding != null && padding.length > 0 ? padding[0] : 0);
			cell.setPaddingLeft(padding != null && padding.length > 1 ? padding[1] : defaultPadding);
			cell.setPaddingBottom(padding != null && padding.length > 2 ? padding[2] : defaultPadding);
			cell.setPaddingRight(padding != null && padding.length > 3 ? padding[3] : defaultPadding);
			if (media)
				fillMedia(cell, text);
			else
				fillText(cell, text, alignment);
			return cell;
		}

		private void fillMedia(final Cell cell, String mediaId) {
			try {
				System.out.println(ExtractService.getTempDir(id).resolve(mediaId).toUri().toURL());
				if (mediaId.endsWith(".mp4")) {
					final Text chunk = new Text("");
					// chunk.setAnnotation(PdfAnnotation
					// .makeAnnotation(
					// writer, cell, "",
					// PdfFileSpec.createEmbeddedFileSpec(document.getPdfDocument(),
					// IOUtils.toByteArray(ExtractService.getTempDir(id).resolve(mediaId)
					// .toUri().toURL()),
					// "", null),
					// "video/mp4", true));
					cell.setMinHeight(200f);
					cell.add(new Paragraph(chunk));
				} else {
					final BufferedImage originalImage = ImageIO
							.read(ExtractService.getTempDir(id).resolve(mediaId).toUri().toURL());
					final double max = 800;
					final int w = originalImage.getWidth(), h = originalImage.getHeight();
					if (mediaId.endsWith(".webp") || w > max || h > max) {
						final double factor = w > h ? (w > max ? max / w : 1) : (h > max ? max / h : 1);
						final BufferedImage image = new BufferedImage((int) (factor * w), (int) (factor * h),
								BufferedImage.TYPE_INT_RGB);
						final Graphics2D g = image.createGraphics();
						g.drawImage(originalImage, 0, 0, image.getWidth(), image.getHeight(), 0, 0, w, h, null);
						image.flush();
						g.dispose();
						mediaId = mediaId.substring(0, mediaId.lastIndexOf('.')) + "_scaled.jpg";
						ImageIO.write(image, "jpg", dir.resolve(mediaId).toAbsolutePath().toFile());
					}
					final Image image = new Image(
							ImageDataFactory.create(dir.resolve(mediaId).toAbsolutePath().toFile().getAbsolutePath()));
					image.setAutoScaleWidth(true);
					cell.add(image);
				}
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}

		private void fillText(final Cell cell, String text, final TextAlignment alignment) {
			final Paragraph paragraph = new Paragraph();
			final List<String> emojis = EmojiParser.extractEmojis(text);
			for (String emoji : emojis) {
				if (text.indexOf(emoji) > 0)
					paragraph.add(createText(text.substring(0, text.indexOf(emoji)), fontMessage));
				paragraph.add(createText(emoji, fontEmoji));
				text = text.substring(text.indexOf(emoji) + emoji.length());
			}
			if (text.length() > 0)
				paragraph.add(createText(text, fontMessage));
			paragraph.setTextAlignment(alignment);
			cell.setFontSize(11f);
			cell.add(paragraph);
		}

		private Text createText(final String text, final PdfFont font) {
			final Text t = new Text(text);
			t.setFont(font);
			return t;
		}
	}
}
