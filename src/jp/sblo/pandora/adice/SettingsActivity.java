package jp.sblo.pandora.adice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.sblo.pandora.dice.DiceFactory;
import jp.sblo.pandora.dice.IIndexCacheFile;
import jp.sblo.pandora.dice.IdicInfo;
import jp.sblo.pandora.dice.Idice;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity implements OnPreferenceChangeListener {

    public static final String KEY_DELAYSEARCH = "DelaySearch";
    public static final String KEY_DICS = "dics";
    public static final String KEY_ADD_DICTIONARY = "Add Dictionary";
    public static final String KEY_REMOVE_THE_DICTIONARY = "|Remove the dictionary";
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
    public static final String KEY_FONTS = "fontslist";

    public static final String KEY_NORMALIZE_SEARCH = "normalizesearch";

    public static final String KEY_LASTVERSION = "LastVersion";

    public static final String DEFAULT_FONT = ".default";

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
        new DicTemplate( "/EIJI-([0-9]+)U?.*\\.DIC",	   R.string.dicname_eijiro,	       true ),
        new DicTemplate( "/WAEI-([0-9]+)U?.*\\.DIC",       R.string.dicname_waeijiro,      false),
        new DicTemplate( "/REIJI([0-9]+)U?.*\\.DIC",       R.string.dicname_reijiro,       false),
        new DicTemplate( "/RYAKU([0-9]+)U?.*\\.DIC",       R.string.dicname_ryakujiro,     false),
        new DicTemplate( "/PDEJ2005U?.dic",                R.string.dicname_pdej,          true),
        new DicTemplate( "/PDEDICTU?.dic",                 R.string.dicname_edict,         false),
        new DicTemplate( "/PDWD1913U?.dic",                R.string.dicname_webster,       true),
        new DicTemplate( "/f2jdic.dic",                    R.string.dicname_ichirofj,      false),
    };


    private PreferenceScreen mPs = null;
    private PreferenceManager mPm = getPreferenceManager();
    private Idice mDice = null;
    private final static int REQUEST_CODE_ADDDIC = 0x1234;
    private final static int REQUEST_CODE_FONTSETTINGS = 0x1237;
    public final static String EXTRA_DLNOW = "DownloadNow";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDice = DiceFactory.getInstance();

        mPm = getPreferenceManager();

        createDictionaryPreference();

        Intent it = getIntent();
        Bundle extras = it.getExtras();
        if ( extras !=null && extras.getBoolean(EXTRA_DLNOW, false) ){
            startDownload();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        // 辞書選択画面からの応答
        if (requestCode == REQUEST_CODE_ADDDIC && resultCode == RESULT_OK && data != null) {
            final String dicname = data.getExtras().getString(FileSelectorActivity.INTENT_FILEPATH );
            final boolean english = data.getExtras().getBoolean(InstallActivity.INTENT_ENGLISH );
            final String defname = data.getExtras().getString(InstallActivity.INTENT_NAME);
            if (dicname != null) {
                // プログレスダイアログを表示
                final ProgressDialog dialog = new ProgressDialog(this);
                dialog.setIndeterminate(true);
                dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                dialog.setMessage(getResources().getString(R.string.createindexdialog));
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
                                setDefaultSettings(SettingsActivity.this, dicinfo , defname , english );
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
                                    Toast.makeText(SettingsActivity.this,
                                            getResources().getString(R.string.toastadded, dicname ) ,
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    Toast.makeText(SettingsActivity.this,
                                            getResources().getString(R.string.toasterror , dicname ) ,
                                            Toast.LENGTH_LONG).show();
                                }
                                finish();
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

                Toast.makeText(SettingsActivity.this,
                        getResources().getString(R.string.toastremoved , key ) ,
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
                    pr.setSummary(sp.getString(pr.getKey(), "100") );
                    pr.setTitle(R.string.delaytitle);
                    pr.setEntries(new String[] { "10ms","100ms", "200ms", "300ms", "500ms", "750ms", "1s", });
                    pr.setEntryValues(new String[] { "10", "100", "200", "300", "500", "750", "1000", });
                    pr.setOnPreferenceChangeListener(this);
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
                {
                    // 検索語の正規化
                    final CheckBoxPreference pr = new CheckBoxPreference(this);
                    pr.setKey(KEY_NORMALIZE_SEARCH);
                    pr.setTitle(R.string.normalize);
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
                    // タイ語フォント使用
                    final CheckBoxPreference pr = new CheckBoxPreference(this);
                    pr.setKey( KEY_THAI);
                    pr.setTitle(R.string.thaialphabet);
                    pr.setSummaryOn(R.string.thaisummaryon);
                    pr.setSummaryOff(R.string.thaisummaryoff);

                    catdic.addPreference(pr);
                }
                {
                    // デフォルトフォント設定
                    final Preference pr = new Preference(this);
                    pr.setTitle(R.string.deffontsettingtitle);
                    catdic.addPreference(pr);

                    pr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference)
                        {
                            // フォント設定選択画面呼び出し
                            Intent intent = new Intent();
                            intent.setClassName("jp.sblo.pandora.adice", "jp.sblo.pandora.adice.FontSettingsActivity");
                            intent.putExtra( FontSettingsActivity.INTENT_DICNAME ,  DEFAULT_FONT  );
                            startActivityForResult(intent, REQUEST_CODE_FONTSETTINGS );

                            return true;
                        }
                    });
                }

                {
                    // 辞書追加
                    final Preference prAdddic = new Preference(this);
                    prAdddic.setKey(KEY_ADD_DICTIONARY);
//					prAdddic.setSummary(R.string.adddictionarysummary);
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
                {
                    // 辞書ダウンロード
                    final Preference pr = new Preference(this);
                    pr.setTitle(R.string.dldictionarytitle);
                    catdic.addPreference(pr);

                    pr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference)
                        {
                            startDownload();
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
                    {
                        // フォント設定
                        final Preference pr = new Preference(this);
                        pr.setTitle(R.string.fontsettingtitle);
                        psdic.addPreference(pr);

                        pr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference)
                            {
                                // フォント設定選択画面呼び出し
                                Intent intent = new Intent();
                                intent.setClassName("jp.sblo.pandora.adice", "jp.sblo.pandora.adice.FontSettingsActivity");
                                intent.putExtra( FontSettingsActivity.INTENT_DICNAME ,  name );
                                startActivityForResult(intent, REQUEST_CODE_FONTSETTINGS );
                                return true;
                            }
                        });
                    }

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
//						pr.setSummary(R.string.removedictionarysummary);
                        pr.setTitle(R.string.removedictionarytitle);
                        pr.setOnPreferenceClickListener(new RemoveDictionary());
                        psdic.addPreference(pr);
                    }
                    // preference をカテゴリに追加
                    catdic.addPreference(psdic);

                    // TODO: 辞書削除時のy/n確認
                }
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

    private static String selectKey( SharedPreferences sp , String key , String name , String defname , String defvalue )
    {
        String k = name + key;
        String val = sp.getString(  k ,"" );
        if ( val.length() == 0 ){
            k = defname + key;
            val = sp.getString(  k , "" );
            if ( val.length()==0 ){
                val = defvalue;
            }
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

        dicinfo.SetIndexSize(    Integer.parseInt( selectKey( sp , KEY_INDEXFONTSIZE  , name ,DEFAULT_FONT  , "20" )) );
        dicinfo.SetPhoneticSize( Integer.parseInt( selectKey( sp , KEY_PHONEFONTSIZE  , name ,DEFAULT_FONT  , "16" )) );
        dicinfo.SetTransSize(    Integer.parseInt( selectKey( sp , KEY_MEANINGFONTSIZE, name ,DEFAULT_FONT  , "16" )) );
        dicinfo.SetSampleSize(   Integer.parseInt( selectKey( sp , KEY_EXAMPLEFONTSIZE, name ,DEFAULT_FONT  , "16" )) );

        dicinfo.SetIndexFont(    selectKey( sp , KEY_INDEXFONT  , name , DEFAULT_FONT  , FontCache.NORMAL )) ;
        dicinfo.SetPhoneticFont( selectKey( sp , KEY_PHONEFONT  , name , DEFAULT_FONT  , FontCache.PHONE  )) ;
        dicinfo.SetTransFont(    selectKey( sp , KEY_MEANINGFONT, name , DEFAULT_FONT  , FontCache.NORMAL )) ;
        dicinfo.SetSampleFont(   selectKey( sp , KEY_EXAMPLEFONT, name , DEFAULT_FONT  , FontCache.NORMAL )) ;

    }

    public static void setDefaultSettings(Context context, IdicInfo dicinfo,String defname , boolean english )
    {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        if ( defname==null ){
            defname = "";
        }

        //インデクス作成までOK
        final String name = dicinfo.GetFilename();

        // 名称未設定の時はデフォルトに戻す
        if ( sp.getString(name +KEY_DICNAME, "").length() == 0 ){
            final SharedPreferences.Editor editor = sp.edit();
            editor.putString( name + KEY_DICNAME , defname );
             editor.putBoolean(name + KEY_ENGLISH , english ) ;
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
                Pattern p = Pattern.compile( DICNTEMPLATE[i].pattern , Pattern.CASE_INSENSITIVE );
                Matcher m = p.matcher(name);
                if ( m.find() ){
                    String dicname;
                    if ( m.groupCount() > 0){
                        String edt = m.group(1);
                        dicname = context.getResources().getString(DICNTEMPLATE[i].resourceDicname , edt );
                    }else{
                        dicname = context.getResources().getString(DICNTEMPLATE[i].resourceDicname );
                    }
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

    public static class Settings {
        int delay;
        boolean fastScroll;
        boolean thai;
        boolean clipboard;
        boolean normalize;
    }

    public	static Settings readSettings(Context ctx)
    {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        Settings ret = new Settings();

        ret.delay = Integer.parseInt(sp.getString(KEY_DELAYSEARCH, "100"));
        ret.fastScroll = sp.getBoolean(KEY_FASTSCROLL, false);
        ret.thai = sp.getBoolean( KEY_THAI , false );
        ret.clipboard = sp.getBoolean(KEY_CLIPBOARD_SEARCH, false);
        ret.normalize = sp.getBoolean(KEY_NORMALIZE_SEARCH, true);
        return ret;
    }

    public	static String getDics(Context ctx)
    {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sp.getString( KEY_DICS , "");

    }


    public static boolean isVersionUp(Context ctx)
    {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean ret = false;
        int lastversion = sp.getInt(KEY_LASTVERSION, 0 );
        int versioncode;
        try {
            String pkgname = ctx.getApplicationInfo().packageName;
            versioncode = ctx.getPackageManager().getPackageInfo(pkgname, 0).versionCode;
            ret = (lastversion != versioncode);

            if ( ret ){
                Editor editor = sp.edit();
                editor.putInt(KEY_LASTVERSION, versioncode );
                editor.commit();

                if ( lastversion <= 30 ){
                    editor.putBoolean(KEY_NORMALIZE_SEARCH, true).commit();
                }
            }

        } catch (NameNotFoundException e) {
        }
        return ret;
    }


    private void startDownload()
    {
        // 辞書DL画面呼び出し
        Intent intent = new Intent(Intent.ACTION_VIEW );

//		intent.setData(Uri.parse("http://aquamarine.sakura.ne.jp/sblo_files/pandora/image/install.html"));

        intent.setClassName("jp.sblo.pandora.adice", "jp.sblo.pandora.adice.AboutActivity");
//		intent.putExtra(AboutActivity.EXTRA_URL, "http://aquamarine.sakura.ne.jp/sblo_files/pandora/image/install.html" );
        intent.putExtra(AboutActivity.EXTRA_URL, getString( R.string.install_url ) );
        intent.putExtra(AboutActivity.EXTRA_TITLE, getString( R.string.install_title ) );
        startActivityForResult(intent, REQUEST_CODE_ADDDIC);

    }
}

