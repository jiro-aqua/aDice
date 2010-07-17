package jp.sblo.pandora.adice;

import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
//import android.util.Log;
import android.widget.Toast;

import jp.sblo.pandora.dice.DiceFactory;
import jp.sblo.pandora.dice.IIndexCacheFile;
import jp.sblo.pandora.dice.IdicInfo;
import jp.sblo.pandora.dice.Idice;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener {

	public static final String KEY_DELAYSEARCH = "DelaySearch";
	public static final String KEY_DICS = "dics";
	public static final String KEY_ADD_DICTIONARY = "Add Dictionary";
	public static final String KEY_REMOVE_THE_DICTIONARY = "|Remove the dictionary";
	public static final String KEY_EXAMPLEFONTSIZE_DEF = "Examplefontsize";
	public static final String KEY_PHONEFONTSIZE_DEF = "Phonefontsize";
	public static final String KEY_MEANINGFONTSIZE_DEF = "Meaningfontsize";
	public static final String KEY_INDEXFONTSIZE_DEF = "indexfontsize";
	public static final String KEY_USE = "|use";
	public static final String KEY_ENGLISH = "|english";
	public static final String KEY_RESULTNUM = "|resultnum";
	public static final String KEY_DICNAME = "|dicname";
	public static final String KEY_MOVE_UP = "|MoveUp";
	public static final String KEY_MOVE_DOWN = "|MoveDown";
	public static final String KEY_THAI = "thai";
	public static final String KEY_FASTSCROLL = "fastscroll";
	public static final String KEY_CLIPBOARD_SEARCH = "clipboardsearch";
	public static final String KEY_INDEXFONT  = "|indexfont" ;
	public static final String KEY_PHONEFONT= "|phonefont" ;
	public static final String KEY_EXAMPLEFONT= "|examplefont" ;
	public static final String KEY_MEANINGFONT= "|trasnfont" ;
	public static final String KEY_EXAMPLEFONTSIZE = "|Examplefontsize";
	public static final String KEY_PHONEFONTSIZE = "|Phonefontsize";
	public static final String KEY_MEANINGFONTSIZE = "|Meaningfontsize";
	public static final String KEY_INDEXFONTSIZE = "|indexfontsize";


	static class DicTemplate {
		public String pattern;
		public int resourceDicname;
		public boolean englishFlag;

		public DicTemplate( String a, int b , boolean c )
		{
			pattern = a;
			resourceDicname =b;
			englishFlag =c;
		}
	}
	private final static DicTemplate DICNTEMPLATE[]={
		new DicTemplate( "/EIJI-([0-9]+).*\\.DIC",	     R.string.dicname_eijiro,	     true ),
		new DicTemplate( "/WAEI-([0-9]+).*\\.DIC",       R.string.dicname_waeijiro,      false),
		new DicTemplate( "/REIJI([0-9]+).*\\.DIC",       R.string.dicname_reijiro,       false),
		new DicTemplate( "/RYAKU([0-9]+).*\\.DIC",       R.string.dicname_ryakujiro,     false),
	};


	private PreferenceScreen mPs = null;
	private PreferenceManager mPm = getPreferenceManager();
	private Idice mDice = null;
	private final static int REQUEST_CODE_ADDDIC = 0x1234;
	private final static int REQUEST_CODE_FONTMANAGER= 0x1235;
	private final Context mContext = this;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mDice = DiceFactory.getInstance();

		mPm = getPreferenceManager();

		createDictionaryPreference();


	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		super.onActivityResult(requestCode, resultCode, data);

		// 辞書選択画面からの応答
		if (requestCode == REQUEST_CODE_ADDDIC && resultCode == RESULT_OK && data != null) {
			final String dicname = data.getExtras().getString(FileSelectorActivity.INTENT_FILEPATH);
			if (dicname != null) {
		        // プログレスダイアログを表示
				final ProgressDialog dialog = new ProgressDialog(this);
		        dialog.setIndeterminate(true);
		        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		        dialog.setMessage(mContext.getResources().getString(R.string.createindexdialog));
		        dialog.show();

		        new Thread(){
		        	@Override
		        	public void run(){
						// 辞書追加
						boolean failed=true;
						final IdicInfo dicinfo = mDice.open(dicname);
						if (dicinfo != null) {
							// 登録成功ならば
							// インデクスキャッシュファイル名取得
							final String filename = dicname.replace("/", ".") + ".idx";

							// インデクス作成
							if (! dicinfo.readIndexBlock(new IIndexCacheFile() {
								final  String path =getCacheDir() + "/"  + filename;
								@Override
								public FileInputStream getInput() throws FileNotFoundException
								{
									return new FileInputStream( path );
								}
								@Override
								public FileOutputStream getOutput() throws FileNotFoundException
								{
									return new FileOutputStream( path );
								}
							})) {
								mDice.close( dicinfo );
							}else{
								failed = false;
								setDefaultSettings(mContext, dicinfo );




							}
						}

						final	boolean result = !failed;
						runOnUiThread(new Runnable(){
							@Override
							public void run(){
						        dialog.dismiss();
								if ( result ){
									writeDictionary();

									// 設定画面更新
									createDictionaryPreference();

									;
									Toast.makeText(mContext,
											mContext.getResources().getString(R.string.toastadded, dicname ) ,
											Toast.LENGTH_LONG).show();
								} else {
									Toast.makeText(mContext,
											mContext.getResources().getString(R.string.toasterror , dicname ) ,
											Toast.LENGTH_LONG).show();
								}
							}
						});
		        	}
		        }.start();
				return;
			}
		}
	}

	private void writeDictionary() {
		// 辞書一覧を作って
		final int len = mDice.getDicNum();
		String dics = "";
		for (int i = 0; i < len; i++) {
			dics += mDice.getDicInfo(i).GetFilename();
			dics += "|";
		}

		// preferenceに登録
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

		SharedPreferences.Editor editor = sp.edit();
		editor.putString(KEY_DICS, dics);
//		Log.e("wriredictionary",KEY_DICS + ","+ dics);
		editor.commit();
	}

	private final class RemoveDictionary implements Preference.OnPreferenceClickListener {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			// 辞書削除

			// 辞書名取得
			final String prefkey = preference.getKey();
			final int token = prefkey.indexOf('|');
			final String key = prefkey.substring(0,token);

			// 該当する辞書を閉じる
			final IdicInfo dicinfo = mDice.getDicInfo(key);
			if (dicinfo != null) {
				// 辞書クローズ
				mDice.close(dicinfo);

				// 親のPreferenceScreenを取得
				PreferenceScreen ps = (PreferenceScreen) mPm.findPreference(key);
				// 今のDialogを閉じて，前の画面に移動
				Dialog dlg = ps.getDialog();
				dlg.dismiss();

				// 一覧を更新
				writeDictionary();

				// 設定画面の一覧を更新
				createDictionaryPreference();

				// インデクスキャッシュファイル名取得
				final String filename = getCacheDir() + "/" + key.replace("/", ".") + ".idx";
				// インデクスファイル削除
				new File( filename ).delete();

				Toast.makeText(mContext,
						mContext.getResources().getString(R.string.toastremoved , key ) ,
						Toast.LENGTH_LONG).show();

			}

			return true;
		}
	}


	private void createDictionaryPreference() {
		// 新規PreferenceScreen
		mPs = mPm.createPreferenceScreen(this);

		{
			final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

			{
				// 辞書管理カテゴリ
				final PreferenceCategory catdic = new PreferenceCategory(this);
				catdic.setTitle(R.string.searchsettingtitle);

				mPs.addPreference(catdic);
				{
					// 検索ディレイ
					final ListPreference pr = new ListPreference(this);
					pr.setKey(KEY_DELAYSEARCH);
					pr.setSummary(R.string.delaysummary );
					pr.setTitle(R.string.delaytitle);
					pr.setEntries(new String[] { "10ms","100ms", "200ms", "300ms", "500ms", "750ms", "1s", });
					pr.setEntryValues(new String[] { "10", "100", "200", "300", "500", "750", "1000", });
					catdic.addPreference(pr);
				}
				{
					// 高速スクロール
					final CheckBoxPreference pr = new CheckBoxPreference(this);
					pr.setKey(KEY_FASTSCROLL);
					pr.setTitle(R.string.fastscroll);
					pr.setSummaryOn(R.string.fastscrollsummaryon);
					pr.setSummaryOff(R.string.fastscrollsummaryoff);
					catdic.addPreference(pr);
				}
				{
					// クリップボード検索
					final CheckBoxPreference pr = new CheckBoxPreference(this);
					pr.setKey(KEY_CLIPBOARD_SEARCH);
					pr.setTitle(R.string.clipboardsearch);
					pr.setSummaryOn(R.string.clipboardsearchsummaryon);
					pr.setSummaryOff(R.string.clipboardsearchsummaryoff);
					catdic.addPreference(pr);
				}
//				{
//					// 検索ディレイ
//					final ListPreference pr = new ListPreference(this);
//					pr.setKey(KEY_CONJUGATIONDELAY);
//					pr.setSummary(R.string.conjudelaysummary );
//					pr.setTitle(R.string.conjudelaytitle);
//					pr.setEntries(new String[] { "100ms", "200ms", "300ms", "500ms", "750ms", "1s", });
//					pr.setEntryValues(new String[] { "100", "200", "300", "500", "750", "1000", });
//					catdic.addPreference(pr);
//				}
				{
					// 辞書追加
					final Preference prAdddic = new Preference(this);
					prAdddic.setKey(KEY_ADD_DICTIONARY);
					prAdddic.setSummary(R.string.adddictionarysummary);
					prAdddic.setTitle(R.string.adddictionarytitle);
					catdic.addPreference(prAdddic);

					prAdddic.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
						@Override
						public boolean onPreferenceClick(Preference preference)
						{
							// ファイル選択画面呼び出し
							Intent intent = new Intent();
							intent.setClassName("jp.sblo.pandora.adice", "jp.sblo.pandora.adice.FileSelectorActivity");
							intent.putExtra(FileSelectorActivity.INTENT_EXTENSION, new String[] { ".dic" });
							startActivityForResult(intent, REQUEST_CODE_ADDDIC);

							return true;
						}
					});
				}
			}

			// 辞書管理カテゴリ
			final PreferenceCategory catdic = new PreferenceCategory(this);
			catdic.setTitle(R.string.dictionarymanagementtitle);
			mPs.addPreference(catdic);

			// 辞書一覧取得
			final ArrayList<IdicInfo>	diclist = new ArrayList<IdicInfo>();
			for ( int i=0; i<mDice.getDicNum();i++) {
				IdicInfo dicinfo = mDice.getDicInfo(i);
				diclist.add( dicinfo );
			}


			for(int i=0;i < diclist.size() ; i++ ){
				final IdicInfo dicinfo = diclist.get(i);
				final String name = dicinfo.GetFilename();

				if (name.length() > 0) {
					// 辞書
					final PreferenceScreen psdic = mPm.createPreferenceScreen(this);
					psdic.setKey(name);

					final String dicname = sp.getString(name + KEY_DICNAME, "");
					psdic.setTitle(dicname.length() > 0 ? dicname : name);

					// 辞書ファイル名
					{
						final Preference pr = new Preference(this);
						pr.setKey(name + "|filename");
						pr.setSummary(name);
						pr.setTitle(R.string.dictionarypathtitle);
						psdic.addPreference(pr);
					}
					{
						// 辞書名称
						final EditTextPreference pr = new EditTextPreference(this);
						pr.setKey(name + KEY_DICNAME);
						pr.setSummary(sp.getString(pr.getKey(), ""));
						pr.setTitle(R.string.dictionarynametitle);
						pr.setOnPreferenceChangeListener(this);
						pr.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
							@Override
							public boolean onPreferenceChange(Preference preference, Object newValue) {
								CharSequence str = "";
								if (newValue != null) {
									str = (CharSequence) newValue;
								}
								psdic.setTitle(str.length() > 0 ? str : name);
								preference.setSummary((CharSequence) newValue);
								return true;
							}
						});
						psdic.addPreference(pr);
					}
					{
						// 検索件数
						final ListPreference pr = new ListPreference(this);
						pr.setKey(name + KEY_RESULTNUM);
						pr.setSummary(sp.getString(pr.getKey(), ""));
						pr.setTitle(R.string.numberofresulttitle);
						pr.setEntries(new String[] { "5", "10", "20", "30" });
						pr.setEntryValues(new String[] { "5", "10", "20", "30" });
						pr.setOnPreferenceChangeListener(this);
						psdic.addPreference(pr);
					}

//					{
//						// 活用検索
//						final CheckBoxPreference pr = new CheckBoxPreference(this);
//						pr.setKey(name + KEY_CONJUGATIONSEARCH);
//						pr.setTitle(R.string.conjusearchtitle);
//						pr.setSummaryOn(R.string.conjusearchsummaryon);
//						pr.setSummaryOff(R.string.conjusearchsummaryoff);
//
//						psdic.addPreference(pr);
//					}

					{
						// 英語辞書
						final CheckBoxPreference pr = new CheckBoxPreference(this);
						pr.setKey(name + KEY_ENGLISH);
						pr.setTitle(R.string.englishtitle);
						pr.setSummaryOn(R.string.englishsummaryon);
						pr.setSummaryOff(R.string.englishsummaryoff);
//						pr.setDependency(name + KEY_CONJUGATIONSEARCH);

						psdic.addPreference(pr);
					}
//					{
//						// アクセント記号無視
//						final CheckBoxPreference pr = new CheckBoxPreference(this);
//						pr.setKey(name + KEY_ACCENT);
//						pr.setTitle("Ignore Accent symbol");
//						pr.setSummaryOn("ÀÁÂÃÄÅ will be match with A");
//						pr.setSummaryOff("ÀÁÂÃÄÅ won't be match with A");
////						pr.setDependency(name + KEY_CONJUGATIONSEARCH);
//
//						psdic.addPreference(pr);
//					}
//					{
//						// 不規則変化辞書
//						final CheckBoxPreference pr = new CheckBoxPreference(this);
//						pr.setKey(name + KEY_IRREG);
//						pr.setTitle("IRREG");
//						pr.setSummaryOn("This dictionary is IRREG");
//						pr.setSummaryOff("This dictionary is not IRREG.");
//
//						psdic.addPreference(pr);
//					}
					{
						// 使用可否
						final CheckBoxPreference pr = new CheckBoxPreference(this);
						pr.setKey(name + KEY_USE);
						pr.setTitle(R.string.enabledictionarytitle);
						pr.setSummaryOn(R.string.enabledictionarysummaryon);
						pr.setSummaryOff(R.string.enabledictionarysummaryoff);
						psdic.addPreference(pr);
					}

					// // チェックボックス
					// final CheckBoxPreference cbp = new
					// CheckBoxPreference(this);
					// cbp.setKey("Incremental Search");
					// cbp.setSummaryOn("inremental search on");
					// cbp.setSummaryOff("inremental search off");
					// cbp.setTitle("Incremental Search");
					// psdic.addPreference(cbp);

					if ( i>0 ){
						// 上ボタン
						final Preference pr = new Preference(this);
						pr.setKey(name + KEY_MOVE_UP);
						pr.setTitle(R.string.moveuptitle);
						pr.setOnPreferenceClickListener(new SwapDic());
						psdic.addPreference(pr);
					}
					if ( i<diclist.size() -1 ){
						// 下ボタン
						final Preference pr = new Preference(this);
						pr.setKey(name + KEY_MOVE_DOWN);
						pr.setTitle(R.string.movedowntitle);
						pr.setOnPreferenceClickListener(new SwapDic());
						psdic.addPreference(pr);
					}
					{
						// 削除ボタン
						final Preference pr = new Preference(this);
						pr.setKey(name + KEY_REMOVE_THE_DICTIONARY);
						pr.setSummary(R.string.removedictionarysummary);
						pr.setTitle(R.string.removedictionarytitle);
						pr.setOnPreferenceClickListener(new RemoveDictionary());
						psdic.addPreference(pr);
					}
					// preference をカテゴリに追加
					catdic.addPreference(psdic);

					// TODO: 起動時の砂時計・辞書削除時のy/n確認・項目変更時の即時反映
				}
			}
			// フォントサイズカテゴリ
			final PreferenceCategory catfont = new PreferenceCategory(this);
			catfont.setTitle(R.string.fontsizettitle);
			mPs.addPreference(catfont);
			{
				// インデックスフォントサイズ
				final ListPreference pr = new ListPreference(this);
				pr.setKey(KEY_INDEXFONTSIZE_DEF);
				pr.setSummary(pr.getValue());
				pr.setTitle(R.string.fontsizeindex);
				pr.setEntries(new String[] { "10", "14", "16", "18", "20", "24", "30", "36",  });
				pr.setEntryValues(new String[] { "10", "14", "16", "18", "20", "24", "30", "36",  });
				pr.setOnPreferenceChangeListener(this);
				catfont.addPreference(pr);
			}
			{
				// 本文フォントサイズ
				final ListPreference pr = new ListPreference(this);
				pr.setKey(KEY_MEANINGFONTSIZE_DEF);
				pr.setSummary(pr.getValue());
				pr.setTitle(R.string.fontsizetrans);
				pr.setEntries(new String[] { "10", "14", "16", "18", "20", "24", "30", "36",  });
				pr.setEntryValues(new String[] { "10", "14", "16", "18", "20", "24", "30", "36",  });
				pr.setOnPreferenceChangeListener(this);
				catfont.addPreference(pr);
			}
			{
				// 発音記号フォントサイズ
				final ListPreference pr = new ListPreference(this);
				pr.setKey(KEY_PHONEFONTSIZE_DEF);
				pr.setSummary(pr.getValue());
				pr.setTitle(R.string.fontsizephone);
				pr.setEntries(new String[] { "10", "14", "16", "18", "20", "24", "30", "36",  });
				pr.setEntryValues(new String[] { "10", "14", "16", "18", "20", "24", "30", "36",  });
				pr.setOnPreferenceChangeListener(this);
				catfont.addPreference(pr);
			}
			{
				// サンプルフォントサイズ
				final ListPreference pr = new ListPreference(this);
				pr.setKey(KEY_EXAMPLEFONTSIZE_DEF);
				pr.setSummary(pr.getValue());
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
			{
				// タイ語辞書
				final CheckBoxPreference pr = new CheckBoxPreference(this);
				pr.setKey( KEY_THAI);
				pr.setTitle(R.string.thaialphabet);
				pr.setSummaryOn(R.string.thaisummaryon);
				pr.setSummaryOff(R.string.thaisummaryoff);

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

	private static String selectKey( SharedPreferences sp , String key , String defkey , String defvalue )
	{
		String val = sp.getString(  key ,"" );
		if ( val.length() == 0 ){
			val = sp.getString(  defkey , defvalue );
		}
		return val;
	}


	public static void apllySettings(Context context, IdicInfo dicinfo)
	{
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

		//インデクス作成までOK
		String name = dicinfo.GetFilename();

		dicinfo.SetDicName( sp.getString( name + KEY_DICNAME ,"" ) );
		dicinfo.SetEnglish( sp.getBoolean(name + KEY_ENGLISH ,false ) );
		dicinfo.SetNotuse( !sp.getBoolean(name + KEY_USE,false ) );
		dicinfo.SetSearchMax( Integer.parseInt( sp.getString( name + KEY_RESULTNUM ,"5" ) ) );

		dicinfo.SetIndexSize( Integer.parseInt( selectKey( sp , name + KEY_INDEXFONTSIZE, KEY_INDEXFONTSIZE_DEF , "20" ) ) );
		dicinfo.SetPhoneticSize( Integer.parseInt( selectKey( sp , name + KEY_PHONEFONTSIZE, KEY_PHONEFONTSIZE_DEF , "16" ) ) );
		dicinfo.SetSampleSize( Integer.parseInt( selectKey( sp , name + KEY_EXAMPLEFONTSIZE, KEY_EXAMPLEFONTSIZE_DEF , "16" )) ) ;
		dicinfo.SetTransSize( Integer.parseInt( selectKey( sp , name + KEY_MEANINGFONTSIZE, KEY_MEANINGFONTSIZE_DEF , "16" )) ) ;

		dicinfo.SetIndexFont( sp.getString( name + KEY_INDEXFONT, FontCache.NORMAL )  );
		dicinfo.SetPhoneticFont( sp.getString( name + KEY_PHONEFONT, FontCache.PHONE )  );
		dicinfo.SetTransFont( sp.getString( name + KEY_MEANINGFONT, FontCache.NORMAL )  );
		dicinfo.SetSampleFont( sp.getString( name + KEY_EXAMPLEFONT, FontCache.NORMAL )  );
	}
	public static void setDefaultSettings(Context context, IdicInfo dicinfo)
	{
		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

		//インデクス作成までOK
		final String name = dicinfo.GetFilename();

		// 名称未設定の時はデフォルトに戻す
		if ( sp.getString(name +KEY_DICNAME, "").length() == 0 ){
			final SharedPreferences.Editor editor = sp.edit();
			editor.putString( name + KEY_DICNAME , "" );
 			editor.putBoolean(name + KEY_ENGLISH , false ) ;
			editor.putBoolean(name + KEY_USE , true );
			editor.putString( name + KEY_RESULTNUM ,"5"  ) ;

			editor.putString(  name + KEY_INDEXFONTSIZE , "" ) ;
			editor.putString(  name + KEY_PHONEFONTSIZE , "" ) ;
			editor.putString(  name + KEY_EXAMPLEFONTSIZE , "" ) ;
			editor.putString(  name + KEY_MEANINGFONTSIZE , "" ) ;
			editor.putString(  name + KEY_INDEXFONT , "" ) ;
			editor.putString(  name + KEY_PHONEFONT , "" ) ;
			editor.putString(  name + KEY_EXAMPLEFONT , "" ) ;
			editor.putString(  name + KEY_MEANINGFONT , "" ) ;

			// 辞書名自動判定
			for( int i=0;i<DICNTEMPLATE.length ;i++ ){
				Pattern p = Pattern.compile( DICNTEMPLATE[i].pattern );
				Matcher m = p.matcher(name);
				if ( m.find() ){
					String edt = m.group(1);
					String dicname = context.getResources().getString(DICNTEMPLATE[i].resourceDicname , edt );
					editor.putString( name +KEY_DICNAME , dicname	);
		 			editor.putBoolean(name + KEY_ENGLISH , DICNTEMPLATE[i].englishFlag ) ;
				}
			}

			editor.commit();

		}

	}

	private final class SwapDic implements Preference.OnPreferenceClickListener {
		@Override
		public boolean onPreferenceClick(Preference preference) {
			// 辞書の位置を変更

			// 辞書名取得
			// 辞書名取得
			final String prefkey = preference.getKey();
			final int token = prefkey.indexOf('|');
			final String key0 = prefkey.substring(0,token);
			final String key1 = prefkey.substring(token+1);

			// 該当する辞書を入れ替える
			final IdicInfo dicinfo = mDice.getDicInfo(key0);

			if ( KEY_MOVE_UP.endsWith(key1)){
				// 辞書を上に
				mDice.swap( dicinfo , -1 );
			}else{
				// 辞書を下に
				mDice.swap( dicinfo , 1 );
			}

			// 親のPreferenceScreenを取得
			PreferenceScreen ps = (PreferenceScreen) mPm.findPreference(key0);
			// 今のDialogを閉じて，前の画面に移動
			Dialog dlg = ps.getDialog();
			dlg.dismiss();

			// 一覧を更新
			writeDictionary();

			// 設定画面の一覧を更新
			createDictionaryPreference();

			return true;
		}
	}


}
