package com.jq.wa2pdf;

import javax.imageio.spi.IIORegistry;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableAsync;

import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi;

@SpringBootApplication
@EnableAsync
public class WhatsApp2PdfApplication {
	public static void main(String[] args) {
		IIORegistry.getDefaultInstance().registerServiceProvider(new WebPImageReaderSpi());
		new SpringApplicationBuilder(WhatsApp2PdfApplication.class).run(args);
	}
}