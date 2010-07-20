package jp.sblo.pandora.adice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jp.sblo.pandora.adice.FontCache.fontName;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;


public class FontSettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener
{
	public static final String KEY_INDEXFONT  = "|indexfont" ;
	public static final String KEY_PHONEFONT= "|phonefont" ;
	public static final String KEY_EXAMPLEFONT= "|examplefont" ;
	public static final String KEY_MEANINGFONT= "|trasnfont" ;
	public static final String KEY_EXAMPLEFONTSIZE = "|Examplefontsize";
	public static final String KEY_PHONEFONTSIZE = "|Phonefontsize";
	public static final String KEY_MEANINGFONTSIZE = "|Meaningfontsize";
	public static final String KEY_INDEXFONTSIZE = "|indexfontsize";
	public static final String KEY_FONTS = "fontslist";

	private PreferenceScreen mPs = null;
	private PreferenceManager mPm = getPreferenceManager();
	private String mName;

	final public static String INTENT_DICNAME = "dicname";
	private final static int REQUEST_CODE_FONTMANAGER= 0x1235;
	private ArrayList<FontCache.fontName> mFontList = null;


	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPm = getPreferenceManager();

		Intent it = getIntent();
		Bundle extra = it.getExtras();
		mName = extra.getString(INTENT_DICNAME);

	}


	@Override
	protected void onResume()
	{
		super.onResume();
		if ( mName!=null && mName.length() != 0 ){
			createFontPreference(mName);
		}
	}

	private void loadFontList()
	{
		mFontList = FontCache.getInstance().getAllList();

		Collections.sort( mFontList , new Comparator<FontCache.fontName>(){

			@Override
			public int compare(fontName object1, fontName object2)
			{
				return object1.fontname.compareToIgnoreCase(object2.fontname);
			}
		});

		mFontList.add(0, new FontCache.fontName( "" , "なし" ));
	}

	private String[] getFontNames()
	{
		ArrayList<String> ret = new ArrayList<String>();
		for( FontCache.fontName font : mFontList ){
			ret.add( font.fontname );
		}
		return ret.toArray(new String[0]);
	}
	private String[] getFileNames()
	{
		ArrayList<String> ret = new ArrayList<String>();
		for( FontCache.fontName font : mFontList ){
			ret.add( font.filename );
		}
		return ret.toArray(new String[0]);
	}


	private void createFontPreference(final String name) {
		// 新規PreferenceScreen
		mPs = mPm.createPreferenceScreen(this);

		loadFontList();
		final String[] fontnames  = getFontNames();
		final String[] filenames  = getFileNames();

		{
			final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

			// フォントカテゴリ
			final PreferenceCategory catfont = new PreferenceCategory(this);
			catfont.setTitle(R.string.fonttitle);
			mPs.addPreference(catfont);
			if ( Build.VERSION.SDK.compareTo("4") >= 0 )			{
				// インデックスフォント設定
				final ListPreference pr = new ListPreference(this);
				pr.setKey(name +KEY_INDEXFONT);
				pr.setTitle(R.string.fontindex);
				pr.setEntries(fontnames);
				pr.setEntryValues(filenames);
				pr.setOnPreferenceChangeListener(this);
				pr.setSummary(sp.getString(pr.getKey(), ""));
				catfont.addPreference(pr);
			}
			{
				// インデックスフォントサイズ
				final ListPreference pr = new ListPreference(this);
				pr.setKey(name + KEY_INDEXFONTSIZE);
				pr.setSummary(sp.getString(pr.getKey(), ""));
				pr.setTitle(R.string.fontsizeindex);
				pr.setEntries(new String[] { "10", "14", "16", "18", "20", "24", "30", "36",  });
				pr.setEntryValues(new String[] { "10", "14", "16", "18", "20", "24", "30", "36",  });
				pr.setOnPreferenceChangeListener(this);
				catfont.addPreference(pr);
			}
			if ( Build.VERSION.SDK.compareTo("4") >= 0 ){
				// 本文フォント設定
				final ListPreference pr = new ListPreference(this);
				pr.setKey(name + KEY_MEANINGFONT);
				pr.setSummary(sp.getString(pr.getKey(), ""));
				pr.setTitle(R.string.fonttrans);
				pr.setEntries(fontnames);
				pr.setEntryValues(filenames);
				pr.setOnPreferenceChangeListener(this);
				catfont.addPreference(pr);
			}
			{
				// 本文フォントサイズ
				final ListPreference pr = new ListPreference(this);
				pr.setKey(name + KEY_MEANINGFONTSIZE );
				pr.setSummary(sp.getString(pr.getKey(), ""));
				pr.setTitle(R.string.fontsizetrans);
				pr.setEntries(new String[] { "10", "14", "16", "18", "20", "24", "30", "36",  });
				pr.setEntryValues(new String[] { "10", "14", "16", "18", "20", "24", "30", "36",  });
				pr.setOnPreferenceChangeListener(this);
				catfont.addPreference(pr);
			}
			if ( Build.VERSION.SDK.compareTo("4") >= 0 ){
				// 発音記号フォント設定
				final ListPreference pr = new ListPreference(this);
				pr.setKey(name + KEY_PHONEFONT);
				pr.setSummary(sp.getString(pr.getKey(), ""));
				pr.setTitle(R.string.fontphone);
				pr.setEntries(fontnames);
				pr.setEntryValues(filenames);
				pr.setOnPreferenceChangeListener(this);
				catfont.addPreference(pr);
			}
			{
				// 発音記号フォントサイズ
				final ListPreference pr = new ListPreference(this);
				pr.setKey(name + KEY_PHONEFONTSIZE);
				pr.setSummary(sp.getString(pr.getKey(), ""));
				pr.setTitle(R.string.fontsizephone);
				pr.setEntries(new String[] { "10", "14", "16", "18", "20", "24", "30", "36",  });
				pr.setEntryValues(new String[] { "10", "14", "16", "18", "20", "24", "30", "36",  });
				pr.setOnPreferenceChangeListener(this);
				catfont.addPreference(pr);
			}
			if ( Build.VERSION.SDK.compareTo("4") >= 0 ){
				// 用例フォント設定
				final ListPreference pr = new ListPreference(this);
				pr.setKey(name + KEY_EXAMPLEFONT);
				pr.setSummary(sp.getString(pr.getKey(), ""));
				pr.setTitle(R.string.fontsample);
				pr.setEntries(fontnames);
				pr.setEntryValues(filenames);
				pr.setOnPreferenceChangeListener(this);
				catfont.addPreference(pr);
			}
			{
				// 用例フォントサイズ
				final ListPreference pr = new ListPreference(this);
				pr.setKey(name + KEY_EXAMPLEFONTSIZE );
				pr.setSummary(sp.getString(pr.getKey(), ""));
				pr.setTitle(R.string.fontsizesample);
				pr.setEntries(new String[] { "10", "14", "16", "18", "20", "24", "30", "36",  });
				pr.setEntryValues(new String[] { "10", "14", "16", "18", "20", "24", "30", "36",  });
				pr.setOnPreferenceChangeListener(this);
				catfont.addPreference(pr);
			}
			if ( Build.VERSION.SDK.compareTo("4") >= 0 ){	//  Donut 以降の機能
				// フォント管理
				final Preference pr = new Preference(this);
				pr.setTitle(R.string.fontmanager);
				pr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference)
					{
						// フォント管理画面呼び出し
						Intent intent = new Intent();
						intent.setClassName("jp.sblo.pandora.adice", "jp.sblo.pandora.adice.FontManagerActivity");
						startActivityForResult(intent, REQUEST_CODE_FONTMANAGER);

						return true;
					}
				});
				catfont.addPreference(pr);
			}
		}
		setPreferenceScreen(mPs);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		if (newValue != null) {
			preference.setSummary((CharSequence) newValue);
			return true;
		}
		return false;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == REQUEST_CODE_FONTMANAGER && resultCode == RESULT_OK ) {
			// preferenceに登録

			String[] fonts = FontCache.getInstance().getFileList();
			StringBuilder  fontlist = new StringBuilder();
			for( String font : fonts ){
				fontlist.append(font);
				fontlist.append('|');
			}
			writeFontList(this, fontlist.toString());
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public	static void writeFontList(Context ctx , String fontlist)
	{
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString( KEY_FONTS, fontlist );
		editor.commit();
	}
	public	static String readFontList(Context ctx)
	{
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
		return sp.getString( KEY_FONTS , "" ) ;
	}


}
