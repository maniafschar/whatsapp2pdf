package com.jq.wa2pdf.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

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
		final String s = line
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

	public static String getEmojiId(String emoji) {
		for (int i = 0; i < emoji.length(); i++) {
			if (Character.isWhitespace(emoji.charAt(i))) {
				emoji = emoji.substring(0, i);
				break;
			}
		}
		String id = "";
		for (int i = 0; i < emoji.length(); i++) {
			if (emoji.codePointAt(i) < 56576 || emoji.codePointAt(i) > 57343)
				id += "_" + Integer.toHexString(emoji.codePointAt(i));
		}
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
		return id;
	}
}