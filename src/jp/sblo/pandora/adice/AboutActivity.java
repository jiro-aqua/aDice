package jp.sblo.pandora.adice;


import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.webkit.WebView;

public class AboutActivity extends Activity
{


	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);

		WebView webview = (WebView)findViewById(R.id.WebView01);
		webview.loadUrl("file:///android_asset/about.html");

		final JsCallbackObj jsobj = new JsCallbackObj();
		webview.addJavascriptInterface(jsobj, "jscallback");

		webview.getSettings().setJavaScriptEnabled(true);
		webview.setFocusable(true);
		webview.setFocusableInTouchMode(true);
	}

	class JsCallbackObj
	{

		public JsCallbackObj()
		{
		}

		public String getAboutStrings(String key)
		{
			if (key.equals("version")) {

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
				return "Ver. " + String.format("%s (%d)", versionName, versionCode);
			} else if (key.equals("description")) {
				return getResources().getString(R.string.description);
			} else if (key.equals("manual")) {
				return getResources().getString(R.string.manual);
			} else {
				return "";
			}
		}
	}
}
