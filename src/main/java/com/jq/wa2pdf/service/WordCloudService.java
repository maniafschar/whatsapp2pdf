package com.jq.wa2pdf.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
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
				g.setFont(g.getFont().deriveFont(AffineTransform.getRotateInstance(Math.PI * 1.5)));
			g.drawString(position.token.getText(), position.x, position.y);
			g.drawLine(position.x, position.y, position.x + 10, position.y);
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
		final Polygon surrounding = new Polygon();
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
			else if (!positionFringe(next, positions, surrounding))
				break;
			positions.add(next);
			surrounding.addPoint(next.x, next.y);
			surrounding.addPoint(next.x + next.width, next.y);
			surrounding.addPoint(next.x + next.width, next.y + next.height);
			surrounding.addPoint(next.x, next.y + next.height);
		}
		return positions;
	}

	private void positionVerticalTopLeft(final Position position, final Position relative) {
		position.x = relative.x + decendent(position.height);
		position.y = relative.y - decendent(relative.height);
		position.vertical = true;
	}

	private void positionVerticalLeftAlignEnd(final Position position, final Position relative) {
		position.x = relative.x - position.height;
		position.y = relative.y - decendent(position.height) + position.width;
		position.vertical = true;
	}

	private boolean positionFringe(final Position position, final List<Position> positions, final Polygon surrounding) {
		final List<Position> p = positions.stream().filter(e -> !e.fringe).collect(Collectors.toList());
		final int offset = (int) (Math.random() * p.size());
		final Rectangle box = surrounding.getBounds();
		for (int i = 0; i < p.size(); i++) {
			final Position candidate = p.get((i + offset) % p.size());
			if (candidate.vertical) {
				int x = candidate.x;
				int y = candidate.y - decendent(candidate.height);
				if (candidate.x < box.getWidth() / 2)
					x -= position.width;
				while (x < candidate.x + candidate.width) {
					if (!surrounding.intersects(x, y, position.width, position.height)) {
						position.x = x;
						position.y = y;
						position.fringe = true;
						return true;
					}
					y += position.height;
				}
			} else {
				int x = candidate.x;
				int y = candidate.y - decendent(candidate.height);
				if (candidate.y > box.getHeight() / 2)
					y += position.height;
				while (x < candidate.x + candidate.width) {
					if (!surrounding.intersects(x, y, position.height, position.width)) {
						position.x = x;
						position.y = y;
						position.fringe = true;
						position.vertical = true;
						return true;
					}
					x += position.height;
				}
			}
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

		@Override
		public String toString() {
			return "{" + token.getText() + ": " + x + ", " + y + ", " + width + ", " + height + ", " + vertical + "}";
		}
	}
}
