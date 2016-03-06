package com.sree.textbytes.jtopia; /**
 * Created by IntelliJ IDEA.
 * 
 * @user 		: Sreejith.S
 *
 * 
 */

import java.util.regex.Pattern;

public class StringSplitter 
{
	private Pattern pattern;
	public StringSplitter(String pattern) {
		this.pattern = Pattern.compile(pattern);
	}

	public String[] split(String input) {
		if (StringHelper.isNullOrEmpty(input)) return new String[0];
		return pattern.split(input);
	}
}