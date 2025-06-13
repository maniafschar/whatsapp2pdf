package com.jq.wa2pdf.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import com.vdurmont.emoji.EmojiParser;

@Service
public class WordCloudService {
	private final static List<String> STOP_WORDS;
	private final Pattern sanatize = Pattern.compile("[ \t\r\n\\,\\.\\-\\!\\?\\[\\]\\{\\}';:/\\(\\)…0-9]");

	static {
		try {
			STOP_WORDS = Arrays.asList(
					IOUtils.toString(WordCloudService.class.getResourceAsStream("/stopWords.txt"),
							StandardCharsets.UTF_8).split("\n"));
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	public static class Token {
		private int count = 1;
		private final String token;

		private Token(String s) {
			token = s;
		}

		public String getToken() {
			return token;
		}

		public int getCount() {
			return count;
		}
	}

	public List<Token> extract(String text) {
		final StringBuilder s = new StringBuilder(sanatize.matcher(text).replaceAll(" "));
		s.trimToSize();
		final List<String> emojis = EmojiParser.extractEmojis(s.toString());
		int i;
		for (String emoji : emojis)
			s.delete(i = s.indexOf(emoji), i + emoji.length());
		final List<Token> list = new ArrayList<>();
		for (String candidate : s.toString().toLowerCase().split(" ")) {
			if (candidate.trim().length() > 0) {
				final Token token = list.stream().filter(e -> e.token.equals(candidate)).findFirst().orElse(null);
				if (token != null)
					token.count++;
				else if (!STOP_WORDS.contains(candidate))
					list.add(new Token(candidate));
			}
		}
		list.sort((e, e2) -> e2.count - e.count);
		return list;
	}

	public void createImage(final List<Token> tokens, final int max, final int min, final Path file)
			throws IOException, FontFormatException {
		final BufferedImage image = new BufferedImage(500, 500, BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D g = image.createGraphics();
		g.setFont(Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/font/Comfortaa-Regular.ttf"))
				.deriveFont(20f));
		for (int i = 0; i < tokens.size(); i++) {
			final Token token = tokens.get(i);
			g.setColor(Color.blue);
			g.drawString(token.getToken(), 10, (i + 1) * 20);
		}
		final File f = file.toAbsolutePath().toFile();
		ImageIO.write(image, f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('.') + 1), f);
	}
}
