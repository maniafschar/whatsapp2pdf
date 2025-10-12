package com.jq.wa2pdf.util;

public class DateHandler {
	public static String replaceDay(String date, final String dateFormat) {
		date = date.substring(0, date.indexOf(' ')).replace(",", "");
		if (dateFormat.startsWith("M/d/"))
			return date.split("/")[0] + "/\\d/" + date.split("/")[2];
		if (dateFormat.contains("/"))
			return "\\d/" + date.split("/")[1] + "/" + date.split("/")[2];
		if (dateFormat.contains("."))
			return "\\d" + date.substring(date.indexOf('.'));
		if (dateFormat.contains("-"))
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
		if (date.contains("/")) {
			final String[] test = date.split("/");
			if (Integer.parseInt(test[0]) < 13)
				return "M/d/" + (test[2].length() > 2 ? "yyyy" : "yy");
		}
		if (date.contains("/"))
			return "d/M/" + (date.split("/")[2].length() > 2 ? "yyyy" : "yy");
		if (date.contains("."))
			return "d.M." + (date.split("\\.")[2].length() > 2 ? "yyyy" : "yy");
		if (date.contains("-"))
			return (date.split("-")[0].length() > 2 ? "yyyy" : "yy") + "-M-d";
		throw new IllegalArgumentException("Unknown date format: " + date);
	}

	public static String dateFormatWithoutYear(final String format) {
		if (format.contains("/"))
			return format.replace("/yyyy", "").replace("/yy", "");
		if (format.contains("."))
			return format.replace(".yyyy", "").replace(".yy", "");
		return format.replace("yyyy-", "").replace("yy-", "");
	}
}