package jp.sblo.pandora.adice;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.widget.Toast;

public class InstallActivity extends Activity
{

    public static final String INTENT_SITE = "site";
    public static final String INTENT_SIZE = "size";
    public static final String INTENT_ENGLISH = "english";
    public static final String INTENT_NAME = "name";
    public static final String INTENT_FILE = "file";
    private Intent mIntent;
    private DownloadTask mDlTask;
    private File SDCARD;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ){
            SupportActionBar.addBackButton(this);
        }

        SDCARD = Environment.getExternalStorageDirectory();

        if ( ! Environment.MEDIA_MOUNTED.equals( Environment.getExternalStorageState() ) ) {
            finish();
            return;
        }

        mIntent = getIntent();

        setResult(RESULT_CANCELED);

        final Intent it = getIntent();
        if ( it!=null ){
            Uri ituri = it.getData();
            if ( ituri.getScheme().equals("adice") && ituri.getHost().equals("install" )){
                String params = ituri.getEncodedQuery();
                String [] paramarr = params.split("&");

                String name = null;
                boolean english = false;
                String site = null;
                String size = null;

                for( String param : paramarr ){
                    String[] namevalue = param.split("=");
                    if ( namevalue.length != 2 ){
                        continue;
                    }
                    if ( INTENT_NAME .equalsIgnoreCase(namevalue[0])  ){
                        name = Uri.decode(namevalue[1]);
                    }
                    if ( INTENT_ENGLISH.equalsIgnoreCase(namevalue[0]) ){
                        english = "true".equalsIgnoreCase(namevalue[1]);
                    }
                    if ( INTENT_SITE.equalsIgnoreCase(namevalue[0])  ){
                        site = Uri.decode(namevalue[1]);
                    }
                    if ( INTENT_SIZE.equalsIgnoreCase(namevalue[0])  ){
                        size = Uri.decode(namevalue[1]);
                    }
                }
                if ( name!=null ){
                    it.putExtra(INTENT_NAME, name);
                }
                it.putExtra(INTENT_ENGLISH, english);

                if ( mDlTask == null ){
                    mDlTask = new DownloadTask();
                    mDlTask.execute(site,size);
                }
            }
        }
    }




    class DownloadTask extends AsyncTask<String, Integer, String>
    {
        private ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            mProgressDialog = new ProgressDialog(InstallActivity.this);
            mProgressDialog.setTitle(R.string.download_process);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }


        @Override
        protected String doInBackground(String... params)
        {
            String filename = downloadDicfile( params[0] , params[1] );
            return filename;
        }


        @Override
        protected void onPostExecute(String result) {
            mProgressDialog.dismiss();

            if (result != null) {
                Toast.makeText(InstallActivity.this,R.string.download_ok, Toast.LENGTH_LONG).show();
                mIntent.putExtra(FileSelectorActivity.INTENT_FILEPATH, result);
                setResult(RESULT_OK , mIntent);
            } else {
                Toast.makeText(InstallActivity.this,R.string.download_ng, Toast.LENGTH_LONG).show();
            }
            mDlTask = null;
            finish();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            mProgressDialog.setProgress(progress[0]);
            if ( progress[1] > 0 ){
                mProgressDialog.setIndeterminate(false);
                mProgressDialog.setMax(progress[1]);
            }else{
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setMax(1);
            }
        }

        private String getName(String path)
        {
            String []patharr = path.split("/");

            return patharr[patharr.length - 1];
        }

        private String downloadDicfile(String site , String size )
        {

            String ret = null;
            String url = "http://"+site;
            try{
                // HTTP GET リクエスト
                HttpUriRequest httpGet = new HttpGet(url);
                DefaultHttpClient httpClient = new DefaultHttpClient();

                HttpConnectionParams.setSoTimeout(httpClient.getParams(), 30000);
                HttpConnectionParams.setConnectionTimeout(httpClient.getParams(),30000);

                HttpResponse httpResponse = httpClient.execute(httpGet);
                if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {

                    InputStream is = httpResponse.getEntity().getContent();
                    int length = (int)httpResponse.getEntity().getContentLength();

                    File f = new File( SDCARD.getPath() + "/adice/" + getName(url) );
                    f.getParentFile().mkdir();

                    FileOutputStream fos = new FileOutputStream(f);

                    byte[] buff = new byte[4096];
                    int	len;
                    int offset=0;
                    int lastoffset=0;
                    for( ;; ){
                        len = is.read(buff);
                        if ( len == -1  )break;
                        fos.write(buff, 0, len);
                        offset += len;

                        // update progress bar
                        if ( offset - lastoffset > 1024*16 ){
                            publishProgress( offset , length );
                            lastoffset = offset;
                        }
                    }
                    fos.close();
                    is.close();

                    if ( url.endsWith(".dic")){
                        ret = f.getPath() ;
                    }else if ( url.endsWith(".zip")){
                        ret = extractZip(f);
                        f.delete();
                    }
                }
            }
            catch(IOException e){}

            return ret;
        }


        private String  extractZip(File f)
        {
            String ret=null;
            ZipInputStream zis;
            try {
                zis = new ZipInputStream(new FileInputStream(f) );
                ZipEntry ze;
                while( ret==null && (ze= zis.getNextEntry())!=null ){
                    String name = ze.getName();

                    if ( name.toLowerCase().endsWith(".dic") ){
                        File nf = new File( SDCARD.getPath() + "/adice/" + getName(name) );
                        nf.getParentFile().mkdir();

                        FileOutputStream fos = new FileOutputStream(nf);

                        byte[] buff = new byte[512];
                        int	len;
                        int offset=0;
                        int lastoffset=0;

                        for( ;; ){
                            len = zis.read(buff);
                            if ( len == -1  )break;
                            fos.write(buff, 0, len);
                            offset += len;

                            // update progress bar
                            if ( offset - lastoffset > 1024*16 ){
                                publishProgress( offset , 0 );
                                lastoffset = offset;
                            }
                        }
                        fos.close();
                        ret = nf.getPath() ;
                    }
                    zis.closeEntry();
                }
                zis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return ret;
        }
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        int itemId = item.getItemId();
        if ( itemId == android.R.id.home ){
            finish();
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

}
