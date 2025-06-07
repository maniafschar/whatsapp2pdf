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
import com.itextpdf.text.ListItem;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Section;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Component
public class PdfService {
	public static final String filename = "wa";

	public Attributes analyse(final String id) throws IOException, DocumentException {
		return new PDF(id, null, null).analyse();
	}

	@Async
	public void create(final String id, final String month, final String user) throws IOException, DocumentException {
		new PDF(id, month, user).create();
	}

	public String getFilename(final String id) throws IOException, InterruptedException {
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
		private int chats = 0;
		private int words = 0;
		private int letters = 0;

		public int getChats() {
			return chats;
		}

		public int getWords() {
			return words;
		}

		public int getLetters() {
			return letters;
		}
	}

	public static class Attributes {
		private final Map<String, Statistics> users = new HashMap<>();
		private final Map<String, Statistics> months = new HashMap<>();
		private final String id;

		public Attributes(String id) {
			this.id = id;
		}

		public Map<String, Statistics> getUsers() {
			return users;
		}

		public Map<String, Statistics> getMonths() {
			return months;
		}

		public String getId() {
			return id;
		}
	}

	private class PDF {
		private static Font catFont = new Font(Font.FontFamily.TIMES_ROMAN, 18, Font.BOLD);
		private static Font redFont = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.NORMAL, BaseColor.RED);
		private static Font subFont = new Font(Font.FontFamily.TIMES_ROMAN, 16, Font.BOLD);
		private static Font smallBold = new Font(Font.FontFamily.TIMES_ROMAN, 12, Font.BOLD);
		private final Path dir;
		private final Document document;
		private final TableOfContent tableOfContent = new TableOfContent();
		private final Map<String, Statistics> total = new HashMap<>();
		private final List<Paragraph> content = new ArrayList<>();
		private final String month;
		private final String user;
		private final String id;

		private PDF(final String id, final String month, final String user) throws IOException, DocumentException {
			this.dir = ExtractService.getTempDir(id).toAbsolutePath();
			this.id = id;
			this.month = month;
			this.user = user;
			if (user == null)
				this.document = null;
			else {
				this.document = new Document();
				Files.deleteIfExists(dir.resolve(filename + ".tmp"));
				Files.deleteIfExists(dir.resolve(filename + ".pdf"));
				PdfWriter.getInstance(document,
						new FileOutputStream(
								dir.resolve(filename + ".tmp").toAbsolutePath().toFile().getAbsoluteFile()));
			}
		}

		private class TableOfContent {
			private final Map<String, Statistics> users = new HashMap<>();
			private String date = null;
		}

		private Attributes analyse() throws IOException, DocumentException {
			try (final BufferedReader chat = new BufferedReader(new FileReader(dir.resolve("_chat.txt").toFile()))) {
				final Attributes attributes = new Attributes(id);
				final Pattern start = Pattern.compile("^.?\\[\\d\\d.\\d\\d.\\d\\d, \\d\\d:\\d\\d:\\d\\d\\] ([^:].*?)");
				String s[], currentDate = null, lastChat = null, line, user = null;
				while ((line = chat.readLine()) != null) {
					if (line.trim().length() > 0 && start.matcher(line).matches()) {
						user = line.substring(line.indexOf("]") + 1, line.indexOf(":", line.indexOf("]"))).trim();
						if (lastChat != null) {
							if (!attributes.users.containsKey(user))
								attributes.users.put(user, new Statistics());
							attributes.users.get(user).chats++;
							if (lastChat != null) {
								lastChat = lastChat.replaceAll("\t", " ");
								lastChat = lastChat.replaceAll("\r", " ");
								lastChat = lastChat.replaceAll("\n", " ");
								while (lastChat.indexOf("  ") > -1)
									lastChat = lastChat.replaceAll("  ", "");
								attributes.users.get(user).words += lastChat.split(" ").length;
								attributes.users.get(user).letters += lastChat.replaceAll(" ", "").length();
							}
						}
						s = line.split(" ");
						s = s[0].replace("[", "").replace(",", "").trim().split("\\.");
						if (currentDate != s[2] + "-" + s[1]) {
							currentDate = s[2] + "-" + s[1];
							if (!attributes.months.containsKey(currentDate))
								attributes.months.put(currentDate, new Statistics());
							attributes.months.get(currentDate).chats++;
							if (lastChat != null) {
								lastChat = sanitize(lastChat);
								lastChat = lastChat.replaceAll("\t", " ");
								lastChat = lastChat.replaceAll("\r", " ");
								lastChat = lastChat.replaceAll("\n", " ");
								while (lastChat.indexOf("  ") > -1)
									lastChat = lastChat.replaceAll("  ", " ");
								attributes.months.get(currentDate).words += lastChat.split(" ").length;
								attributes.months.get(currentDate).letters += lastChat.replaceAll(" ", "").length();
							}
						}
						if (line.indexOf("<Anhang: ") < 0)
							lastChat = line.substring(line.indexOf(": ") + 2);
					} else
						lastChat += " " + line;
				}
				return attributes;
			}
		}

		private void create() throws IOException, DocumentException {
			total.clear();
			try (final BufferedReader chat = new BufferedReader(new FileReader(dir.resolve("_chat.txt").toFile()))) {
				document.open();
				final Pattern start = Pattern.compile("^.?\\[\\d\\d.\\d\\d.\\d\\d, \\d\\d:\\d\\d:\\d\\d\\] ([^:].*?)");
				String e, s[], currentDate = null, lastChat = null, me = "man", line, user = null;
				while ((line = chat.readLine()) != null) {
					line = line.replaceAll("\u200E", "");
					if (line.trim().length() > 0 && start.matcher(line).matches()) {
						user = line.substring(line.indexOf("]") + 1, line.indexOf(":", line.indexOf("]"))).trim();
						if (lastChat != null) {
							// addElement("msg", lastChat);
							// document.body.append(currentChat);
							if (!tableOfContent.users.containsKey(user))
								tableOfContent.users.put(user, new Statistics());
							tableOfContent.users.get(user).chats++;
							if (lastChat != null) {
								lastChat = sanitize(lastChat);
								lastChat = lastChat.replaceAll("\t", " ");
								lastChat = lastChat.replaceAll("\r", " ");
								lastChat = lastChat.replaceAll("\n", " ");
								while (lastChat.indexOf("  ") > -1)
									lastChat = lastChat.replaceAll("  ", " ");
								tableOfContent.users.get(user).words += lastChat.split(" ").length;
								tableOfContent.users.get(user).letters += lastChat.replaceAll(" ", "").length();
							}
							// currentChat = document.createElement("chat");
						}
						s = line.split(" ");
						if (currentDate != s[0]) {
							currentDate = s[0];
							addDate(currentDate);
						}
						// addElement("time", s[1].replace("]", ""));
						// e = addElement("user", line.substring(line.indexOf("]") + 1, line.indexOf(":
						// ")).trim());
						// currentChat.classList = "user" + (e.innerText == me ? "Me" : "");
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
						lastChat += "<br/>" + line;
				}
				document.close();
			}
			Files.move(dir.resolve(filename + ".tmp"), dir.resolve(filename + ".pdf"));
		}

		private void addDate(final String date) throws DocumentException {
			if (tableOfContent.date != null) {
				String s = tableOfContent.date;
				for (final Map.Entry<String, Statistics> entry : tableOfContent.users.entrySet()) {
					final Statistics user = entry.getValue();
					s += entry.getKey() + " - " + user.chats + " ";
					if (!total.containsKey(entry.getKey()))
						total.put(entry.getKey(), new Statistics());
					total.get(entry.getKey()).chats += user.chats;
					total.get(entry.getKey()).words += user.words;
					total.get(entry.getKey()).letters += user.letters;
				}
				final Chunk tableOfContentEntry = new Chunk(s.trim());
				tableOfContentEntry.setLocalGoto(tableOfContent.date.replaceAll(".", ""));
				document.add(new Paragraph(tableOfContentEntry));
			}

			tableOfContent.date = date.replace("[", "").replace(",", "").trim();
			tableOfContent.users.clear();

			final Chunk dateHeader = new Chunk(tableOfContent.date);
			dateHeader.setLocalDestination(tableOfContent.date.replaceAll(".", ""));
			content.add(new Paragraph(dateHeader));
		}

		private String sanitize(String s) {
			return s.replace("<Diese Nachricht wurde bearbeitet.>", "").replaceAll("<", "&lt;").trim();
		}

		private void addMetaData() {
			document.addTitle("My first PDF");
			document.addSubject("Using iText");
			document.addKeywords("Java, PDF, iText");
			document.addAuthor("Lars Vogel");
			document.addCreator("Lars Vogel");
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