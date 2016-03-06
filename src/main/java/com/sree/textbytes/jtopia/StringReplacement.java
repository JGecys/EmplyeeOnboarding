package com.sree.textbytes.jtopia;

import java.util.regex.Pattern;

public class StringReplacement
{
	private Pattern pattern;
	private String replaceWith;
	
	private StringReplacement(Pattern pattern, String replaceWith) {
		this.pattern = pattern;
		this.replaceWith = replaceWith;
	}

	public static StringReplacement compile(String pattern, String replaceWith) {
		if (StringHelper.isNullOrEmpty(pattern)) throw new IllegalArgumentException("Patterns must not be null or empty!");
		Pattern p = Pattern.compile(pattern);
		return new StringReplacement(p, replaceWith);
	}

	public String replaceAll(String input) {
		if (StringHelper.isNullOrEmpty(input)) return "";
		return pattern.matcher(input).replaceAll(replaceWith);
	}
}