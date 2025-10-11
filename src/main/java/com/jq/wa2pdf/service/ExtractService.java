package com.jq.wa2pdf.service;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jq.wa2pdf.util.DateHandler;

@Service
public class ExtractService {
	public static final String filename = "wa";

	public static class Statistics {
		String user;
		String period;
		int chats = 0;
		int words = 0;
		int letters = 0;
		int media = 0;
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

		public int getMedia() {
			return this.media;
		}

		public String getUser() {
			return this.user;
		}

		public String getPeriod() {
			return this.period;
		}
	}

	public static class Attributes {
		private final List<Statistics> users = new ArrayList<>();
		private final List<Statistics> periods = new ArrayList<>();
		private final String id;

		public Attributes() {
			this.id = null;
		}

		public Attributes(final String id) {
			this.id = id;
		}

		public List<Statistics> getUsers() {
			return this.users;
		}

		public List<Statistics> getPeriods() {
			return this.periods;
		}

		public String getId() {
			return this.id;
		}
	}

	public static Path getTempDir(final String id) {
		return Paths.get(System.getProperty("java.io.tmpdir")).resolve("whatsapp2pdf_" + id);
	}

	public String getFilename(final String id) throws IOException {
		return IOUtils.toString(
				ExtractService.getTempDir(id).resolve(ExtractService.filename + "Filename").toUri().toURL(),
				StandardCharsets.UTF_8);
	}

	public Path getFilenameChat(final String id) throws IOException {
		final Path tempDir = getTempDir(id);
		if (tempDir.resolve("_chat.txt").toFile().exists())
			return tempDir.resolve("_chat.txt");
		String filename = this.getFilename(id);
		filename = filename.substring(0, filename.lastIndexOf('.')) + ".txt";
		if (tempDir.resolve(filename).toFile().exists())
			return tempDir.resolve(filename);
		for (final String file : tempDir.toFile().list()) {
			if (file.toLowerCase().contains("whatsapp") && file.toLowerCase().contains("chat")
					&& file.toLowerCase().endsWith(".txt"))
				return tempDir.resolve(file);
		}
		throw new IOException("Chat file not found!");
	}

	@SuppressWarnings("null")
	private void unzip(final MultipartFile file, final String id) throws IOException {
		final Path targetDir = getTempDir(id).toAbsolutePath();
		Files.createDirectories(targetDir);
		try (final ZipInputStream zipIn = new ZipInputStream(file.getInputStream());
				final FileOutputStream filename = new FileOutputStream(
						targetDir.resolve(ExtractService.filename + "Filename").toFile())) {
			for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null;) {
				final Path resolvedPath = targetDir.resolve(ze.getName()).normalize();
				if (!resolvedPath.startsWith(targetDir))
					// Hacker attack!
					throw new RuntimeException("Entry with an illegal path: " + ze.getName());
				if (ze.isDirectory())
					Files.createDirectories(resolvedPath);
				else {
					Files.createDirectories(resolvedPath.getParent());
					Files.copy(zipIn, resolvedPath);
				}
			}
			filename.write((file.getOriginalFilename() == null ? "WhatsAppChat.zip" : file.getOriginalFilename())
					.getBytes(StandardCharsets.UTF_8));
		}
	}

	public void delete(final String id) throws IOException {
		FileUtils.deleteDirectory(ExtractService.getTempDir(id).toAbsolutePath().toFile());
	}

	String getPatternMadia(final String id) {
		final StringBuilder s = new StringBuilder();
		for (final String file : getTempDir(id).toFile().list()) {
			if (!file.startsWith(filename))
				s.append("|" + file.replace(".", "\\."));
		}
		if (s.length() > 0)
			return ".*(" + s.delete(0, 1) + ").*";
		return "";
	}

	String getPatternStart(final String date) {
		return "^[^\u200E]?((\\[{date}])|({date} -)) ([^:].*?):.*"
				.replace("{date}", (date == null ? "\\d{1,2}[\\.|/]\\d{1,2}[\\.|/]\\d{2,4}" : date)
						+ ", \\d{1,2}:\\d{1,2}(:\\d{1,2})?(|.AM|.PM|.am|.pm)");
	}

	public Attributes analyse(final MultipartFile file, final String id) throws IOException {
		this.unzip(file, id);
		try (final BufferedReader chat = new BufferedReader(new FileReader(this.getFilenameChat(id).toFile()))) {
			final Attributes attributes = new Attributes(id);
			final Pattern start = Pattern.compile(this.getPatternStart(null));
			final Pattern media = Pattern.compile(this.getPatternMadia(id));
			String date, currentDate = null, line, separator = null;
			Statistics user = null, period = null;
			while ((line = chat.readLine()) != null) {
				if (!line.isBlank()) {
					final Matcher matcher = start.matcher(line);
					if (matcher.matches()) {
						if (separator == null)
							separator = line.startsWith("[") || line.substring(1).startsWith("[") ? "]" : "-";
						final String u = line
								.substring(line.indexOf(separator) + 1, line.indexOf(":", line.indexOf(separator)))
								.trim();
						user = attributes.users.stream().filter(e -> e.user.equals(u)).findFirst().orElse(null);
						if (user == null) {
							user = new Statistics();
							user.user = u;
							attributes.users.add(user);
						}
						date = DateHandler.replaceDay(matcher.group(1).trim());
						if (currentDate == null || !currentDate.equals(date)) {
							currentDate = date;
							if (attributes.periods.size() == 0
									|| !attributes.periods.get(attributes.periods.size() - 1).period
											.equals(currentDate)) {
								final Statistics statistics = new Statistics();
								statistics.period = currentDate;
								attributes.periods.add(statistics);
							}
						}
						period = attributes.periods.get(attributes.periods.size() - 1);
						line = line.substring(line.indexOf(':', line.indexOf(separator)) + 1);
					}
				}
				if (user != null && period != null) {
					user.chats++;
					period.chats++;
					if (media.matcher(line).matches()) {
						user.media++;
						period.media++;
					} else {
						line = line.replaceAll("\t", " ");
						while (line.indexOf("  ") > -1)
							line = line.replaceAll("  ", " ");
						final int words = line.trim().split(" ").length;
						final int letters = line.replaceAll(" ", "").length();
						user.words += words;
						user.letters += letters;
						period.words += words;
						period.letters += letters;
					}
				}
			}
			new ObjectMapper().writeValue(
					ExtractService.getTempDir(id).resolve(ExtractService.filename + "Attributes").toFile(), attributes);
			return attributes;
		}
	}
}