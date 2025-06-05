package com.jq.wa2pdf;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class WhatsApp2PdfApplication {
	public static void main(String[] args) {
		new SpringApplicationBuilder(WhatsApp2PdfApplication.class).run(args);
	}
}