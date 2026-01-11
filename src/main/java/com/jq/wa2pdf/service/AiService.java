package com.jq.wa2pdf.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableList;
import com.google.genai.Client;
import com.google.genai.ResponseStream;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.Type;
import com.jq.wa2pdf.entity.Ticket;
import com.jq.wa2pdf.util.Utilities;

@Service
public class AiService {
	private static AiType type = AiType.Gemini;
	private static final String promptSummerize = "Summarize this WhatsApp chat in about 300 words "
			+ "in the language they speak and at the end of the summary add for each user "
			+ "in one line 3 comma separated adjectives and 3 emojis mainly discribing "
			+ "their mood during conversation:";
	private static final String promptImage = "Create an image expressing the feelings of the people in this text:";

	@Autowired
	private AdminService adminService;

	@Value("${app.chatGPT.key}")
	private String chatGpt;

	@Value("${app.google.gemini.apiKey}")
	private String geminiKey;

	private enum AiType {
		GPT, Gemini, None
	}

	public static class AiSummary {
		public String text;
		public byte[] image;
		public final Map<String, List<String>> adjectives = new HashMap<>();
		public final Map<String, List<String>> emojis = new HashMap<>();
	}

	public AiSummary summerize(final String text, final Set<String> users) {
		if (text.length() < 900)
			return null;
		return type == AiType.Gemini ? this.summerizeGemini(text, users)
				: type == AiType.GPT ? this.summerizeGPT(text, users) : null;
	}

	protected AiSummary summerizeGemini(final String text, final Set<String> users) {
		final List<Content> contents = ImmutableList.of(Content.builder().role("user")
				.parts(ImmutableList.of(Part.fromText(promptSummerize + "\n" + text))).build());
		final Map<String, Schema> attributes = new HashMap<>();
		attributes.put("name", Schema.builder().type(Type.Known.STRING).build());
		attributes.put("adjectives", Schema.builder().type(Type.Known.ARRAY).items(Schema.builder()
				.type(Type.Known.STRING).build()).build());
		attributes.put("emojis", Schema.builder().type(Type.Known.ARRAY).items(Schema.builder()
				.type(Type.Known.STRING).build()).build());
		final Map<String, Schema> schema = new HashMap<>();
		schema.put("summary", Schema.builder().type(Type.Known.STRING).build());
		schema.put("attributes", Schema.builder().type(Type.Known.ARRAY).items(Schema.builder()
				.type(Type.Known.OBJECT).properties(attributes).required(Arrays.asList("name", "adjectives", "emojis"))
				.build()).build());
		final GenerateContentConfig config = GenerateContentConfig.builder()
				.thinkingConfig(ThinkingConfig.builder().thinkingBudget(0).build()).responseMimeType("application/json")
				.responseSchema(Schema.builder()
						.type(Type.Known.OBJECT)
						.properties(schema)
						.required(Arrays.asList("summary", "attributes"))
						.propertyOrdering(Arrays.asList("summary", "attributes"))
						.build())
				.build();
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
			final AiSummary aiSummary = this.convert(s.toString(), users);
			aiSummary.image = this.imageGemini(aiSummary.text);
			return aiSummary;
		}
	}

	private byte[] imageGemini(final String text) {
		final GenerateContentConfig config = GenerateContentConfig.builder()
				.responseModalities(Arrays.asList("IMAGE")).build();
		final GenerateContentResponse generateContentResponse = Client.builder().apiKey(this.geminiKey)
				.build().models.generateContent("gemini-2.5-flash-image", promptImage + "\n" + text, config);
		final ImmutableList<Part> parts = generateContentResponse.parts();
		if (parts != null) {
			for (final Part part : parts) {
				if (part.inlineData().isPresent()) {
					final var blob = part.inlineData().get();
					if (blob.data().isPresent())
						return blob.data().get();
				}
			}
		}
		this.adminService.createTicket(new Ticket(
				Ticket.ERROR + "AI image not created: " + generateContentResponse.finishReason().knownEnum()));
		return null;
	}

	protected AiSummary convert(final String summary, final Set<String> users) {
		String error = "";
		try {
			final JsonNode node = new ObjectMapper().readTree(summary);
			final AiSummary response = new AiSummary();
			response.text = node.get("summary").asText().trim();
			final ArrayNode attributes = (ArrayNode) node.get("attributes");
			for (final String user : users) {
				final String u = user.trim().toLowerCase();
				for (final JsonNode attribute : attributes) {
					if (u.contains(Utilities.extractUser(attribute.get("name").asText(), null).toLowerCase())) {
						response.adjectives.put(user, this.convertList((ArrayNode) attribute.get("adjectives")));
						response.emojis.put(user, this.convertList((ArrayNode) attribute.get("emojis")));
						break;
					}
				}
				if (!response.adjectives.containsKey(user))
					error += user + " not found\n";
			}
			return response;
		} catch (final JsonProcessingException ex) {
			throw new RuntimeException(ex);
		} finally {
			this.adminService.createTicket(new Ticket((error.length() > 0 ? Ticket.ERROR : "") +
					"AI\n" + error + summary));
		}
	}

	private List<String> convertList(final ArrayNode node) {
		final List<String> list = new ArrayList<>();
		for (int i = 0; i < node.size(); i++)
			list.add(node.get(i).asText().toLowerCase().replace("**", ""));
		return list;
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
