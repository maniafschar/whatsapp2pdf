package com.jq.wa2pdf.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class DateHandler {
	public static String replaceStrangeWhitespace(String date) {
		date = date.replace(". ", ".");
		date = date.split(" ")[0];
		return date.replaceAll("[^0-9/\\-\\.]", "");
	}

	public static String replaceDay(String date, final String dateFormat) throws ParseException {
		date = replaceStrangeWhitespace(date);
		final GregorianCalendar gc = new GregorianCalendar();
		gc.setTime(new SimpleDateFormat(dateFormat).parse(date));
		String year = "" + gc.get(Calendar.YEAR);
		if (!dateFormat.contains("yyyy"))
			year = year.substring(year.length() - 2);
		String month = "" + (gc.get(Calendar.MONTH) + 1);
		if (month.length() < 2 && date.replace(year, "").contains("0" + month))
			month = "0" + month;
		return dateFormat.replace("d", "\\d").replace("M", month).replace(year.length() == 2 ? "yy" : "yyyy", year);
	}

	public static String periodSuffix(String period) {
		if (period == null)
			return "";
		if (period.contains("/"))
			period = period.replace("\\d/", "").replace("/", "_");
		else if (period.contains("."))
			period = period.replace("\\d.", "").replace(".", "_");
		else if (period.contains("-"))
			period = period.replace("\\d-", "").replace("-\\d", "").replace("-", "_");
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
			return "d.M." + (replaceStrangeWhitespace(date).split("\\.")[2].length() > 2 ? "yyyy" : "yy");
		if (date.contains("-")) {
			final String[] dates = date.split("-");
			if (dates.length > 1 && dates[2].length() > 2)
				return "d-M-yyyy";
			return (dates[0].length() > 2 ? "yyyy" : "yy") + "-M-d";
		}
		throw new IllegalArgumentException("Unknown date format: " + date);
	}

	public static String dateFormatWithoutYear(String format) {
		format = format.replace("yyyy", "yy");
		if (format.contains("/"))
			return format.replace("/yy", "");
		if (format.contains("."))
			return format.replace(".yy", "");
		return format.replace("yy-", "").replace("-yy", "");
	}
}