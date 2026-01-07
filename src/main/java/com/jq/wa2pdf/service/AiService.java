package com.jq.wa2pdf.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.GoogleSearch;
import com.google.genai.types.Part;
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.Tool;
import com.jq.wa2pdf.entity.Ticket;
import com.jq.wa2pdf.util.Utilities;
import com.vdurmont.emoji.EmojiParser;

@Service
public class AiService {
	public static AiType type = AiType.Gemini;
	private static final String promptSummerize = "Summarize this WhatsApp chat in about 300 words "
			+ "in the language they speak and at the end of the summary add for each user "
			+ "in one line 3 comma separated adjectives and 3 emojis mainly discribing "
			+ "their mood during conversation:";
	private static final String promptImage = "Summarize this WhatsApp chat in about 300 words "
			+ "in the language they speak and at the end of the summary add for each user "
			+ "in one line 3 comma separated adjectives and 3 emojis mainly discribing "
			+ "their mood during conversation:";

	@Autowired
	private AdminService adminService;

	@Value("${app.chatGPT.key}")
	private String chatGpt;

	@Value("${app.google.gemini.apiKey}")
	private String geminiKey;

	public enum AiType {
		GPT, Gemini, None
	}

	class AiSummary {
		String text;
		byte[] image;
		final Map<String, List<String>> adjectives = new HashMap<>();
		final Map<String, List<String>> emojis = new HashMap<>();
	}

	public AiSummary summerize(final String text, final Set<String> users) {
		if (text.length() < 900)
			return null;
		return type == AiType.Gemini ? this.summerizeGemini(text, users)
				: type == AiType.GPT ? this.summerizeGPT(text, users) : null;
	}

	private AiSummary summerizeGemini(final String text, final Set<String> users) {
		final List<Content> contents = ImmutableList.of(Content.builder().role("user")
				.parts(ImmutableList.of(Part.fromText(promptSummerize + "\n" + text))).build());
		final GenerateContentConfig config = GenerateContentConfig.builder()
				.thinkingConfig(ThinkingConfig.builder().thinkingBudget(0).build())
				.tools(Arrays.asList(Tool.builder().googleSearch(GoogleSearch.builder().build()).build())).build();
		try (final ResponseStream<GenerateContentResponse> responseStream = Client.builder().apiKey(this.geminiKey)
				.build().models.generateContentStream("gemini-2.5-flash-lite", contents, config)) {
			final StringBuffer s = new StringBuffer();
			for (final GenerateContentResponse res : responseStream) {
				if (res.candidates().isEmpty() || res.candidates().get().get(0).content().isEmpty()
						|| res.candidates().get().get(0).content().get().parts().isEmpty())
					continue;
				final List<Part> parts = res.candidates().get().get(0).content().get().parts().get();
				for (final Part part : parts)
					s.append(part.text().orElse(""));
			}
			final AiSummary aiSummary = this.parseAdjectives(s.toString(), users);
			aiSummary.image = this.imageGemini(aiSummary.text);
			return aiSummary;
		}
	}

	private byte[] imageGemini(final String text) {
		final GenerateContentConfig config = GenerateContentConfig.builder()
				.responseModalities(Arrays.asList("IMAGE")).build();
		final GenerateContentResponse generateContentResponse = Client.builder().apiKey(this.geminiKey)
				.build().models.generateContent("gemini-2.5-flash-image", promptImage + "\n" + text, config);
		if (generateContentResponse != null) {
			for (final Part part : generateContentResponse.parts()) {
				if (part.inlineData().isPresent()) {
					final var blob = part.inlineData().get();
					if (blob.data().isPresent())
						return blob.data().get();
				}
			}
		}
		return null;
	}

	AiSummary parseAdjectives(final String summary, final Set<String> users) {
		final AiSummary response = new AiSummary();
		response.text = summary;
		String error = "";
		final StringBuilder adjectives = new StringBuilder(response.text.toLowerCase());
		int first = Integer.MAX_VALUE;
		for (final String user : users) {
			String u = user.trim().toLowerCase();
			Matcher matcher = Pattern.compile("^([^:^\n]*" + u + ")[^:^\n]*:.*$", Pattern.MULTILINE)
					.matcher(adjectives.toString());
			boolean found = matcher.find();
			if (!found && u.contains(" ")) {
				u = u.split(" ")[0];
				matcher = Pattern.compile("^([^:^\n]*" + u + ")[^:^\n]*:.*$", Pattern.MULTILINE)
						.matcher(adjectives.toString());
				found = matcher.find();
			}
			if (found) {
				final int pos = matcher.start(1);
				if (first > pos)
					first = pos;
				this.parseAdjectivesOfUser(response, user, adjectives.substring(pos));
			} else
				error += user + " not found\n";
		}
		if (response.adjectives.size() > 0)
			response.text = response.text.substring(0, first).trim();
		this.adminService.createTicket(new Ticket((error.length() > 0 ? Ticket.ERROR : "") +
				"AI\n" + error + summary));
		return response;
	}

	private void parseAdjectivesOfUser(final AiSummary response, final String user, final String adjectives) {
		if (!response.adjectives.containsKey(user)) {
			response.adjectives.put(user, new ArrayList<>());
			response.emojis.put(user, new ArrayList<>());
		}
		String s = "";
		for (final String line : adjectives.split("\n")) {
			s += (line.contains(":") ? line.substring(line.indexOf(':') + 1) : line).replace("*", "").replace(".", "")
					.trim();
			final List<String> emojis = EmojiParser.extractEmojis(s);
			if (emojis.size() > 0) {
				s = s.substring(0, s.indexOf(emojis.get(0))).trim();
				response.adjectives.get(user).addAll(
						Arrays.asList(s.replace("\ufe0f", "").trim().split(",")).stream()
								.map(e -> e.trim()).collect(Collectors.toList()));
				for (int i = 0; i < emojis.size(); i++) {
					String e = emojis.get(i);
					final int start = line.indexOf(e);
					final int position = Math.min(line.indexOf("\ufe0f", start),
							i < emojis.size() - 1 ? line.indexOf(emojis.get(i + 1)) - 1
									: Integer.MAX_VALUE);
					if (position > -1) {
						for (int i2 = start + e.length(); i2 <= position
								&& line.codePointAt(i2) > 128; i2++)
							e += line.charAt(i2);
					}
					response.emojis.get(user).add(e);
				}
				if (response.adjectives.get(user).size() > 2)
					break;
				s = "";
			}
		}
	}

	private AiSummary summerizeGPT(final String text, final Set<String> users) {
		try (final InputStream in = this.getClass().getResourceAsStream("/gpt.json")) {
			final String s = WebClient
					.create("https://api.openai.com/v1/completions")
					.post().accept(MediaType.APPLICATION_JSON)
					.header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
					.header("Authorization", "Bearer " + this.chatGpt)
					.bodyValue(IOUtils.toString(in, StandardCharsets.UTF_8)
							.replace("{chat}", text.replace("\"", "\\\"").replace("\n", "\\n")))
					.retrieve().toEntity(String.class).block().getBody();
			final AiSummary response = new AiSummary();
			response.text = new ObjectMapper().readTree(s).get("choices").get(0).get("text").asText().trim();
			return response;
		} catch (final Exception ex) {
			ex.printStackTrace();
			this.adminService.createTicket(new Ticket(Ticket.ERROR + Utilities.stackTraceToString(ex)));
			return null;
		}
	}
}
