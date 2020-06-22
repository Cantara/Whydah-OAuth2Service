package net.whydah.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class URLHelper {

	public static String encode(String value) {
		try {
			if (value != null) {
				return URLEncoder.encode(value, "UTF-8");
			} else {
				return "";
			}
		} catch (UnsupportedEncodingException e) {
			
		}
		return value;
	}
}
