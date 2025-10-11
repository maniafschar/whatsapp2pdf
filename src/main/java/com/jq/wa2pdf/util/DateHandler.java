package com.jq.wa2pdf.util;

public class DateHandler {
	public static String replaceDay(String date) {
		final boolean ampm = date.toLowerCase().contains("m");
		date = date.substring(0, date.indexOf(' ')).replace(",", "");
		if (date.contains("/") && ampm)
			return date.split("/")[0] + "/\\d/" + date.split("/")[2];
		if (date.contains("/"))
			return "\\d/" + date.split("/")[1] + "/" + date.split("/")[2];
		if (date.contains("."))
			return "\\d" + date.substring(date.indexOf('.'));
		if (date.contains("-"))
			return date.substring(0, date.lastIndexOf('-') + 1) + "\\d";
		throw new IllegalArgumentException("Unknown date format: " + date);
	}

	public static String periodSuffix(String period) {
		if (period == null)
			return "";
		if (period.contains("/"))
			period = period.replace("\\d/", "").replace("/", "_");
		else if (period.contains("."))
			period = period.replace("\\d.", "").replace(".", "_");
		else if (period.contains("-"))
			period = period.substring(0, period.lastIndexOf('-')).replace("-", "_");
		return "_" + period;
	}

	public static String dateFormat(final String date) {
		return date.contains("/") && date.toLowerCase().contains("m") ? "M/d/yy"
				: date.contains("/") ? "d/M/yy" : date.contains(".") ? "d.M.yy" : "yy-M-d";
	}

	public static String removeYear(final String tag, final String pattern) {
		return "yy-M-d".equals(pattern) ? tag.substring(3) : tag.substring(0, tag.length() - 3);
	}
}
