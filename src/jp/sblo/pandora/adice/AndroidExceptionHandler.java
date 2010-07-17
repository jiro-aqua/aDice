// original package from "package jp.benishouga.common;"
// http://aexceptions.appspot.com/ made by benishouga.
// Thanks!

package jp.sblo.pandora.adice;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

public class AndroidExceptionHandler implements UncaughtExceptionHandler {
//	private static final String META_KEY_URL = "jp.benishouga.exception_post_url";
	private static final String DEFAULT_POST_URL = "http://aexceptions.appspot.com/post";

	private Context mContext;
	private PackageInfo mPackInfo;
	private UncaughtExceptionHandler mDefaultUEH;
	private String mAppId;
	private File mBugFile;

	public static void bind(Context context, String appId) {
		ApplicationInfo appInfo;
		try {
			appInfo = context.getPackageManager().getApplicationInfo(
					context.getPackageName(),
					0);
		}
		catch( NameNotFoundException e) {
			return ;
		}
		if (( appInfo !=null && (appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE)){
			// DEBUGモードでは何もしない
			return ;
		}

		// release モードでのみクラッシュレポートを動作させる
		AndroidExceptionHandler handler = new AndroidExceptionHandler(context,
				appId);
		handler.showBugReportDialogIfExist();

		Thread.setDefaultUncaughtExceptionHandler(handler);
	}

	public AndroidExceptionHandler(Context context, String appId) {
		mContext = context;
		mAppId = appId;
		mBugFile = getBugFile();

		try {
			mPackInfo = context.getPackageManager().getPackageInfo(
					context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		mDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
	}

	public void uncaughtException(Thread th, Throwable t) {
		try {
			saveState(t);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		mDefaultUEH.uncaughtException(th, t);
	}

	protected File getBugFile() {
		String sdcard = Environment.getExternalStorageDirectory().getPath();
		String path = sdcard + File.separator + mAppId + "e.txt";
		return new File(path);
	}

	protected void saveState(Throwable e) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new FileOutputStream(mBugFile));
		e.printStackTrace(pw);
		pw.close();
	}

	protected final void showBugReportDialogIfExist() {
		if (mBugFile != null & mBugFile.exists()) {
			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
			builder.setTitle(mContext.getResources().getString(R.string.crash_report));
			builder.setMessage(mContext.getResources().getString(R.string.crash_report_text));
			builder.setNegativeButton("Cancel", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					if (mBugFile.exists()) {
						mBugFile.delete();
					}
					dialog.dismiss();
				}
			});
			builder.setPositiveButton("Post", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					postBugReportInBackground();// バグ報告
					dialog.dismiss();
				}
			});
			AlertDialog dialog = builder.create();
			dialog.show();
		}
	}

	protected void postBugReportInBackground() {
		final String bug = getFileBody();
		new Thread(new Runnable() {
			public void run() {
				postBugReport(bug);
				if (mBugFile != null && mBugFile.exists()) {
					mBugFile.delete();
				}
			}
		}).start();
	}

	protected void postBugReport(String bug) {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("appId", mAppId));
		nvps.add(new BasicNameValuePair("dev", Build.DEVICE));
		nvps.add(new BasicNameValuePair("mod", Build.MODEL));
		nvps.add(new BasicNameValuePair("sdk", Build.VERSION.SDK));
		nvps.add(new BasicNameValuePair("ver", mPackInfo.versionName));
		nvps.add(new BasicNameValuePair("bug", bug));
		try {
//			Bundle meta = mContext.getPackageManager().getApplicationInfo(
//					mContext.getPackageName(),
//					android.content.pm.PackageManager.GET_META_DATA).metaData;
//			String url = null;
//			if (meta != null && meta.containsKey(META_KEY_URL)) {
//				url = meta.getString(META_KEY_URL);
//			}
			HttpPost httpPost = new HttpPost( DEFAULT_POST_URL );
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
			DefaultHttpClient httpClient = new DefaultHttpClient();
			httpClient.execute(httpPost);
//		} catch (NameNotFoundException e) {
//			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected String getFileBody() {
		StringBuilder sb = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(mBugFile));
			String line;
			while ((line = br.readLine()) != null) {
				sb.append(line.replace("\t", "  ")).append("\r\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}

}