package cl.beauty.util.regex;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilRegex {
	public static List<String> getAllMatches(String text,String rgx){
		List<String> allMatches = new ArrayList<String>();
		Matcher m = Pattern.compile(rgx).matcher(text);
		while (m.find()) {
		   allMatches.add(m.group());
		}
		return allMatches;
	}
}



