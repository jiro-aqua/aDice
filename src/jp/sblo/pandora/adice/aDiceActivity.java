package jp.sblo.pandora.adice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jp.sblo.pandora.dice.DiceFactory;
import jp.sblo.pandora.dice.IIndexCacheFile;
import jp.sblo.pandora.dice.IdicInfo;
import jp.sblo.pandora.dice.IdicResult;
import jp.sblo.pandora.dice.Idice;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;

public class aDiceActivity extends Activity implements DicView.Callback
{

	private final static String TAG = "aDice";
	private static String mLast = "";
	private static Thread mDiceThread = null;

	private final Idice mDice = DiceFactory.getInstance();;
	private final Context mContext = this;
	private final HashMap<String, String> mIrreg = new HashMap<String, String>();

	private int mDelay = 0;
	private DicView mDicView;
	private DicEditText mEdittext;

	private static String mMorebutton;
	private static String mNoResult;
	private static String mStartPage = null;
	private static String mFooter;
	private DicView.ResultAdapter mAdapter;
	private ArrayList<DicView.Data> mResultData;
	private boolean mFastScroll;

	private static final int DISP_MODE_RESULT = 0;
	private static final int DISP_MODE_MORE = 1;
	private static final int DISP_MODE_FOOTER = 2;
	private static final int DISP_MODE_NORESULT = 3;
	private static final int DISP_MODE_START = 4;

	private ArrayList<CharSequence> mSearchHistory = new ArrayList<CharSequence>();
	private ClipboardManager mClipboardManager;
	private CharSequence mLastClipboard = null;
	private static int LONG_PRESS_DELAY = 500;// msec
	private Handler mHandler = new Handler();
	private boolean registLongPress = false;
	private final Runnable mLongPressAction = new Runnable() {
		@Override
		public void run()
		{
			finish();
		}
	};
	private final FontCache mFontCache = FontCache.getInstance();
	private Typeface mNormalFont ;
	private Typeface mThaiFont ;
	private boolean mInitialized = false;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// fontロード
		mNormalFont = Typeface.defaultFromStyle(Typeface.NORMAL);
		mThaiFont = Typeface.createFromAsset( getAssets(), "DroidSansThai.ttf");
		mFontCache.put(FontCache.NORMAL , mNormalFont  );
		mFontCache.put(FontCache.PHONE , Typeface.createFromAsset( getAssets(), "DoulosSILR.ttf") );

		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
		String[] fontlist = sp.getString( SettingsActivity.KEY_FONTS , "").split("\\|");

		for( String font : fontlist ){
			if ( font.length() == 0 )
				continue;
			File f = new File( font );
			Typeface tf = FontManagerActivity.createTypefaceFromFile( f );
			mFontCache.put(font, tf);
		}

		// プログレスダイアログを表示
		final ProgressDialog dialog = new ProgressDialog(this);
		dialog.setIndeterminate(true);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setMessage(mContext.getResources().getString(R.string.loadingdictionarydialog));
		dialog.show();
		// Log.e( TAG , "Dialog Show");

		setContentView(R.layout.main);

		mMorebutton = getResources().getString(R.string.morebtn);
		mNoResult = getResources().getString(R.string.noresulthtml);
		mFooter = getResources().getString(R.string.resulttitlehtml);
		if (mStartPage == null) {
			StringBuilder s = new StringBuilder();
			BufferedReader in;
			String str;
			try {
				in = new BufferedReader(new InputStreamReader(getAssets().open("start.html")));
				while ((str = in.readLine()) != null) {
					s.append(str);
				}
				in.close();
				mStartPage = s.toString();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		mDicView = (DicView) findViewById(R.id.DicView01);
		mDicView.setCallback(this);
		mResultData = new ArrayList<DicView.Data>();
		mAdapter = new DicView.ResultAdapter(mContext, R.layout.list, R.id.DicView01, mResultData);
		mDicView.setAdapter(mAdapter);

		mEdittext = (DicEditText) findViewById(R.id.EditText01);
		mEdittext.setHint(R.string.hinttext);
		mEdittext.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable editable)
			{

			}

			@Override
			public void beforeTextChanged(CharSequence charsequence, int i, int j, int k)
			{

			}

			@Override
			public void onTextChanged(CharSequence charsequence, int i, int j, int k)
			{

				if (mInitialized){
					String text = DiceFactory.convert(charsequence);

					if (text.length() > 0 && !mLast.equals(text)) {
						int timer = mDelay;
						mLast = text;
						if (mLast.charAt(mLast.length() - 1) != text.charAt(text.length() - 1)) {
							timer = 10;
							// トリムした後の文字列と、末尾の文字が違っていたら即検索かける
						}

						search(text, timer);
					}
				}
			}
		});
		mEdittext.setOnKeyListener(new View.OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event)
			{
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					String text = mEdittext.getEditableText().toString();
					searchWord(text);
				}
				return false;
			}
		});
		mEdittext.setImeOptions(EditorInfo.IME_ACTION_SEARCH);

		ImageButton clrBtn = (ImageButton) findViewById(R.id.ButtonClear);
		clrBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View view)
			{
				EditText edittext = (EditText) findViewById(R.id.EditText01);
				searchForward();
				edittext.setText("");
				edittext.requestFocus();
			}
		});

		new Thread() {
			@Override
			public void run()
			{
//				try {
//					sleep(100);
					loadIrreg();
					initDice();
					Log.i(TAG, "aDice Initiliezed");
					mInitialized = true;
					runOnUiThread(new Runnable() {
						@Override
						public void run()
						{
							dialog.dismiss();

							String text = mEdittext.getEditableText().toString();
							text = DiceFactory.convert(text);
							if (text.length() > 0) {
								mLast = text;

								search(text, 10);
							}

						}
					});
//				} catch (InterruptedException e) {
//				}
			}
		}.start();
		generateDisp(DISP_MODE_START, 0, null, mResultData, -1);
		mAdapter.notifyDataSetChanged();

		// クラッシュレポートハンドラの設定
		AndroidExceptionHandler.bind(this, "019749b2-5b96-4c90-b793-7e04cc8d3cc7");

		mClipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
	}

	private void initDice()
	{
		synchronized( this ){
			final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

			final String key = SettingsActivity.KEY_DICS;
			// Log.i(TAG,key );
			final String dicss = sp.getString(key, "");
			final String[] dics = dicss.split("\\|");

			// 外部辞書読込
			for (String name : dics) {
				if (name.length() == 0) {
					continue;
				}
				final IdicInfo dicinfo = mDice.open(name);
				if (dicinfo != null) {
					Log.i(TAG, "Open OK:" + name);

					// インデクスキャッシュファイル名取得
					final String filename = name.replace("/", ".") + ".idx";

					// インデクス作成
					if (!dicinfo.readIndexBlock(new IIndexCacheFile() {
						final String path = getCacheDir() + "/" + filename;

						@Override
						public FileInputStream getInput() throws FileNotFoundException
						{
							return new FileInputStream(path);
						}

						@Override
						public FileOutputStream getOutput() throws FileNotFoundException
						{
							return new FileOutputStream(path);
						}

					})) {
						mDice.close(dicinfo);
					} else {
						SettingsActivity.apllySettings(this, dicinfo);
					}
				} else {
					Log.i(TAG, "Open NG:" + name);
				}
			}
		}
	}

	// 英語向けIRREG読込
	private HashMap<String, String> loadIrreg()
	{
		final String name = "IrregDic.txt";

		try {

			BufferedReader in = new BufferedReader(new InputStreamReader(getAssets().open(name)));

			String str;
			while ((str = in.readLine()) != null) {
				int s = str.indexOf('\t');
				if (s != -1) {
					String s0 = str.substring(0, s);
					String s1 = str.substring(s + 1);
					mIrreg.put(s0, s1);
				}
			}
			in.close();
			Log.i(TAG, "Open OK:" + name);
			mDice.setIrreg(mIrreg);
		} catch (FileNotFoundException e) {
			Log.i(TAG, "Open NG:" + name);
		} catch (IOException e) {
			Log.i(TAG, "Open NG:" + name);
		}
		return mIrreg;
	}

	private void search(final String text, final int timer)
	{
		synchronized( this ){
			// Log.i("search ", text);

			if (mDiceThread != null) {
				// Log.i("search ", "int");
				mDiceThread.interrupt();
				try {
					// Log.i("search ", "join");
					mDiceThread.join();
					// Log.i("search ", "joined");
				} catch (InterruptedException e) {
				}
				mDiceThread = null;
			}

			mDiceThread = new Thread() {
				public void run()
				{
					// Log.i("search thread ", "start");
					searchProc(text, timer);
					// Log.i("search thread ", "end");
				}

				private void searchProc(String text, int timer)
				{

					final ArrayList<DicView.Data> result = new ArrayList<DicView.Data>();
					try {
						// Log.i("search thread ", "sleeping...");
						sleep(timer);
						// Log.i("search thread ", "got up");
						int dicnum = mDice.getDicNum();
						for (int dic = 0; dic < dicnum; dic++) {
							if (interrupted())
								return;

							if (!mDice.isEnable(dic)) {
								continue;
							}

							if (interrupted())
								return;

							mDice.search(dic, text);

							IdicResult pr = mDice.getResult(dic);

							if (interrupted())
								return;
							if (pr.getCount() > 0) {
								generateDisp(DISP_MODE_RESULT, dic, pr, result, -1);
								generateDisp(DISP_MODE_FOOTER, dic, null, result, -1);
							}

							if (interrupted())
								return;
						}

						if (result.size() == 0) {
							generateDisp(DISP_MODE_NORESULT, -1, null, result, -1);
						}
						if (!interrupted()) {
							runOnUiThread(new Runnable() {
								@Override
								public void run()
								{
									// TODO:
									mResultData.clear();
									for (DicView.Data d : result) {
										mResultData.add(d);
									}
									mAdapter.notifyDataSetChanged();
									mDicView.setSelectionFromTop(0, 0);
								}
							});
						}

					} catch (InterruptedException e) {
						// Log.i("search Thread ", "interrupted.");
					}

				}
			};
			mDiceThread.start();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item)
	{
		int id = item.getItemId();
		if (id == R.id.settings) {
			// 設定画面呼び出し
			Intent intent = new Intent();
			intent.setClassName("jp.sblo.pandora.adice", "jp.sblo.pandora.adice.SettingsActivity");
			startActivity(intent);
			return true;
		}
		if (id == R.id.help) {
			// About画面呼び出し
			Intent intent = new Intent();
			intent.setClassName("jp.sblo.pandora.adice", "jp.sblo.pandora.adice.AboutActivity");
			startActivity(intent);
			return true;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

		mLast = "";
		// 設定値反映
		mDelay = Integer.parseInt(sp.getString(SettingsActivity.KEY_DELAYSEARCH, "100"));
		for (int i = 0; i < mDice.getDicNum(); i++) {
			SettingsActivity.apllySettings(this, mDice.getDicInfo(i));
		}
		mFastScroll = sp.getBoolean(SettingsActivity.KEY_FASTSCROLL, false);
		mDicView.setFastScrollEnabled(mFastScroll);

		// タイ語フォントの適用
		if (  sp.getBoolean( SettingsActivity.KEY_THAI , false ) ){
			mFontCache.put(FontCache.NORMAL , mThaiFont  );
		}else{
			mFontCache.put(FontCache.NORMAL , mNormalFont  );
		}
		mEdittext.setTypeface(mFontCache.get(FontCache.NORMAL));

		// intentからのデータ取得
		Intent it = getIntent();
		String text = null;
		if (it != null && (
				Intent.ACTION_SEND.equals(it.getAction()) ||
				"jp.sblo.pandora.adice.action.SEARCH".equals(it.getAction())
			) && "text/plain".equals(it.getType())) {
			Bundle extras = it.getExtras();
			text = extras.getString(Intent.EXTRA_TEXT);
		}
		// クリップボード検索
		if (text == null && sp.getBoolean(SettingsActivity.KEY_CLIPBOARD_SEARCH, false)) {
			CharSequence clip = mClipboardManager.getText();
			if (mLastClipboard == null || !mLastClipboard.equals(clip)) {
				text = clip.toString();
				mLastClipboard = clip;
			}
		}
		if (text != null) {
			int pos = text.indexOf("\n");
			if (pos > 0) {
				text = text.substring(0, pos);
			}
			mEdittext.setText(text);
			mEdittext.setSelection(0, text.length());
		}else{
			text = mEdittext.getEditableText().toString();
			searchWord(text);
		}
	}

	/**
	 * DicView#Callbackの実装
	 */
	@Override
	public void onDicviewItemClicked(int position)
	{

		DicView.Data data = (DicView.Data) mAdapter.getItem(position);
		switch (data.getMode()) {
		case DicView.Data.MORE: {
			mResultData.remove(position);
			int dic = data.getDic();
			IdicResult pr = mDice.getMoreResult(dic);
			generateDisp(DISP_MODE_RESULT, dic, pr, mResultData, position);
			mAdapter.notifyDataSetChanged();
		}
			break;
		case DicView.Data.WORD: {

			final ArrayList<CharSequence> items = new ArrayList<CharSequence>();
			final ArrayList<CharSequence> disps = new ArrayList<CharSequence>();

			// <→リンク> 英辞郎形式
			{
				Pattern p = Pattern.compile("<(→(.+?))>");
				Matcher m = p.matcher(data.Trans);

				while (m.find()) {
					disps.add(m.group(1));
					items.add(m.group(2));
				}
			}
			// "→　" 和英辞郎形式
			{
				Pattern p = Pattern.compile("(→　(.+))");
				Matcher m = p.matcher(data.Trans);

				while (m.find()) {
					disps.add(m.group(1));
					items.add(m.group(2));
				}
			}

			// "＝リンク●" 略辞郎形式
			{
				Pattern p = Pattern.compile("(＝(.+))●");
				Matcher m = p.matcher(data.Trans);

				while (m.find()) {
					disps.add(m.group(1));
					items.add(m.group(2));
				}
			}

			DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which)
				{
					// selected dialog list item
					CharSequence cs = items.get(which);
					searchForward();
					mEdittext.setText(cs);
				}
			};

			if (disps.size() == 1) {
				listener.onClick(null, 0);
			} else if (disps.size() > 1) {
				new AlertDialog.Builder(this).setIcon(R.drawable.icon).setTitle(data.Index.toString()).setItems(
						disps.toArray(new CharSequence[0]), listener).show();
			}
		}
			break;
		case DicView.Data.NONE:
		case DicView.Data.NORESULT:
			break;
		}
	}

	private void generateDisp(int mode, int dic, IdicResult pr, ArrayList<DicView.Data> result, int pos)
	{
		synchronized( this ){
			switch (mode) {
			case DISP_MODE_RESULT: {
				// 表示させる内容を生成
				for (int i = 0; i < pr.getCount(); i++) {
					DicView.Data data = new DicView.Data(DicView.Data.WORD, dic);

					String idx = pr.getDisp(i);
					data.Index = idx;
					if (idx == null || idx.length() == 0) {
						data.Index = pr.getIndex(i);
					}

					data.Phone = pr.getPhone(i);
					data.Trans = pr.getTrans(i);
					data.Sample = pr.getSample(i);

					IdicInfo info = mDice.getDicInfo(dic);
					data.IndexSize = info.GetIndexSize();
					data.PhoneSize = info.GetPhoneticSize();
					data.TransSize = info.GetTransSize();
					data.SampleSize = info.GetSampleSize();

					data.PhoneFont = mFontCache.get( FontCache.PHONE );
					data.IndexFont = mFontCache.get( FontCache.NORMAL );
					data.TransFont = mFontCache.get( FontCache.NORMAL );
					data.SampleFont = mFontCache.get( FontCache.NORMAL );

					if (pos == -1) {
						result.add(data);
					} else {
						result.add(pos++, data);
					}
				}

				// 結果がまだあるようならmoreボタンを表示
				if (mDice.hasMoreResult(dic)) {
					DicView.Data data = new DicView.Data(DicView.Data.MORE, dic);

					data.Index = mMorebutton;

					if (pos == -1) {
						result.add(data);
					} else {
						result.add(pos++, data);
					}
				}
				break;
			}
			case DISP_MODE_FOOTER: {
				String dicname = mDice.getDicInfo(dic).GetDicName();
				if (dicname == null || dicname.length() == 0) {
					dicname = mDice.getDicInfo(dic).GetFilename();
				}
				DicView.Data data = new DicView.Data(DicView.Data.FOOTER, dic);

				data.Index = String.format(mFooter, dicname);
				if (pos == -1) {
					result.add(data);
				} else {
					result.add(pos++, data);
				}
				break;
			}
			case DISP_MODE_NORESULT: {
				DicView.Data data = new DicView.Data(DicView.Data.NONE, 0);
				data.Index = mNoResult;
				result.add(data);
				break;
			}
			case DISP_MODE_START: {
				String versionName = "-.-";
				int versionCode = 0;
				PackageManager pm = getPackageManager();
				try {
					PackageInfo info = null;
					info = pm.getPackageInfo("jp.sblo.pandora.adice", 0);
					versionName = info.versionName;
					versionCode = info.versionCode;
				} catch (NameNotFoundException e) {
				}
				String version = "Ver. " + String.format("%s (%d)", versionName, versionCode);
				String description = getResources().getString(R.string.description);

				DicView.Data data = new DicView.Data(DicView.Data.NONE, 0);
				data.Index = Html.fromHtml(mStartPage.replace("$version$", version).replace("$description$", description));
				result.add(data);
				break;
			}
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (event.getRepeatCount() == 0 && mSearchHistory.size() > 0) {
				return true;
			}
			synchronized (this) {
				if (!registLongPress) {
					registLongPress = true;
					mHandler.postDelayed(mLongPressAction, LONG_PRESS_DELAY);
				}
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// 短押し
			searchBackword();
			synchronized (this) {
				registLongPress = false;
				mHandler.removeCallbacks(mLongPressAction);
			}
			return true; // prevent back button action
		}
		return super.onKeyUp(keyCode, event);
	}

	private void searchForward()
	{

		if (mLast == null || mLast.equals("") || (mSearchHistory.size() > 0 && mLast.equals(mSearchHistory.get(0)))) {
			return;
		}
		mSearchHistory.add(0, mLast);
	}

	private void searchBackword()
	{
		if (mSearchHistory.size() > 0) {
			CharSequence cs = mSearchHistory.get(0);
			if (cs != null) {
				mSearchHistory.remove(0);
				mEdittext.setText(cs);
			}
		}

	}

	private void searchWord(CharSequence cs)
	{
		mEdittext.setText(cs);
		cs = DiceFactory.convert(cs);
		if (cs.length() > 0) {
			mLast = cs.toString();
			search(cs.toString(), 10);
		}
	}


}
