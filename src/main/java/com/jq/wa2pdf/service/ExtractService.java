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
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jq.wa2pdf.service.PdfService.Statistics;
import com.jq.wa2pdf.util.DateHandler;

@Service
public class ExtractService {
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
		return IOUtils.toString(ExtractService.getTempDir(id).resolve(PdfService.filename + "Filename").toUri().toURL(),
				StandardCharsets.UTF_8);
	}

	public Path getFilenameChat(final String id) throws IOException {
		final Path tempDir = getTempDir(id);
		String filename = this.getFilename(id);
		filename = filename.substring(0, filename.lastIndexOf('.')) + ".txt";
		return tempDir.resolve(tempDir.resolve(filename).toFile().exists() ? filename : "_chat.txt");
	}

	@SuppressWarnings("null")
	private void unzip(final MultipartFile file, final String id) throws IOException {
		final Path targetDir = getTempDir(id).toAbsolutePath();
		Files.createDirectories(targetDir);
		try (final ZipInputStream zipIn = new ZipInputStream(file.getInputStream());
				final FileOutputStream filename = new FileOutputStream(
						targetDir.resolve(PdfService.filename + "Filename").toFile())) {
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

	Pattern createMadiaPattern() {
		return Pattern.compile(
				".?<[a-zA-Z]{4,10}: [0-9]{8}-[A-Z]{4,10}-[0-9]{4}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}-[0-9]{2}\\.[a-z0-9]{3,4}>");

	}

	public Attributes analyse(final MultipartFile file, final String id) throws IOException {
		this.unzip(file, id);
		try (final BufferedReader chat = new BufferedReader(new FileReader(this.getFilenameChat(id).toFile()))) {
			final Attributes attributes = new Attributes(id);
			final Pattern start = Pattern.compile("^.?\\[[0-9/:-\\\\,\\\\. (|\\u202fAM|\\u202fPM)]+\\] ([^:].*?)");
			final Pattern media = this.createMadiaPattern();
			String s[], currentDate = null, lastChat = null, line;
			while ((line = chat.readLine()) != null) {
				if (line.trim().length() > 0 && start.matcher(line).matches()) {
					final String user = line.substring(line.indexOf("]") + 1, line.indexOf(":", line.indexOf("]")))
							.trim();
					if (lastChat != null) {
						Statistics u = attributes.users.stream().filter(e -> e.user.equals(user)).findFirst()
								.orElse(null);
						if (u == null) {
							u = new Statistics();
							u.user = user;
							attributes.users.add(u);
						}
						u.chats++;
						if (lastChat != null) {
							lastChat = lastChat.replaceAll("\t", " ");
							lastChat = lastChat.replaceAll("\r", " ");
							lastChat = lastChat.replaceAll("\n", " ");
							while (lastChat.indexOf("  ") > -1)
								lastChat = lastChat.replaceAll("  ", "");
							u.words += lastChat.split(" ").length;
							u.letters += lastChat.replaceAll(" ", "").length();
						}
					}
					s = line.split(" ");
					s[0] = DateHandler.replaceDay(s[0].replace("[", "").replace(",", "").trim());
					if (currentDate == null || !currentDate.equals(s[0])) {
						currentDate = s[0];
						if (attributes.periods.size() == 0
								|| !attributes.periods.get(attributes.periods.size() - 1).period.equals(currentDate)) {
							final Statistics statistics = new Statistics();
							statistics.period = currentDate;
							attributes.periods.add(statistics);
						}
					}
					final Statistics period = attributes.periods.get(attributes.periods.size() - 1);
					period.chats++;
					if (lastChat != null) {
						lastChat = lastChat.replaceAll("\t", " ");
						lastChat = lastChat.replaceAll("\r", " ");
						lastChat = lastChat.replaceAll("\n", " ");
						while (lastChat.indexOf("  ") > -1)
							lastChat = lastChat.replaceAll("  ", " ");
						period.words += lastChat.split(" ").length;
						period.letters += lastChat.replaceAll(" ", "").length();
					}
					if (!media.matcher(line).matches())
						lastChat = line.substring(line.indexOf(": ") + 2);
				} else
					lastChat += " " + line;
			}
			new ObjectMapper().writeValue(
					ExtractService.getTempDir(id).resolve(PdfService.filename + "Attributes").toFile(), attributes);
			return attributes;
		}
	}
}
