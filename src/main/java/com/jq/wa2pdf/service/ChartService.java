package com.jq.wa2pdf.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
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
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.jq.wa2pdf.service.PdfService.Statistics;

@Service
public class ChartService {
	public void createImage(final List<Statistics> data, final Path file, final boolean preview)
			throws IOException, ParseException {
		final List<String> periods = expandPeriods(data.stream().map(e -> e.getPeriod()).collect(Collectors.toList()));
		final BufferedImage image = new BufferedImage(800, 350, BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D g = image.createGraphics();
		final int marginLegend = 40;
		final int marginPlot = 10;
		final int heightPlot = (image.getHeight() - marginLegend - 2 * marginPlot) / 3;
		final int marginX = (image.getWidth() - marginLegend) / periods.size();
		drawLegend(g, data, image.getWidth(), image.getHeight(), marginLegend, marginPlot, marginX, heightPlot,
				periods);
		drawChart(g, data, image.getWidth(), image.getHeight(), marginLegend, marginPlot, marginX, heightPlot, periods,
				preview);
		g.dispose();
		image.flush();
		final File f = file.toAbsolutePath().toFile();
		ImageIO.write(image, f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('.') + 1), f);
	}

	private List<String> expandPeriods(List<String> periods) throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat(
				periods.get(0).contains("/") ? "MM/dd/yy" : periods.get(0).contains(".") ? "dd.MM.yy" : "yy-MM-dd");
		final GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(formatter.parse(periods.get(0)));
		gc.set(Calendar.DATE, 1);
		final int month = gc.get(Calendar.MONTH);
		final List<String> result = new ArrayList<>();
		formatter = new SimpleDateFormat(
				formatter.toPattern().replace("/yy", "").replace(".yy", "").replace("yy-", ""));
		while (month == gc.get(Calendar.MONTH)) {
			result.add(formatter.format(gc.getTime()));
			gc.add(Calendar.DATE, 1);
		}
		return result;
	}

	private void drawChart(final Graphics2D g, final List<Statistics> data, final int width, final int height,
			final int marginLegend, final int marginPlot, final int marginX, final int heightPlot,
			final List<String> periods, final boolean preview) {
		int chatsMax = 0, wordsMax = 0, lettersMax = 0;
		final List<Plot> plots = new ArrayList<>();
		for (Statistics statistics : data) {
			if (chatsMax < statistics.chats)
				chatsMax = statistics.chats;
			if (wordsMax < statistics.words)
				wordsMax = statistics.words;
			if (lettersMax < statistics.letters)
				lettersMax = statistics.letters;
		}
		for (int i = 0; i < data.size() && (!preview || i < 400); i++) {
			final Statistics statistics = data.get(i);
			Plot plot = plots.stream().filter(e -> e.user.equals(statistics.user)).findFirst().orElse(null);
			if (plot == null) {
				plot = new Plot(statistics.user);
				plots.add(plot);
			}
			final int x = marginLegend
					+ marginX * (1 + periods
							.indexOf(periods.stream().filter(e -> statistics.period.contains(e)).findFirst().get()));
			plot.chats.addPoint(x, heightPlot - heightPlot * statistics.chats / chatsMax);
			plot.words.addPoint(x, 2 * heightPlot + marginPlot - heightPlot * statistics.words / wordsMax);
			plot.letters.addPoint(x,
					3 * heightPlot + 2 * marginPlot - heightPlot * statistics.letters / lettersMax);
		}
		plots.stream().forEach(e -> {
			g.setColor(Color.BLACK);
			g.drawPolyline(e.chats.xpoints, e.chats.ypoints, e.chats.npoints);
			g.drawPolyline(e.words.xpoints, e.words.ypoints, e.words.npoints);
			g.drawPolyline(e.letters.xpoints, e.letters.ypoints, e.letters.npoints);
		});
	}

	private void drawLegend(final Graphics2D g, final List<Statistics> data, final int width, final int height,
			final int marginLegend, final int marginPlot, final int marginX, final int heightPlot,
			final List<String> periods) {
		int x = marginLegend;
		final Font fontHorizontal = g.getFont();
		final Font fontVertical = g.getFont().deriveFont(AffineTransform.getRotateInstance(Math.PI * 1.5));
		g.setFont(fontVertical);
		g.setColor(new Color(0, 0, 0, 200));

		for (String period : periods)
			g.drawString(period, x += marginX, height);

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
	}

	private class Plot {
		private final String user;
		private final Polygon chats = new Polygon();
		private final Polygon words = new Polygon();
		private final Polygon letters = new Polygon();

		private Plot(String user) {
			this.user = user;
		}
	}
}