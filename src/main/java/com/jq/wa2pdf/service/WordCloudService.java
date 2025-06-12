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
	private final Pattern sanatize = Pattern.compile("[ \t\r\n\\,\\.\\-\\!\\?\\[\\]\\{\\}';:/\\(\\)0-9]");

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

	public List<Token> extract(final String text) throws IOException {
		String s = sanatize.matcher(text).replaceAll(" ").replaceAll("  ", " ").trim();
		final List<String> emojis = EmojiParser.extractEmojis(text);
		for (String emoji : emojis)
			s = s.substring(0, s.indexOf(emoji)) + s.substring(s.indexOf(emoji) + emoji.length());
		final List<String> stopWords = Arrays.asList(
				IOUtils.toString(getClass().getResourceAsStream("/stopWords.txt"), StandardCharsets.UTF_8).split("\n"));
		final List<Token> list = new ArrayList<>();
		for (String s2 : s.toLowerCase().split(" ")) {
			final Token t = list.stream().filter(e -> e.token.equals(s2)).findFirst().orElse(null);
			if (t != null)
				t.count++;
			else if (!stopWords.contains(s))
				list.add(new Token(s));
		}
		list.sort((e, e2) -> e2.count - e.count);
		return list;
	}

	public void createImage(final List<Token> tokens, final Path file) throws IOException, FontFormatException {
		final BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
		final Graphics2D g = image.createGraphics();
		g.setFont(Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream("/Comfortaa-Regular.ttf")));
		for (int i = 0; i < tokens.size() && i < 10; i++) {
			final Token token = tokens.get(i);
			g.setColor(Color.blue);
			g.drawString(token.getToken(), 10, (i + 1) * 20);
		}
		final File f = file.toAbsolutePath().toFile();
		ImageIO.write(image, f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('.') + 1), f);
	}
}
