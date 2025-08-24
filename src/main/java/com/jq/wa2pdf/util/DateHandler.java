package com.jq.wa2pdf.util;

public class DateHandler {
	public static String replaceDay(final String date) {
		if (date.contains("/"))
			return "\\d\\d/" + date.split("/")[1] + "/" + date.split("/")[2];
		if (date.contains("."))
			return "\\d\\d" + date.substring(date.indexOf('.'));
		if (date.contains("-"))
			return date.substring(0, date.lastIndexOf('-') + 1) + "\\d\\d";
		throw new IllegalArgumentException("Unknown date format: " + date);
	}

	public static String periodSuffix(String period) {
		if (period == null)
			return "";
		if (period.contains("/"))
			period = period.replace("\\d\\d/", "").replace("/", "_");
		else if (period.contains("."))
			period = period.replace("\\d\\d.", "").replace(".", "_");
		else if (period.contains("-"))
			period = period.substring(0, period.lastIndexOf('-')).replace("-", "_");
		return "_" + period;
	}

	public static String dateFormat(final String period) {
		return period.contains("/") ? "dd/MM/yy" : period.contains(".") ? "dd.MM.yy" : "yy-MM-dd";
	}
}