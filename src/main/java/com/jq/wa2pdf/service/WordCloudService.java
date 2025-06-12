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

import com.jq.wa2pdf.service.PdfService.Statistics;

@Service
public class WordCloudService {
	public static class Token {
		int count = 1;
		final String token;

		private Token(String s) {
			token = s;
		}
	}

	public List<Token> extract(final String text) {
		String s = text.replaceAll("[ \t\r\n\\,\\.\\-\\!\\?\\[\\]\\{\\}';:/\\(\\)0-9]", " ");
		while (s.contains("  "))
			s = s.replaceAll("  ", " ");
		final List<String> stopWords = Arrays.asList(IOUtils.toString(getClass().getResourceAsStream("/stopWords.txt"), StandardCharsets.UTF_8).split("\n"));
		final List<Token> list = new ArrayList<>();
		for (s : s.toLowerCase().split(" ")) {
			final Token t = list.stream().filter(e -> e.token.equals(s)).findFirst().orElse(null);
			if (t != null)
				t.count++;
			else if (!stopWords.contains(s))
				list.add(new Token(s));
		}
		list.sort((e, e2) -> e2.count - e.count);
		return list;
	}
}
