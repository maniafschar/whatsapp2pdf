package com.jq.wa2pdf.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.UnknownKeyException;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Service;

import com.jq.wa2pdf.service.PdfService.Statistics;

@Service
public class ChartService {
	public void createImage(final List<Statistics> data, final Path file) throws IOException {
		final Set<String> users = new HashSet<>();
		data.stream().forEach(e -> users.add(e.getUser()));
		final Set<String> periods = expandPeriods(data.stream().map(e -> e.getPriod()).collect(Collectors.toList()));

		final BufferedImage image = new BufferedImage(800, 350, BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D g = image.createGraphics();
		drawLegend(g, data, image.getWidth(), image.getHeight());
		users.stream().forEach(user -> {
			drawChart(g, data.stream().filter(e -> user.equals(e.getUser())).collect(Collectors.toList()), image.getWidth(), image.getHeight(), user);
		});
		g.dispose();
		image.flush();
		final File f = file.toAbsolutePath().toFile();
		ImageIO.write(image, f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('.') + 1), f);
	}

	private Set<String> expandPeriods(List<String> periods) {
		SimpleDateFormat formatter = new SimpleDateFormat(periods.get(0).contains("/") ? "MM/dd/yy" : periods.get(0).contains(".") ? "dd.MM.yy" : "yy-MM-dd");
	        final GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(formatter.parse(periods.get(0)));
        	gc.set(Calendar.DATE, 1);
		final int month = gc.get(Calendar.MONTH);
		final Set<String> result = new HashSet<>();
		formatter = new SimpleDateFormat(formatter.toPattern().replace("/yy", "").replace(".yy", "").replace("yy-", ""));
		while (month == gc.get(Calendar.MONTH)) {
			result.add(formatter.format(gc.getTime()));
        		gc.add(Calendar.DATE, 1);
		}
		return result;
	}

	private void drawChart(final Graphics2D g, final List<Statistics> data, final int width, final int height, final Set<String> periods, final String user) {
	}

	private void drawLegend(final Graphics2D g, final List<Statistics> data, final int width, final int height, final Set<String> periods) {
		final int margin = 20;
		g.setStroke(new BasicStroke(4));
		g.setColor(new Color(255, 255, 255, 140));
		g.drawLine(margin, margin, margin, height - margin);
		g.drawLine(margin, margin, width - margin, margin);
		int x = margin;
		for (String period : periods)
			g.drawString(period, x += (width - 2 * margin) / periods.size(), height - 5);
	}
}
