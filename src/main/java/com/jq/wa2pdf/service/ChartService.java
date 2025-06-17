package com.jq.wa2pdf.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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
		final Set<String> periods = new HashSet<>();
		data.stream().forEach(e -> periods.add(e.getPriod()));

		final BufferedImage image = new BufferedImage(800, 350, BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D g = image.createGraphics();
		users.stream().forEach(user -> {
			draw(g, user, data.stream().filter(e -> user.equals(e.getUser())).collect(Collectors.toList()));
		});
		g.dispose();
		image.flush();
		final File f = file.toAbsolutePath().toFile();
		ImageIO.write(image, f.getAbsolutePath().substring(f.getAbsolutePath().lastIndexOf('.') + 1), f);
	}

	private void draw(final Graphics2D g, final String user, final List<Statistics> data) {
	}
}
