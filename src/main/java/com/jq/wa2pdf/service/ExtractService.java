package com.jq.wa2pdf.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ExtractService {
	static Path getTempDir(final String id) {
		return Paths.get(System.getProperty("java.io.tmpdir")).resolve("whatsapp2pdf_" + id);
	}

	@SuppressWarnings("null")
	Path unzip(final MultipartFile file, final String id) throws IOException {
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
		return targetDir;
	}
}
