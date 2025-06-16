package com.jq.wa2pdf.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

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
		final XYSeriesCollection data2 = new XYSeriesCollection();
		data.stream().forEach(e -> {
			try {
				data2.getSeries(e.user);
			} catch (UnknownKeyException ex) {
				data2.addSeries(new XYSeries(e.user));
			}
			final XYSeries series = data2.getSeries(e.user);
		});
		final CategoryDataset dataset = DatasetUtils.createCategoryDataset("S", "C",
				new Number[][] { { 1, 2 }, { 2, 8 }, { 3, -6 } });
		final JFreeChart chart = ChartFactory.createLineChart("Line Chart", "Domain", "Range", dataset);
		// decorate(chart);
		final BufferedImage image = new BufferedImage(800, 350, BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D g2 = image.createGraphics();
		chart.draw(g2, new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight()), null, null);
		g2.dispose();
		final File f = file.toAbsolutePath().toFile();
		ImageIO.write(image, f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('.') + 1), f);
	}

	private void decorate(final JFreeChart chart) {
		XYPlot plot = chart.getXYPlot();
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesPaint(0, Color.RED);
		renderer.setSeriesPaint(1, Color.GREEN);
		renderer.setSeriesPaint(2, Color.YELLOW);
		renderer.setSeriesStroke(0, new BasicStroke(4.0f));
		renderer.setSeriesStroke(1, new BasicStroke(3.0f));
		renderer.setSeriesStroke(2, new BasicStroke(2.0f));
		plot.setOutlinePaint(Color.BLUE);
		plot.setOutlineStroke(new BasicStroke(2.0f));
		plot.setRenderer(renderer);
		plot.setBackgroundPaint(Color.DARK_GRAY);
		plot.setRangeGridlinesVisible(true);
		plot.setRangeGridlinePaint(Color.BLACK);
		plot.setDomainGridlinesVisible(true);
		plot.setDomainGridlinePaint(Color.BLACK);
	}
}
