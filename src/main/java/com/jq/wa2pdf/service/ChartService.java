package com.jq.wa2pdf.service;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeListener;
import org.jfree.chart.labels.CategoryToolTipGenerator;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.urls.CategoryURLGenerator;
import org.jfree.chart.urls.StandardCategoryURLGenerator;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.springframework.stereotype.Service;

@Service
public class ChartService {
	public void createImage(final List<Statistics> data, final Path file) throws IOException {
		final XYSeriesCollection data = new XYSeriesCollection();
		data.stream().forEach(e -> {
			if (data.getSeries(e.user) == null)
				data.addSeries(new XYSeries(e.user));
			final XYSeries series = data.getSeries(e.user);
		});
		final CategoryDataset dataset = DatasetUtils.createCategoryDataset("S", "C", data);
		final JFreeChart chart = ChartFactory.createLineChart("Line Chart", "Domain", "Range", dataset);
		decorate(chat);
		final BufferedImage image = new BufferedImage(800 , 500, BufferedImage.TYPE_4BYTE_ABGR);
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
