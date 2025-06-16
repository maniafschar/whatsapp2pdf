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
import org.springframework.stereotype.Service;

@Service
public class ChartService {
	public void createImage(final List<Statistics> data, final Path file) throws IOException {
		final Number[][] data = new Integer[][] {{-3, -2}, {-1, 1}, {2, 3}};
		final CategoryDataset dataset = DatasetUtils.createCategoryDataset("S", "C", data);
		final JFreeChart chart = ChartFactory.createLineChart("Line Chart", "Domain", "Range", dataset);
		((CategoryPlot) chart.getPlot()).setDataset(newData);

		final BufferedImage image = new BufferedImage(200 , 100, BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D g2 = image.createGraphics();
		chart.draw(g2, new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight()), null, null);
		g2.dispose();
		final File f = file.toAbsolutePath().toFile();
		ImageIO.write(image, f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('.') + 1), f);
	}
}
