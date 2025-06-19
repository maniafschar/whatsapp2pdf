package com.jq.wa2pdf.service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

import com.jq.wa2pdf.service.PdfService.Statistics;

@Service
public class ChartService {
	public void createImage(final List<Statistics> data, final Path file) throws IOException, ParseException {
		final List<String> periods = expandPeriods(data.stream().map(e -> e.getPeriod()).collect(Collectors.toList()));
		final BufferedImage image = new BufferedImage(800, 350, BufferedImage.TYPE_4BYTE_ABGR);
		final Graphics2D g = image.createGraphics();
		drawLegend(g, data, image.getWidth(), image.getHeight(), periods);
		drawChart(g, data, image.getWidth(), image.getHeight(), periods);
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
			final List<String> periods) {
		final Set<String> users = new HashSet<>();
		data.stream().forEach(e -> users.add(e.getUser()));
	}

	private void drawLegend(final Graphics2D g, final List<Statistics> data, final int width, final int height,
			final List<String> periods) {
		final int marginLegend = 40, marginPlot = 10, heightPlot = (height - marginLegend - 2 * marginPlot) / 3;
		int x = marginLegend;
		final Font fontHorizontal = g.getFont();
		final Font fontVertical = g.getFont().deriveFont(AffineTransform.getRotateInstance(Math.PI * 1.5));
		g.setFont(fontVertical);
		g.setColor(new Color(0, 0, 0, 200));

		for (String period : periods)
			g.drawString(period, x += (width - marginLegend) / periods.size(), height);

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
}