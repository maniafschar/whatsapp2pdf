package com.jq.wa2pdf.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
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
	private final static Font FONT;
	private final Pattern sanatize = Pattern.compile("[ \t\r\n\\,\\.\\-\\!\\?\\[\\]\\{\\}';:/\\(\\)…0-9]");

	static {
		try {
			STOP_WORDS = Arrays.asList(
					IOUtils.toString(WordCloudService.class.getResourceAsStream("/stopWords.txt"),
							StandardCharsets.UTF_8).split("\n"));
			FONT = Font.createFont(Font.TRUETYPE_FONT,
					WordCloudService.class.getResourceAsStream("/font/Comfortaa-Regular.ttf"));
		} catch (Exception ex) {
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
			if (candidate.trim().length() > 1) {
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
		final List<Rectangle> occupied = new ArrayList<>();
		int x = 0, y = 0;
		double rotate = 0;
		for (int i = 0; i < tokens.size(); i++) {
			final Token token = tokens.get(i);
			final double percent = ((double) token.getCount() - min) / (max - min);
			g.setFont(FONT.deriveFont((float) ((percent + 1) * 20f)));
			final int width = g.getFontMetrics().stringWidth(token.getToken());
			if (i == 0) {
				x = (image.getWidth() - width) / 2;
				y = (g.getFontMetrics().getHeight() + image.getHeight()) / 2;
			} else {
				Rectangle o = occupied.get(occupied.size() - 1);
				if (i % 4 == 0) {
					rotate = 0;
					x = o.x;
					o = occupied.get(occupied.size() - 4);
					y = o.y - g.getFontMetrics().getAscent();
				} else if (i % 4 == 1) {
					rotate = Math.PI * 0.5;
					x = o.x + o.width + 2 * g.getFontMetrics().getDescent();
					y = o.y - (int) (0.8 * o.height);
				} else if (i % 4 == 2) {
					rotate = Math.PI;
					o = occupied.get(occupied.size() - 2);
					x = o.x + o.width;
					y = o.y + 2 * g.getFontMetrics().getDescent();
				} else {
					rotate = Math.PI * 1.5;
					x = o.x - o.width - 2 * g.getFontMetrics().getDescent();
					y = o.y - o.height;
					o = occupied.get(occupied.size() - 3);
					if (x > o.x - 2 * g.getFontMetrics().getDescent())
						x = o.x - 2 * g.getFontMetrics().getDescent();
				}
			}
			if (rotate > 0)
				g.setFont(g.getFont().deriveFont(AffineTransform.getRotateInstance(rotate)));
			System.out
					.println(x + " - " + y + " - " + rotate + " - " + token.getToken() + " (" + token.getCount() + ")");
			g.setColor(createColor(token.getCount(), max, min));
			g.drawString(token.getToken(), x, y);
			occupied.add(new Rectangle(x, y, width, g.getFontMetrics().getHeight()));
		}
		g.dispose();
		image.flush();
		final File f = file.toAbsolutePath().toFile();
		ImageIO.write(image, f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('.') + 1), f);
	}

	private Color createColor(final int count, final int max, final int min) {
		final double percent = ((double) count - min) / (max - min);
		if (percent > 0.666)
			return new Color(0, 0, 255 - (int) (percent * 200));
		if (percent > 0.333)
			return new Color(0, 255 - (int) (percent * 200), 0);
		return new Color(255 - (int) (percent * 200), 0, 0);
	}
}
