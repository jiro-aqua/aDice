package jp.sblo.pandora.dice;

final public class DiceFactory {

	private static Idice mDice = null;

	public static Idice getInstance()
	{
		if ( mDice == null ){
			mDice = new Dice();
		}
		return mDice;
	}

	private static CharSequence trimchar(CharSequence input,CharSequence patterna ,CharSequence patternb  ) {
		final int len = input.length();
		final int patlena = patterna.length();
		final int patlenb = patternb.length();
		int s, e;
		// 行頭から
		for (s = 0; s < len; s++) {
			boolean found = false;
			char c = input.charAt(s);
			for (int j = 0; j < patlena; j++) {
				if (c == patterna.charAt(j)) {
					found = true;
					break;
				}
			}
			if (!found) {
				break;
			}
		}

		// 行末から
		for (e = len - 1; e > s; e--) {
			boolean found = false;
			char c = input.charAt(e);
			for (int j = 0; j < patlenb; j++) {
				if (c == patternb.charAt(j)) {
					found = true;
					break;
				}
			}
			if (!found) {
				break;
			}
		}

		return input.subSequence(s, e + 1);
	}

	public static  String convert(CharSequence s){
		final CharSequence dakuon="、･-ガギグゲゴザジズゼゾダヂヅデドバビブベボパピプペポァィゥェォャュョッÀÁÂÃÄÅàáâãäåÆæÇçÈÉÊËèéêëÌÍÎÏìíîïÐðÑñÒÓÔÕÖØòóôõöøŒœÙÚÛÜùúûüÝÞýþÿß";
	  	final CharSequence seion =",・ カキクケコサシスセソタチツテトハヒフヘホハヒフヘホアイウエオヤユヨツaaaaaaaaaaaaaacceeeeeeeeiiiiiiiiddnnoooooooooooooouuuuuuuuyyyyys";

		s = trimchar(s , " 　" , " 　\"'?.,()[]{}|!");

		char[] cs = s.toString().toCharArray();

        for( int j=0;j<cs.length;j++ ){
	        for(int i=0; i<dakuon.length(); i++){
	        	if ( cs[j] == dakuon.charAt(i)){
	        		cs[j] = seion.charAt(i);
	        	}
	        }
        }
        return String.valueOf(cs).toLowerCase().replace("'", "").replace("  "," ");
    }

}
