package com.jq.wa2pdf.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jq.wa2pdf.entity.Ticket;
import com.jq.wa2pdf.util.Utilities;

@Service
class AIService {
	@Autowired
	private AdminService adminService;

	@Value("${app.chatGPT.key}")
	private String chatGpt;

	public String summerize(final String text) {
		try (final InputStream in = this.getClass().getResourceAsStream("/gpt.json")) {
			final String s = WebClient
					.create("https://api.openai.com/v1/completions")
					.post().accept(MediaType.APPLICATION_JSON)
					.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
					.header("Authorization", "Bearer " + this.chatGpt)
					.bodyValue(IOUtils.toString(in, StandardCharsets.UTF_8)
							.replace("{chat}", text.replace("\"", "\\\"").replace("\n", "\\n")))
					.retrieve().toEntity(String.class).block().getBody();
			return new ObjectMapper().readTree(s).get("choices").get(0).get("text").asText().trim();
		} catch (final Exception ex) {
			this.adminService.createTicket(new Ticket(Utilities.stackTraceToString(ex)));
			return "";
		}
	}
}
