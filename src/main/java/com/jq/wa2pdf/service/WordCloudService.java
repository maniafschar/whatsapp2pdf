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
		} catch (final Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	static class Token {
		int count = 1;
		private final String text;

		Token(final String s) {
			this.text = s;
		}

		public String getText() {
			return this.text;
		}

		public int getCount() {
			return this.count;
		}
	}

	List<Token> extract(final String text) {
		final StringBuilder s = new StringBuilder(this.sanatize.matcher(text).replaceAll(" "));
		s.trimToSize();
		final List<String> emojis = EmojiParser.extractEmojis(s.toString());
		int i;
		for (final String emoji : emojis)
			s.delete(i = s.indexOf(emoji), i + emoji.length());
		final List<Token> list = new ArrayList<>();
		for (final String candidate : s.toString().toLowerCase().split(" ")) {
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

	void createImage(final List<Token> tokens, final int max, final int min, final Path file)
			throws IOException {
		final BufferedImage image = new BufferedImage(500, 500, BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D g = image.createGraphics();
		final List<Position> positions = this.createPositions(tokens, min, max, image, 20.0f);
		for (final Position position : positions) {
			g.setFont(position.font);
			g.setColor(this.createColor(position.percent));
			if (position.vertical) {
				final AffineTransform t = AffineTransform.getRotateInstance(Math.PI * 1.5, position.x, position.y);
				t.concatenate(AffineTransform.getTranslateInstance(-position.width, 0));
				g.setTransform(t);
			}
			g.drawString(position.token.getText(), position.x, position.y + (int) (0.784 * position.height));
			if (position.vertical)
				g.setTransform(AffineTransform.getRotateInstance(0));
		}
		g.dispose();
		image.flush();
		final File f = file.toAbsolutePath().toFile();
		ImageIO.write(image, f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('.') + 1), f);
	}

	private Color createColor(final double percent) {
		if (percent > 0.45)
			return new Color(0, 0, 255 - (int) (percent * 150));
		if (percent > 0.15)
			return new Color(0, 255 - (int) (percent * 150), 0);
		return new Color(255 - (int) (percent * 150), 0, 0);
	}

	private List<Position> createPositions(final List<Token> tokens, final int min, final int max,
			final BufferedImage image, final float fontSize) {
		final List<Position> positions = new ArrayList<>();
		if (tokens.size() == 0)
			return positions;
		final Graphics2D g = image.createGraphics();
		final int width = image.getWidth();
		final int height = image.getHeight();
		boolean nextLoop = true;
		Position next;
		for (int i = 0; i < tokens.size(); i++) {
			final Token token = tokens.get(i);
			final double percent = ((double) token.getCount() - min) / (max - min);
			g.setFont(FONT.deriveFont((float) ((percent + 1) * fontSize)));
			next = new Position(token, g.getFontMetrics().stringWidth(token.getText()),
					g.getFontMetrics().getHeight(), percent, g.getFont());
			if (i == 0) {
				next.x = (image.getWidth() - next.width) / 2;
				next.y = (image.getHeight() - next.height) / 2;
			} else if (nextLoop)
				nextLoop = this.positionNext(next, positions, width, height);
			else if (i > tokens.size() / 3)
				nextLoop = false;
			if (!nextLoop && !this.positionFringe(next, positions, width, height))
				next = null;
			if (next != null)
				positions.add(next);
			else
				System.out.println("Failed on " + token.text);
		}
		return positions;
	}

	private boolean positionNext(final Position position, final List<Position> positions,
			final int width, final int height) {
		final int offset = (int) (Math.random() * positions.size());
		for (int i = 0; i < positions.size(); i++) {
			final Position candidate = positions.get((i + offset) % positions.size());
			final int x1, x2, x3, x4, y1, y2, y3, y4;
			if (candidate.vertical) {
				position.vertical = false;
				x1 = candidate.x - position.width;
				x2 = candidate.x - position.width + candidate.height;
				x3 = candidate.x;
				x4 = candidate.x + candidate.height;
				y1 = candidate.y - position.height;
				y2 = candidate.y;
				y3 = candidate.y + candidate.width - position.height;
				y4 = candidate.y + candidate.width;
			} else {
				position.vertical = true;
				x1 = candidate.x - position.height;
				x2 = candidate.x;
				x3 = candidate.x + candidate.width - position.height;
				x4 = candidate.x + candidate.width;
				y1 = candidate.y - position.width;
				y2 = candidate.y;
				y3 = candidate.y + candidate.height - position.width;
				y4 = candidate.y + candidate.height;
			}
			for (final Integer[] xy : Arrays.asList(
					new Integer[] { x1, y2 }, new Integer[] { x2, y1 },
					new Integer[] { x3, y1 }, new Integer[] { x4, y2 },
					new Integer[] { x1, y3 }, new Integer[] { x2, y4 },
					new Integer[] { x3, y4 }, new Integer[] { x4, y3 })) {
				position.x = xy[0];
				position.y = xy[1];
				if (this.inside(position, width, height) && this.intersects(position, positions) == null)
					return true;
			}
		}
		return false;
	}

	private boolean positionFringe(final Position position, final List<Position> positions,
			final int width, final int height) {
		final List<Position> p = positions.stream().filter(e -> !e.fringe).collect(Collectors.toList());
		final int offset = (int) (Math.random() * p.size());
		for (int i = 0; i < p.size(); i++) {
			final Position candidate = p.get((i + offset) % p.size());
			if (candidate.vertical) {
				position.x = candidate.x - position.width;
				position.y = candidate.y;
				position.vertical = false;
				for (int i2 = 0; i2 < 2; i2++) {
					if (i2 == 1) {
						position.x = candidate.x + candidate.height;
						position.y = candidate.y;
					}
					while (position.y < candidate.y + candidate.width) {
						final Position intersection = this.intersects(position, positions);
						if (intersection == null) {
							if (this.inside(position, width, height)) {
								position.fringe = true;
								return true;
							}
							position.y += position.width;
						} else
							position.y = intersection.y
									+ (intersection.vertical ? intersection.width : intersection.height);
					}
				}
			} else {
				position.x = candidate.x;
				position.y = candidate.y - position.width;
				position.vertical = true;
				for (int i2 = 0; i2 < 2; i2++) {
					if (i2 == 1) {
						position.x = candidate.x;
						position.y = candidate.y + candidate.height;
					}
					while (position.x < candidate.x + candidate.width) {
						final Position intersection = this.intersects(position, positions);
						if (intersection == null) {
							if (this.inside(position, width, height)) {
								position.fringe = true;
								return true;
							}
							position.x += position.height;
						} else
							position.x = intersection.x
									+ (intersection.vertical ? intersection.height : intersection.width);
					}
				}
			}
		}
		return false;
	}

	private Position intersects(final Position position, final List<Position> positions) {
		for (final Position p : positions) {
			if (p.intersects(position))
				return p;
		}
		return null;
	}

	private boolean inside(final Position position, final int width, final int height) {
		if (position.x < 0 || position.y < 0)
			return false;
		if (position.vertical)
			return position.x + position.height < width && position.y + position.width < height;
		return position.x + position.width < width && position.y + position.height < height;
	}

	private static class Position {
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

		private boolean intersects(final Position position) {
			final int w1, h1, w2, h2;
			if (this.vertical) {
				w1 = this.height;
				h1 = this.width;
			} else {
				w1 = this.width;
				h1 = this.height;
			}
			if (position.vertical) {
				w2 = position.height;
				h2 = position.width;
			} else {
				w2 = position.width;
				h2 = position.height;
			}
			return this.x + w1 > position.x && this.x < position.x + w2 && this.y + h1 > position.y
					&& this.y < position.y + h2;
		}
	}
}