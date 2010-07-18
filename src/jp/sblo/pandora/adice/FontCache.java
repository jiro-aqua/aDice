package jp.sblo.pandora.adice;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import android.graphics.Typeface;

public class FontCache
{
	static FontCache mInstance = null;
	private WeakHashMap<String , Typeface> mFontCache = new WeakHashMap<String , Typeface>();

	static public String	NORMAL = "Normal";
	static public String	PHONE = "DoulosSILR";
//	static public String	THAI = "DroidSansThai";

	public static class fontName {
		String filename;
		String fontname;
		public fontName(String f1 , String f2 ){
			filename = f1;
			fontname = f2;
		}
	}

	static FontCache getInstance() {
		if ( mInstance == null ){
			mInstance = new FontCache();
		}
		return mInstance;
	}

	public void put(String fontname, Typeface tf)
	{
		if ( fontname!=null && fontname.length()>0 && tf!=null ){
			mFontCache.put( fontname, tf );
		}
	}

	public void remove(String fontname)
	{
		if ( !fontname.equals(NORMAL) &&
			 !fontname.equals(PHONE) ){
			mFontCache.remove(fontname);
		}
	}


	public Typeface get(String fontname)
	{
		return mFontCache.get(fontname);
	}

	public ArrayList<fontName> getList()
	{
		ArrayList<fontName> ret = new ArrayList<fontName>();
		Set<Entry<String,Typeface>> sets = mFontCache.entrySet();

		for( Entry<String,Typeface> entry : sets )
		{
			String key = entry.getKey();
			if ( !key.equals(NORMAL) &&
				!key.equals(PHONE)  )
			{
				File f = new File(key);
				ret.add(new fontName(key ,f.getName() ));
			}
		}

		return ret;

	}

	public String[] getFileList()
	{
		ArrayList<String> ret = new ArrayList<String>();
		Set<Entry<String,Typeface>> sets = mFontCache.entrySet();

		for( Entry<String,Typeface> entry : sets )
		{
			String key = entry.getKey();
			if ( !key.equals(NORMAL) &&
				!key.equals(PHONE)  )
			{
				ret.add( key );
			}
		}

		return ret.toArray(new String[0]);
	}

	public String getSampleString( String key )
	{
		if (key.equals(PHONE) ){

		}

		return "Font Sample";
	}

}
