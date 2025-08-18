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
}
