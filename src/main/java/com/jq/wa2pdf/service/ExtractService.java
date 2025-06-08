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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jq.wa2pdf.service.PdfService.Statistics;

@Service
public class ExtractService {

	public static class Attributes {
		private final List<Statistics> users = new ArrayList<>();
		private final List<Statistics> months = new ArrayList<>();
		private final String id;

		public Attributes(String id) {
			this.id = id;
		}

		public List<Statistics> getUsers() {
			return users;
		}

		public List<Statistics> getMonths() {
			return months;
		}

		public String getId() {
			return id;
		}
	}

	public static Path getTempDir(final String id) {
		return Paths.get(System.getProperty("java.io.tmpdir")).resolve("whatsapp2pdf_" + id);
	}

	@SuppressWarnings("null")
	public Attributes unzip(final MultipartFile file, final String id) throws IOException {
		final Path targetDir = getTempDir(id).toAbsolutePath();
		Files.createDirectories(targetDir);
		try (final ZipInputStream zipIn = new ZipInputStream(file.getInputStream());
				final FileOutputStream filename = new FileOutputStream(
						targetDir.resolve(PdfService.filename + "Filename").toFile())) {
			for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null;) {
				final Path resolvedPath = targetDir.resolve(ze.getName()).normalize();
				if (!resolvedPath.startsWith(targetDir)) {
					// Hacker attack!
					throw new RuntimeException("Entry with an illegal path: "
							+ ze.getName());
				}
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
		return analyse(id);
	}

	public void cleanUp(final String id) throws IOException {
		FileUtils.deleteDirectory(ExtractService.getTempDir(id).toAbsolutePath().toFile());
	}

	private Attributes analyse(final String id) throws IOException {
		try (final BufferedReader chat = new BufferedReader(
				new FileReader(ExtractService.getTempDir(id).toAbsolutePath().resolve("_chat.txt").toFile()))) {
			final Attributes attributes = new Attributes(id);
			final Pattern start = Pattern.compile("^.?\\[\\d\\d.\\d\\d.\\d\\d, \\d\\d:\\d\\d:\\d\\d\\] ([^:].*?)");
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
					s = s[0].replace("[", "").replace(",", "").trim().split("\\.");
					if (currentDate != s[2] + "-" + s[1]) {
						currentDate = s[2] + "-" + s[1];
						if (attributes.months.size() == 0
								|| !attributes.months.get(attributes.months.size() - 1).month.equals(currentDate)) {
							final Statistics statistics = new Statistics();
							statistics.month = currentDate;
							attributes.months.add(statistics);
						}
						final Statistics month = attributes.months.get(attributes.months.size() - 1);
						month.chats++;
						if (lastChat != null) {
							lastChat = lastChat.replaceAll("\t", " ");
							lastChat = lastChat.replaceAll("\r", " ");
							lastChat = lastChat.replaceAll("\n", " ");
							while (lastChat.indexOf("  ") > -1)
								lastChat = lastChat.replaceAll("  ", " ");
							month.words += lastChat.split(" ").length;
							month.letters += lastChat.replaceAll(" ", "").length();
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
}
