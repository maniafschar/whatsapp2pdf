package com.jq.wa2pdf.service;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.itextpdf.text.Anchor;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.ListItem;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Section;
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
		private static Font catFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
		private static Font redFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL, BaseColor.RED);
		private static Font subFont = new Font(Font.FontFamily.TIMES_ROMAN, 16, Font.BOLD);
		private static Font smallBold = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
		private final Path dir;
		private final PdfWriter writer;
		private final Document document;
		private final List<String> outline = new ArrayList<>();
		private final TableOfContent tableOfContent = new TableOfContent();
		private final Map<String, Statistics> total = new HashMap<>();
		private final List<PdfPTable> content = new ArrayList<>();
		private final String month;
		private final String user;
		private final String id;
		private float position;

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
			private final Map<String, Statistics> users = new HashMap<>();
			private String date = null;
		}

		private void create() throws IOException, DocumentException {
			total.clear();
			try (final BufferedReader chat = new BufferedReader(new FileReader(dir.resolve("_chat.txt").toFile()))) {
				document.open();
				addMetaData();
				final Pattern start = Pattern.compile("^.?\\[\\d\\d.\\d\\d.\\d\\d, \\d\\d:\\d\\d:\\d\\d\\] ([^:].*?)");
				String lastChat = null, line, user = null, time = null;
				while ((line = chat.readLine()) != null) {
					line = line.replaceAll("\u200E", "");
					if (line.trim().length() > 0 && start.matcher(line).matches()) {
						if (lastChat != null) {
							if (!tableOfContent.users.containsKey(user))
								tableOfContent.users.put(user, new Statistics());
							tableOfContent.users.get(user).chats++;
							if (lastChat != null) {
								lastChat = lastChat.replaceAll("\t", " ");
								lastChat = lastChat.replaceAll("\r", " ");
								lastChat = lastChat.replaceAll("\n", " ");
								while (lastChat.indexOf("  ") > -1)
									lastChat = lastChat.replaceAll("  ", " ");
								tableOfContent.users.get(user).words += lastChat.split(" ").length;
								tableOfContent.users.get(user).letters += lastChat.replaceAll(" ", "").length();
							}
							addMessage(user, time, lastChat);
						}
						addDate(line.split(" ")[0].replace("[", "").replace(",", "").trim());
						user = line.substring(line.indexOf("]") + 1, line.indexOf(":", line.indexOf("]"))).trim();
						time = line.substring(line.indexOf(' '), line.indexOf(']')).trim();
						if (line.indexOf("<Anhang: ") < 0)
							lastChat = line.substring(line.indexOf(": ") + 2);
						else if (line.indexOf(".mp4") > 0)
							lastChat = "<video controls><source src=\"wa/"
									+ line.substring(line.indexOf("<Anhang: ") + 9, line.length() - 1).trim()
									+ "\" /></video>";
						else
							lastChat = "<img src=\"wa/"
									+ line.substring(line.indexOf("<Anhang: ") + 9, line.length() - 1).trim() + "\" />";
					} else
						lastChat += "\n" + line;
				}
				addMessage(user, time, lastChat);
				addDate(null);
				document.newPage();
				int i = 0;
				for (PdfPTable e : content) {
					try {
						document.add(e);
						if (e.getNumberOfColumns() == 1 && !e.getRow(0).getCells()[0].hasFixedHeight())
							writer.getRootOutline().addKid(new PdfOutline(writer.getRootOutline(),
									new PdfDestination(PdfDestination.XYZ, 0,
											writer.getVerticalPosition(false) + e.getTotalHeight(), 0),
									new Paragraph(outline.get(i++))));
					} catch (DocumentException ex) {
						throw new RuntimeException(ex);
					}
				}
				document.close();
			}
			Files.move(dir.resolve(filename + ".tmp"), dir.resolve(filename + ".pdf"));
		}

		private void addDate(final String date) throws DocumentException {
			if (tableOfContent.date != null && !tableOfContent.date.equals(date)) {
				String s = tableOfContent.date;
				for (final Map.Entry<String, Statistics> entry : tableOfContent.users.entrySet()) {
					final Statistics user = entry.getValue();
					s += " · " + entry.getKey() + " " + user.chats;
					if (!total.containsKey(entry.getKey()))
						total.put(entry.getKey(), new Statistics());
					total.get(entry.getKey()).chats += user.chats;
					total.get(entry.getKey()).words += user.words;
					total.get(entry.getKey()).letters += user.letters;
				}
				outline.add(s);
			}
			if (date != null && !date.equals(tableOfContent.date)) {
				tableOfContent.date = date;
				tableOfContent.users.clear();

				final PdfPCell cell = new PdfPCell();
				cell.setBorder(0);
				cell.setPaddingTop(1.5f);
				cell.setPaddingLeft(0);
				cell.setPaddingRight(0);
				cell.setPaddingBottom(10);
				cell.setBackgroundColor(new BaseColor(200, 200, 200, 200));

				final Paragraph paragraph = new Paragraph(new Chunk(date));
				paragraph.setAlignment(Element.ALIGN_CENTER);
				cell.addElement(paragraph);

				final PdfPTable table = new PdfPTable(1);
				table.setWidthPercentage(100.0f);
				table.addCell(cell);
				table.setComplete(true);
				content.add(table);
				addEmptyLine();
			}
		}

		private void addMessage(final String user, final String time, final String message) throws DocumentException {
			final PdfPCell cellMessage = new PdfPCell();
			cellMessage.setBorder(0);
			cellMessage.setPaddingTop(0);
			cellMessage.setPaddingLeft(10);
			cellMessage.setPaddingRight(10);
			cellMessage.setPaddingBottom(10);
			final Chunk chunkMessage = new Chunk(message);
			chunkMessage.setFont(fontMessage);
			cellMessage.addElement(chunkMessage);

			final PdfPCell cellTime = new PdfPCell();
			cellTime.setBorder(0);
			cellTime.setPaddingTop(0);
			cellTime.setPaddingLeft(10);
			cellTime.setPaddingRight(0);
			cellTime.setPaddingBottom(2);
			final Chunk chunkTime = new Chunk(time);
			chunkTime.setFont(fontTime);
			cellTime.addElement(chunkTime);

			final PdfPCell empty = new PdfPCell();
			empty.setBorder(0);
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
			final PdfPCell cell = new PdfPCell();
			cell.setFixedHeight(5f);
			cell.setBorder(0);
			cell.addElement(new Paragraph(" "));
			empty.addCell(cell);
			content.add(empty);
		}

		private void addMetaData() throws IOException {
			document.addTitle("PDF of exported WhatsApp Conversation");
			document.addSubject(getFilename(id));
			document.addKeywords("WhatsApp PDF Converter");
			document.addAuthor(user);
			document.addCreator("https://wa2pdf.com");
			document.addCreationDate();
		}

		private void addTitlePage() throws DocumentException {
			Paragraph preface = new Paragraph();
			// We add one empty line
			addEmptyLine(preface, 1);
			// Lets write a big header
			preface.add(new Paragraph("Title of the document", catFont));

			addEmptyLine(preface, 1);
			// Will create: Report generated by: _name, _date
			preface.add(new Paragraph("Report generated by: " + System.getProperty("user.name") + ", " + new Date(), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					smallBold));
			addEmptyLine(preface, 3);
			preface.add(new Paragraph("This document describes something which is very important ", smallBold));

			addEmptyLine(preface, 8);

			preface.add(new Paragraph(
					"This document is a preliminary version and not subject to your license agreement or any other agreement with vogella.com ;-).",
					redFont));

			document.add(preface);
			// Start a new page
			document.newPage();
		}

		private void addContent() throws DocumentException {
			Anchor anchor = new Anchor("First Chapter", catFont);
			anchor.setName("First Chapter");

			// Second parameter is the number of the chapter
			Chapter catPart = new Chapter(new Paragraph(anchor), 1);

			Paragraph subPara = new Paragraph("Subcategory 1", subFont);
			Section subCatPart = catPart.addSection(subPara);
			subCatPart.add(new Paragraph("Hello"));

			subPara = new Paragraph("Subcategory 2", subFont);
			subCatPart = catPart.addSection(subPara);
			subCatPart.add(new Paragraph("Paragraph 1"));
			subCatPart.add(new Paragraph("Paragraph 2"));
			subCatPart.add(new Paragraph("Paragraph 3"));

			// add a list
			createList(subCatPart);
			Paragraph paragraph = new Paragraph();
			addEmptyLine(paragraph, 5);
			subCatPart.add(paragraph);

			// add a table
			createTable(subCatPart);

			// now add all this to the document
			document.add(catPart);

			// Next section
			anchor = new Anchor("Second Chapter", catFont);
			anchor.setName("Second Chapter");

			// Second parameter is the number of the chapter
			catPart = new Chapter(new Paragraph(anchor), 1);

			subPara = new Paragraph("Subcategory", subFont);
			subCatPart = catPart.addSection(subPara);
			subCatPart.add(new Paragraph("This is a very important message"));

			// now add all this to the document
			document.add(catPart);

		}

		private void createTable(Section subCatPart) throws BadElementException {
			PdfPTable table = new PdfPTable(3);

			// t.setBorderColor(BaseColor.GRAY);
			// t.setPadding(4);
			// t.setSpacing(4);
			// t.setBorderWidth(1);

			PdfPCell c1 = new PdfPCell(new Phrase("Table Header 1"));
			c1.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.addCell(c1);

			c1 = new PdfPCell(new Phrase("Table Header 2"));
			c1.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.addCell(c1);

			c1 = new PdfPCell(new Phrase("Table Header 3"));
			c1.setHorizontalAlignment(Element.ALIGN_CENTER);
			table.addCell(c1);
			table.setHeaderRows(1);

			table.addCell("1.0");
			table.addCell("1.1");
			table.addCell("1.2");
			table.addCell("2.1");
			table.addCell("2.2");
			table.addCell("2.3");

			subCatPart.add(table);

		}

		private void createList(Section subCatPart) {
			com.itextpdf.text.List list = new com.itextpdf.text.List(true, false, 10);
			list.add(new ListItem("First point"));
			list.add(new ListItem("Second point"));
			list.add(new ListItem("Third point"));
			subCatPart.add(list);
		}

		private void addEmptyLine(Paragraph paragraph, int number) {
			for (int i = 0; i < number; i++) {
				paragraph.add(new Paragraph(" "));
			}
		}
	}
}