package com.jq.wa2pdf.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.jq.wa2pdf.entity.Ticket;
import com.jq.wa2pdf.service.AdminService;

import ezvcard.Ezvcard;
import ezvcard.property.Address;
import ezvcard.property.Agent;
import ezvcard.property.BinaryProperty;
import ezvcard.property.ListProperty;
import ezvcard.property.SimpleProperty;
import ezvcard.property.StructuredName;
import ezvcard.property.VCardProperty;

public class Utilities {
	public static String stackTraceToString(final Throwable ex) {
		if (ex == null)
			return "";
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ex.printStackTrace(new PrintStream(baos));
		String s = new String(baos.toByteArray());
		if (s.indexOf(ex.getClass().getName()) < 0)
			s = ex.getClass().getName() + ": " + s;
		return s.replaceAll("\r", "").replaceAll("\n\n", "\n");
	}

	public static String trim(String s, final int length) {
		if (s != null)
			s = s.replaceAll("\r", "").replaceAll("\n\n", "\n").trim();
		return s != null && s.length() > length ? s.substring(0, length - 1) + "…" : s;
	}

	public static String extractUser(final String line, final String separator) {
		final String s = separator == null ? line
				: line
						.substring(line.indexOf(separator) + 1, line.indexOf(":", line.indexOf(separator)))
						.trim();
		String result = "";
		for (int i = 0; i < s.length(); i++) {
			if (Character.isLetterOrDigit(s.charAt(i)))
				result += s.charAt(i);
			else if (Character.isWhitespace(s.charAt(i)))
				result += " ";
		}
		result = result.trim();
		return result.length() == 0 ? "" + s.hashCode() : result;
	}

	public static List<String> createAdjectives(final boolean emojis) {
		final List<String> source = emojis ? Arrays.asList("🏋️", "🍾", "🤯", "❤️‍🔥", "💕", "🥳", "😔", "😇", "😎",
				"🥸", "🦍", "🐦", "🐘", "🦊", "🦅")
				: Arrays.asList("affectionate", "passionate", "intense", "funny", "sexy",
						"loving", "clingy", "vulnerable", "thoughtful", "poetic", "caring", "inquisitive",
						"communicative", "recovering", "observant", "planning");
		final List<String> list = new ArrayList<>();
		final Set<Integer> used = new HashSet<>();
		for (int i = 0; i < 3; i++) {
			while (true) {
				final int i2 = (int) (Math.random() * source.size());
				if (!used.contains(i2)) {
					used.add(i2);
					list.add(source.get(i2));
					break;
				}
			}
		}
		return list;
	}

	public static String getEmojiId(String emoji) {
		for (int i = 0; i < emoji.length(); i++) {
			if (Character.isWhitespace(emoji.charAt(i))) {
				emoji = emoji.substring(0, i);
				break;
			}
		}
		String id = "";
		for (int i = 0; i < emoji.length(); i++) {
			id += "_" + Integer.toHexString(emoji.codePointAt(i));
			if (emoji.codePointAt(i) > 65536)
				i++;
		}
		if (id.length() > 0)
			id = id.substring(1);
		if (Utilities.class.getResourceAsStream("/emoji/" + id + ".png") == null) {
			while (id.contains("_")) {
				id = id.substring(0, id.lastIndexOf('_'));
				if (Utilities.class.getResourceAsStream("/emoji/" + id + ".png") != null)
					break;
			}
		}
		if (Utilities.class.getResourceAsStream("/emoji/" + id + "_fe0f.png") != null)
			id += "_fe0f";
		return Utilities.class.getResourceAsStream("/emoji/" + id + ".png") == null ? null : id;
	}

	public static String formatVCard(final String vCard, final AdminService adminService) {
		final Collection<VCardProperty> properties = Ezvcard.parse(vCard).first().getProperties();
		final StringBuilder sb = new StringBuilder();
		properties.forEach(property -> {
			sb.append("\n");
			String value = null;
			if (property instanceof SimpleProperty)
				value = ((SimpleProperty<?>) property).getValue().toString();
			else if (property instanceof ListProperty)
				value = ((ListProperty<?>) property).getValues().stream().map(e -> e.toString())
						.collect(Collectors.joining(", "));
			else if (property instanceof Agent)
				value = ((Agent) property).getUrl(); // nested vCard not supported, ignore rest of it
			else if (property instanceof StructuredName) {
				final StructuredName name = (StructuredName) property;
				value = name.getPrefixes() == null ? "" : name.getPrefixes().stream().collect(Collectors.joining(" "));
				value += name.getGiven() == null ? "" : " " + name.getGiven();
				value += name.getFamily() == null ? "" : " " + name.getFamily();
				value = name.getSuffixes() == null ? ""
						: " " + name.getSuffixes().stream().collect(Collectors.joining(" "));
			} else if (property instanceof Address) {
				final Address address = (Address) property;
				value = address.getPoBox() == null ? "" : address.getPoBox();
				value += address.getExtendedAddress() == null ? "" : "\n" + address.getExtendedAddress();
				value += address.getStreetAddress() == null ? "" : "\n" + address.getStreetAddress();
				value += address.getPostalCode() == null ? "" : "\n" + address.getPostalCode();
				value += address.getLocality() == null ? ""
						: (address.getPostalCode() == null ? "\n" : " ") + address.getLocality();
				value += address.getRegion() == null ? "" : "\n" + address.getRegion();
				value += address.getCountry() == null ? "" : "\n" + address.getCountry();
			} else if (property instanceof BinaryProperty) {
				if (((BinaryProperty<?>) property).getUrl() != null)
					value = ((BinaryProperty<?>) property).getUrl();
			} else {
				try {
					value = property.getClass().getDeclaredMethod("getText").invoke(property).toString();
				} catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
					adminService.createTicket(new Ticket(Ticket.ERROR + "vCard " + property));
				}
			}
			if (value != null) {
				sb.append(value);
				if (property.getParameters().getType() != null)
					sb.append(" · " + property.getParameters().getType());
			}
		});
		return sb.toString().replace("\n\n", "\n").trim();
	}
}