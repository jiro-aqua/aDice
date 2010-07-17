package jp.sblo.pandora.adice;

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

	public Typeface get(String fontname)
	{
		return mFontCache.get(fontname);
	}

	public ArrayList<String> getList()
	{
		ArrayList<String> ret = new ArrayList<String>();
		Set<Entry<String,Typeface>> sets = mFontCache.entrySet();

		for( Entry<String,Typeface> entry : sets )
		{
			String key = entry.getKey();
			if ( !key.equals(NORMAL)  /*&&
				!key.equals(PHONE) &&
				!key.equals(THAI )*/ )
			{
				ret.add(key);
			}
		}

		Collections.sort( ret , new Comparator<String>(){
			@Override
			public int compare(String object1, String object2)
			{
				return object2.compareToIgnoreCase(object1);
			}
		});

		return ret;

	}

	public String getSampleString( String key )
	{
		if (key.equals(PHONE) ){

		}

		return "Font Sample";
	}

}
