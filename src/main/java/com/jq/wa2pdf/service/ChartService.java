package com.jq.wa2pdf.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.jq.wa2pdf.service.ExtractService.Statistics;
import com.jq.wa2pdf.util.DateHandler;

@Service
class ChartService {
	void createImage(final List<Statistics> data, final Path file, final Map<String, Color> colors,
			final String dateFormat) throws IOException, ParseException {
		final SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);
		final List<String> xAxis = this.createXAxis(data.get(0).getPeriod(), formatter);
		final BufferedImage image = new BufferedImage(800, 350, BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
		final int marginLegend = 40;
		final int marginPlot = 10;
		final int heightPlot = (image.getHeight() - marginLegend - 2 * marginPlot) / 3;
		final int marginX = (image.getWidth() - marginLegend) / xAxis.size();
		this.drawLegend(g, data, image.getWidth(), image.getHeight(), marginLegend, marginPlot, marginX, heightPlot,
				xAxis, dateFormat);
		this.drawCharts(g, marginLegend, marginPlot, heightPlot,
				this.preparePlotData(data, marginLegend, marginPlot, marginX, heightPlot, xAxis, colors, formatter));
		g.dispose();
		image.flush();
		final File f = file.toAbsolutePath().toFile();
		ImageIO.write(image, f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('.') + 1), f);
	}

	private List<String> createXAxis(final String period, final SimpleDateFormat formatter) throws ParseException {
		final GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(formatter.parse(period));
		gc.set(Calendar.DATE, 1);
		final int month = gc.get(Calendar.MONTH);
		final List<String> result = new ArrayList<>();
		while (month == gc.get(Calendar.MONTH)) {
			result.add(formatter.format(gc.getTime()));
			gc.add(Calendar.DATE, 1);
		}
		return result;
	}

	private PlotData preparePlotData(final List<Statistics> data, final int marginLegend, final int marginPlot,
			final int marginX, final int heightPlot, final List<String> xAxis, final Map<String, Color> colors,
			final SimpleDateFormat formatter) {
		final PlotData plotData = new PlotData();
		for (final Statistics dataUser : data) {
			if (plotData.chatsMax < dataUser.chats)
				plotData.chatsMax = dataUser.chats;
			if (plotData.wordsMax < dataUser.words)
				plotData.wordsMax = dataUser.words;
			if (plotData.lettersMax < dataUser.letters)
				plotData.lettersMax = dataUser.letters;
		}
		for (final Statistics dataUser : data) {
			final String date = xAxis.stream().filter(e -> {
				try {
					return formatter.parse(dataUser.period).equals(formatter.parse(e));
				} catch (final ParseException ex) {
					return false;
				}
			}).findFirst().orElse(null);
			if (date != null) {
				Plot plot = plotData.plots.stream().filter(e -> e.user.equals(dataUser.user)).findFirst()
						.orElse(null);
				if (plot == null) {
					plot = new Plot(dataUser.user, colors.get(dataUser.user));
					plotData.plots.add(plot);
				}
				final int index = xAxis.indexOf(date);
				// add null values before, from beginning or between days
				for (int i = (int) Math.signum(plot.lastIndex); i < index - plot.lastIndex; i++) {
					final int x = marginLegend + marginX * (1 + plot.lastIndex + i);
					plot.chats.addPoint(x, heightPlot);
					plot.words.addPoint(x, 2 * heightPlot + marginPlot);
					plot.letters.addPoint(x, 3 * heightPlot + 2 * marginPlot);
				}
				// print entry
				final int x = marginLegend + marginX * (1 + index);
				plot.chats.addPoint(x, heightPlot - heightPlot * dataUser.chats / plotData.chatsMax);
				plot.words.addPoint(x, 2 * heightPlot + marginPlot - heightPlot * dataUser.words / plotData.wordsMax);
				plot.letters.addPoint(x,
						3 * heightPlot + 2 * marginPlot - heightPlot * dataUser.letters / plotData.lettersMax);
				plot.lastIndex = index;
			}
		}
		// add null values after till end
		plotData.plots.stream().forEach(e -> {
			for (int i = 1; i < xAxis.size() - e.lastIndex; i++) {
				final int x = marginLegend + marginX * (1 + e.lastIndex + i);
				e.chats.addPoint(x, heightPlot);
				e.words.addPoint(x, 2 * heightPlot + marginPlot);
				e.letters.addPoint(x, 3 * heightPlot + 2 * marginPlot);
			}
		});
		return plotData;
	}

	private void drawCharts(final Graphics2D g, final int marginLegend, final int marginPlot,
			final int heightPlot, final PlotData plotData) {
		final Font fontHorizontal = g.getFont();
		final Font fontVertical = g.getFont().deriveFont(AffineTransform.getRotateInstance(Math.PI * 1.5));
		String s = String.format("%,d", plotData.chatsMax);
		int w = g.getFontMetrics().stringWidth(s);
		g.setFont(fontVertical);
		g.drawString(s, marginLegend - marginPlot / 2, w);
		s = String.format("%,d", plotData.wordsMax);
		g.setFont(fontHorizontal);
		w = g.getFontMetrics().stringWidth(s);
		g.setFont(fontVertical);
		g.drawString(s, marginLegend - marginPlot / 2, heightPlot + marginPlot + w);
		s = String.format("%,d", plotData.lettersMax);
		g.setFont(fontHorizontal);
		w = g.getFontMetrics().stringWidth(s);
		g.setFont(fontVertical);
		g.drawString(s, marginLegend - marginPlot / 2, 2 * heightPlot + 2 * marginPlot + w);
		plotData.plots.stream().forEach(e -> {
			g.setColor(e.color);
			g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.drawPolyline(e.chats.xpoints, e.chats.ypoints, e.chats.npoints);
			g.drawPolyline(e.words.xpoints, e.words.ypoints, e.words.npoints);
			g.drawPolyline(e.letters.xpoints, e.letters.ypoints, e.letters.npoints);
		});
	}

	private void drawLegend(final Graphics2D g, final List<Statistics> data, final int width, final int height,
			final int marginLegend, final int marginPlot, final int marginX, final int heightPlot,
			final List<String> xAxis, final String dateFormat) {
		int x = marginLegend;
		final Font fontHorizontal = g.getFont();
		final Font fontVertical = g.getFont().deriveFont(AffineTransform.getRotateInstance(Math.PI * 1.5));
		g.setFont(fontVertical);
		g.setColor(new Color(0, 0, 0, 120));

		for (final String period : xAxis) {
			final String s = DateHandler.removeYear(period, dateFormat);
			g.setFont(fontHorizontal);
			final int stringWidth = g.getFontMetrics().stringWidth(s);
			g.setFont(fontVertical);
			g.drawString(s, x += marginX, height - marginLegend + stringWidth + marginPlot / 2);
		}

		g.setFont(g.getFont().deriveFont(AffineTransform.getRotateInstance(0)));
		final String[] types = { "chats", "words", "letters" };
		for (int i = 0; i < types.length; i++) {
			final int y = (i + 1) * heightPlot + i * marginPlot;
			g.drawLine(marginLegend - marginPlot / 2, y, width, y);
			g.drawLine(marginLegend, y + marginPlot / 2, marginLegend, y - heightPlot);
			g.setFont(fontHorizontal);
			final int stringWidth = g.getFontMetrics().stringWidth(types[i]);
			g.setFont(fontVertical);
			g.drawString(types[i], width - g.getFontMetrics().getHeight(),
					y - heightPlot + stringWidth + marginPlot / 2);
		}
		g.setFont(fontHorizontal);
	}

	private class Plot {
		private final String user;
		private final Polygon chats = new Polygon();
		private final Polygon words = new Polygon();
		private final Polygon letters = new Polygon();
		private final Color color;
		private int lastIndex = 0;

		private Plot(final String user, final Color color) {
			this.user = user;
			this.color = color;
		}
	}

	private class PlotData {
		private int chatsMax = 0;
		private int wordsMax = 0;
		private int lettersMax = 0;
		private final List<Plot> plots = new ArrayList<>();
	}
}
