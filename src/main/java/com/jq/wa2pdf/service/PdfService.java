package com.jq.wa2pdf.service;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfDestination;
import com.itextpdf.text.pdf.PdfOutline;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;

@Component
public class PdfService {
	public static final String filename = "wa";

	@Async
	public void create(final String id, final String month, final String user) throws IOException, DocumentException {
		new PDF(id, month, user).create();
	}

	public String getFilename(final String id) throws IOException {
		return IOUtils.toString(ExtractService.getTempDir(id).resolve(filename + "Filename").toUri().toURL(),
				StandardCharsets.UTF_8);
	}

	public Path get(final String id) throws IOException {
		final Path pdfPath = ExtractService.getTempDir(id).resolve(filename + ".pdf");
		if (Files.exists(pdfPath))
			return pdfPath;
		return null;
	}

	public static class Statistics {
		String user;
		String month;
		int chats = 0;
		int words = 0;
		int letters = 0;

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

		public String getMonth() {
			return month;
		}
	}

	class PDF {
		private static Font fontMessage = new Font(Font.FontFamily.HELVETICA, 11f, Font.NORMAL);
		private static Font fontTime = new Font(Font.FontFamily.HELVETICA, 8.5f, Font.NORMAL);
		private final Path dir;
		private final PdfWriter writer;
		private final Document document;
		private final List<String> outline = new ArrayList<>();
		private final TableOfContent tableOfContent = new TableOfContent();
		private final List<Statistics> total = new ArrayList<>();
		private final List<PdfPTable> content = new ArrayList<>();
		private final String month;
		private final String user;
		private final String id;

		private PDF(final String id, final String month, final String user) throws IOException, DocumentException {
			this.dir = ExtractService.getTempDir(id).toAbsolutePath();
			this.month = month;
			this.user = user;
			this.id = id;
			this.document = new Document();

			Files.deleteIfExists(dir.resolve(filename + ".tmp"));
			Files.deleteIfExists(dir.resolve(filename + ".pdf"));
			writer = PdfWriter.getInstance(document,
					new FileOutputStream(
							dir.resolve(filename + ".tmp").toAbsolutePath().toFile().getAbsoluteFile()));
			writer.setPageEvent(new PdfPageEventHelper() {
				@Override
				public void onEndPage(PdfWriter writer, Document document) {
					try {
						writer.getDirectContentUnder()
								.addImage(
										Image.getInstance(
												getClass().getResource("/background/000001.png")
														.toExternalForm()),
										document.getPageSize().getWidth(), 0, 0,
										document.getPageSize().getHeight(), 0, 0);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});
		}

		private class TableOfContent {
			private final List<Statistics> users = new ArrayList<>();
			private String date = null;
		}

		private void create() throws IOException, DocumentException {
			total.clear();
			try (final BufferedReader chat = new BufferedReader(new FileReader(dir.resolve("_chat.txt").toFile()))) {
				document.open();
				boolean foundMonth = false;
				final Pattern patternStart = Pattern
						.compile("^.?\\[" + month.replaceAll("[0-9]", "\\\\d") + ", \\d\\d:\\d\\d:\\d\\d\\] ([^:].*?)");
				final Pattern patternMonth = Pattern
						.compile("^.?\\[" + month + ", \\d\\d:\\d\\d:\\d\\d\\] ([^:].*?)");
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
								Statistics u = tableOfContent.users.stream().filter(e -> e.user.equals(s)).findFirst()
										.orElse(null);
								if (u == null) {
									u = new Statistics();
									u.user = user;
									tableOfContent.users.add(u);
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
							else
								addMessage(user, time,
										line.substring(line.indexOf("<Anhang: ") + 9, line.length() - 1).trim(), true);
						}
					} else if (foundMonth)
						lastChat += "\n" + line;
				}
				addMessage(user, time, lastChat);
				addDate(null);
				addMetaData();
				int i = 0;
				for (PdfPTable e : content) {
					document.add(e);
					if (e.getNumberOfColumns() == 1 && !e.getRow(0).getCells()[0].hasFixedHeight())
						writer.getRootOutline().addKid(new PdfOutline(writer.getRootOutline(),
								new PdfDestination(PdfDestination.XYZ, 0,
										writer.getVerticalPosition(false) + e.getTotalHeight(), 0),
								new Paragraph(outline.get(i++))));
				}
				document.close();
			}
			Files.move(dir.resolve(filename + ".tmp"), dir.resolve(filename + ".pdf"));
		}

		private void addDate(final String date) throws DocumentException {
			if (tableOfContent.date != null && !tableOfContent.date.equals(date)) {
				String s = tableOfContent.date;
				for (final Statistics statistics : tableOfContent.users) {
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
			if (date != null && !date.equals(tableOfContent.date)) {
				tableOfContent.date = date;
				tableOfContent.users.clear();

				final PdfPCell cell = createCell(date, Element.ALIGN_CENTER, 1.5f, 0, 10, 0);
				cell.setBackgroundColor(new BaseColor(200, 200, 200, 200));

				final PdfPTable table = new PdfPTable(1);
				table.setWidthPercentage(100.0f);
				table.addCell(cell);
				table.setComplete(true);
				content.add(table);
				addEmptyLine();
			}
		}

		private void addMessage(final String user, final String time, final String message) throws DocumentException {
			addMessage(user, time, message, false);
		}

		private void addMessage(final String user, final String time, final String message, boolean media)
				throws DocumentException {
			final PdfPCell cellMessage = createCell(message, media);

			final PdfPCell cellTime = createCell(time);
			cellTime.setPaddingBottom(2);
			cellTime.getCompositeElements().get(0).getChunks().get(0).setFont(fontTime);

			final PdfPCell empty = createCell("");
			empty.setPadding(0);

			final PdfPTable table = new PdfPTable(2);
			table.setWidthPercentage(100f);
			if (user.equals(this.user)) {
				table.setTotalWidth(new float[] { 20f, 80f });
				cellMessage.setBackgroundColor(new BaseColor(0, 200, 255, 20));
				table.addCell(empty);
				table.addCell(cellTime);
				table.addCell(empty);
				table.addCell(cellMessage);
			} else {
				table.setTotalWidth(new float[] { 80f, 20f });
				cellMessage.setBackgroundColor(new BaseColor(255, 200, 0, 20));
				table.addCell(cellTime);
				table.addCell(empty);
				table.addCell(cellMessage);
				table.addCell(empty);
			}
			table.setKeepTogether(true);
			table.setComplete(true);
			content.add(table);
			addEmptyLine();
		}

		private void addEmptyLine() {
			final PdfPTable empty = new PdfPTable(1);
			empty.setWidthPercentage(100f);
			final PdfPCell cell = createCell("");
			cell.setFixedHeight(5f);
			empty.addCell(cell);
			content.add(empty);
		}

		private void addMetaData() throws IOException, DocumentException {
			document.addTitle("PDF of exported WhatsApp Conversation");
			document.addSubject(getFilename(id));
			document.addKeywords("WhatsApp PDF Converter");
			document.addAuthor(user);
			document.addCreator("https://wa2pdf.com");
			document.addCreationDate();

			final PdfPTable table = new PdfPTable(4);
			table.setKeepTogether(true);
			table.addCell(createCell(""));
			table.addCell(createCell("Chats", Element.ALIGN_RIGHT, 0, 0, 0, 0));
			table.addCell(createCell("Words", Element.ALIGN_RIGHT, 0, 0, 0, 0));
			table.addCell(createCell("Letters", Element.ALIGN_RIGHT, 0, 0, 0, 0));
			total.stream().forEach(e -> {
				table.addCell(createCell(e.user, Element.ALIGN_RIGHT, 0, 0, 0, 0));
				table.addCell(createCell("" + e.chats, Element.ALIGN_RIGHT, 0, 0, 0, 0));
				table.addCell(createCell("" + e.words, Element.ALIGN_RIGHT, 0, 0, 0, 0));
				table.addCell(createCell("" + e.letters, Element.ALIGN_RIGHT, 0, 0, 0, 0));
			});
			table.setSpacingAfter(20f);
			table.setComplete(true);
			document.add(table);
		}

		private PdfPCell createCell(final String text, final boolean... media) {
			return createCell(text, Element.ALIGN_LEFT, media == null || media.length == 0 ? false : media[0]);
		}

		private PdfPCell createCell(String text, final int alignment, final float... padding) {
			return createCell(text, alignment, false, padding);
		}

		private PdfPCell createCell(String text, final int alignment, final boolean media, final float... padding) {
			final PdfPCell cell = new PdfPCell();
			final int defaultPadding = 10;
			cell.setBorder(0);
			cell.setPaddingTop(padding != null && padding.length > 0 ? padding[0] : 0);
			cell.setPaddingLeft(padding != null && padding.length > 1 ? padding[1] : defaultPadding);
			cell.setPaddingBottom(padding != null && padding.length > 2 ? padding[2] : defaultPadding);
			cell.setPaddingRight(padding != null && padding.length > 3 ? padding[3] : defaultPadding);
			if (media) {
				try {
					System.out.println(ExtractService.getTempDir(id).resolve(text).toUri().toURL());
					if (padding == null || padding.length == 0)
						cell.setPaddingTop(defaultPadding);
					if (text.endsWith(".mp4")) {

					} else {
						if (text.endsWith(".webp")) {
							final BufferedImage originalImage = ImageIO
									.read(ExtractService.getTempDir(id).resolve(text).toUri().toURL());
							final BufferedImage image = new BufferedImage(originalImage.getWidth(),
									originalImage.getHeight(), BufferedImage.TYPE_INT_RGB);
							final Graphics2D g = image.createGraphics();
							g.drawImage(originalImage, 0, 0, originalImage.getWidth(), originalImage.getHeight(), 0, 0,
									originalImage.getWidth(), originalImage.getHeight(), null);
							image.flush();
							g.dispose();
							text = text.substring(0, text.length() - 4) + "jpg";
							ImageIO.write(image, "jpg",
									ExtractService.getTempDir(id).resolve(text).toAbsolutePath().toFile());
						}
						final Image image = Image
								.getInstance(ExtractService.getTempDir(id).resolve(text).toUri().toURL());
						cell.addElement(image);
					}
				} catch (BadElementException | IOException ex) {
					throw new RuntimeException(ex);
				}
			} else {
				final Chunk chunkMessage = new Chunk(text);
				chunkMessage.setFont(fontMessage);
				final Paragraph paragraph = new Paragraph();
				paragraph.add(chunkMessage);
				paragraph.setAlignment(alignment);
				cell.addElement(paragraph);
			}
			return cell;
		}
	}
}