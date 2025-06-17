package com.jq.wa2pdf.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
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
import java.util.stream.Collectors;

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

	static class Token {
		int count = 1;
		private final String text;

		Token(String s) {
			text = s;
		}

		public String getText() {
			return text;
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
				final Token token = list.stream().filter(e -> e.text.equals(candidate)).findFirst().orElse(null);
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
			throws IOException {
		final BufferedImage image = new BufferedImage(500, 500, BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D g = image.createGraphics();
		final List<Position> positions = createPositions(tokens, min, max, image, 20.0f);
		System.out.println(positions);
		for (Position position : positions) {
			g.setFont(position.font);
			g.setColor(createColor(position.percent));
			if (position.vertical)
				g.setTransform(AffineTransform.getRotateInstance(Math.PI * 1.5, position.x, position.y));
			g.drawString(position.token.getText(), position.x, position.y - (int) (0.216 * position.height));
			g.drawRect(position.x, position.y - position.height, position.width, position.height);
			g.setTransform(AffineTransform.getRotateInstance(0));
		}
		g.dispose();
		image.flush();
		final File f = file.toAbsolutePath().toFile();
		ImageIO.write(image, f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('.') + 1), f);
	}

	private Color createColor(final double percent) {
		if (percent > 0.666)
			return new Color(0, 0, 255 - (int) (percent * 200));
		if (percent > 0.333)
			return new Color(0, 255 - (int) (percent * 200), 0);
		return new Color(255 - (int) (percent * 200), 0, 0);
	}

	private List<Position> createPositions(final List<Token> tokens, final int min, final int max,
			final BufferedImage image, final float fontSize) {
		final List<Position> positions = new ArrayList<>();
		if (tokens.size() == 0)
			return positions;
		final Graphics2D g = image.createGraphics();
		for (int i = 0; i < tokens.size(); i++) {
			final Token token = tokens.get(i);
			final double percent = ((double) token.getCount() - min) / (max - min);
			g.setFont(FONT.deriveFont((float) ((percent + 1) * fontSize)));
			final Position next = new Position(token, g.getFontMetrics().stringWidth(token.getText()),
					g.getFontMetrics().getHeight(), percent, g.getFont());
			if (i == 0) {
				next.x = (image.getWidth() - next.width) / 2;
				next.y = (image.getHeight() - next.height) / 2;
			} else if (i == 1)
				positionVerticalTopLeft(next, positions.get(positions.size() - 1));
			else if (i == 2)
				positionVerticalLeftAlignEnd(next, positions.get(positions.size() - 1));
			else if (!positionFringe(next, positions))
				break;
			positions.add(next);
		}
		return positions;
	}

	private void positionVerticalTopLeft(final Position position, final Position relative) {
		position.x = relative.x + position.height;
		position.y = relative.y - relative.height;
		position.vertical = true;
	}

	private void positionVerticalLeftAlignEnd(final Position position, final Position relative) {
		position.x = relative.x - relative.height;
		position.y = relative.y - position.height + position.width;
		position.vertical = true;
	}

	private boolean positionFringe(final Position position, final List<Position> positions) {
		final List<Position> p = positions.stream().filter(e -> !e.fringe).collect(Collectors.toList());
		final int offset = 0;// (int) (Math.random() * p.size());
		System.out.println(p);
		System.out.println(offset);
		for (int i = 0; i < p.size(); i++) {
			final Position candidate = p.get((i + offset) % p.size());
			if (candidate.vertical) {
				position.x = candidate.x;
				position.y = candidate.y - candidate.height;
				for (int i2 = 0; i2 < 2; i2++) {
					if (i2 == 1)
						position.x -= position.width;
					while (position.x < candidate.x + candidate.width) {
						if (!intersects(position, positions)) {
							position.fringe = true;
							return true;
						}
						position.y += position.height;
					}
				}
			} else {
				position.x = candidate.x + position.height;
				position.y = candidate.y - candidate.height;
				for (int i2 = 0; i2 < 2; i2++) {
					if (i2 == 1)
						position.y += position.height;
					while (position.x < candidate.x + candidate.width) {
						if (!intersects(position, positions)) {
							position.fringe = true;
							position.vertical = true;
							return true;
						}
						position.x += position.height;
					}
				}
			}
		}
		return false;
	}

	private boolean intersects(final Position position, final List<Position> positions) {
		for (final Position p : positions) {
			if (p.intersects(position))
				return true;
		}
		return false;
	}

	private int decendent(int height) {
		return (int) (0.784 * height);
	}

	private class Position {
		private int x = 0;
		private int y = 0;
		private final int width;
		private final int height;
		private final Token token;
		private final double percent;
		private boolean vertical = false;
		private boolean fringe = false;
		private Font font = null;

		private Position(final Token token, final int width, final int height, final double percent, final Font font) {
			this.token = token;
			this.width = width;
			this.height = height;
			this.percent = percent;
			this.font = font;
		}

		private boolean intersects(Position position) {
			final int x1, y1, w1, h1, x2, y2, w2, h2;
			if (vertical) {
				x1 = x - height;
				y1 = y - width;
				w1 = height;
				h1 = width;
			} else {
				x1 = x - width;
				y1 = y - height;
				w1 = width;
				h1 = height;
			}
			if (position.vertical) {
				x2 = position.x - position.height;
				y2 = position.y - position.width;
				w2 = position.height;
				h2 = position.width;
			} else {
				x2 = position.x - position.width;
				y2 = position.y - position.height;
				w2 = position.width;
				h2 = position.height;
			}
			return (x2 + w2 < x2 || x2 + w2 > x1) &&
					(y2 + h2 < y2 || y2 + h2 > y1) &&
					(x1 + w1 < x1 || x1 + w1 > x2) &&
					(y1 + h1 < y1 || y1 + h1 > y2);
		}

		@Override
		public String toString() {
			return "{" + token.getText() + ": " + x + ", " + y + ", " + width + ", " + height + ", " + vertical + "}";
		}
	}
}
