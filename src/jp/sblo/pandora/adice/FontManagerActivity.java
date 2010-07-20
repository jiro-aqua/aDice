package jp.sblo.pandora.adice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.io.File;
import java.util.List;

import jp.sblo.pandora.adice.FontCache.fontName;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class FontManagerActivity extends ListActivity
{
    private int REQUEST_CODE_GETFONT = 0x1236;
	FontCache mFontCache = FontCache.getInstance();
	private ArrayList<FontCache.fontName> mFontList;
	private FontAdapter mFontAdapter = null;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.fontmanager);

		setResult(RESULT_OK);

		loadList();

		final Activity thisAct = this;
		ListView lv = getListView();
		lv.setOnItemLongClickListener( new ListView.OnItemLongClickListener( ) {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id)
			{
				final String fontname = mFontList.get(position).fontname;
				final String filename = mFontList.get(position).filename;
				if ( position < mFontList.size()-1 ){
					new AlertDialog.Builder(thisAct)
					.setIcon(R.drawable.icon)
					.setTitle(R.string.remove_font)
					.setMessage( getResources().getString(R.string.remove_font_confirm, fontname) )
					.setPositiveButton(R.string.label_yes, new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int whichButton) {
					        // YESの処理
							new AlertDialog.Builder(thisAct)
							.setTitle(R.string.remove_font)
							.setMessage( getResources().getString(R.string.remove_font_message, fontname)  )
							.setPositiveButton(R.string.label_ok, null)
							.show();

							mFontCache.remove(filename);
							loadList();
					    }

					})
					.setNegativeButton(R.string.label_no, new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog, int whichButton) {
					        // ここにNOの処理
					    }
					})
					.show();
				}
				return false;
			}
		});
		lv.setOnItemClickListener( new OnItemClickListener( ) {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				if ( position == mFontList.size()-1 ){
					// ファイル選択画面呼び出し
					Intent intent = new Intent();
					intent.setClassName("jp.sblo.pandora.adice", "jp.sblo.pandora.adice.FileSelectorActivity");
					intent.putExtra(FileSelectorActivity.INTENT_EXTENSION, new String[] { ".ttf" });
					startActivityForResult(intent, REQUEST_CODE_GETFONT);
				}
			}
		});

	}


	private void loadList(){
		mFontList = mFontCache.getList();

		Collections.sort(mFontList ,  new Comparator<FontCache.fontName>(){

			@Override
			public int compare(fontName object1, fontName object2)
			{
				return object1.fontname.compareToIgnoreCase(object2.fontname);
			}

		});
		mFontList.add( new FontCache.fontName("","") );

		mFontAdapter = new FontAdapter(this,R.layout.fontlist_row , R.id.fontname01 , mFontList);
		setListAdapter( mFontAdapter );
	}

	public static Typeface createTypefaceFromFile( File f ){
		if ( f.exists() ){
			return Typeface.createFromFile( f );
		}
		return null;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// 辞書選択画面からの応答
		if (requestCode == REQUEST_CODE_GETFONT && resultCode == RESULT_OK && data != null) {
			final String fontname = data.getExtras().getString(FileSelectorActivity.INTENT_FILEPATH);

			File file = new File( fontname );
			if ( file.exists() ){
				Typeface tf = Typeface.createFromFile( fontname );
				mFontCache.put( fontname , tf );
				loadList();
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}




	private class FontAdapter extends ArrayAdapter<FontCache.fontName>
	{
		private class ViewHolder {
			TextView fontname;
			TextView filename;
			TextView sample;
		}

		public FontAdapter(Context context, int resource, int textViewResourceId, List<FontCache.fontName> objects)
		{
			super(context, resource, textViewResourceId, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View view = convertView;
			ViewHolder holder;
			if ( view!=null  ){
				holder = (ViewHolder) view.getTag();
			}else{
				view = View.inflate(getContext() , R.layout.fontlist_row , null );
				holder = new ViewHolder();

				holder.fontname = (TextView)view.findViewById(R.id.fontname01);
				holder.filename = (TextView)view.findViewById(R.id.fontname02);
				holder.sample = (TextView)view.findViewById(R.id.fontsample);

				view.setTag( holder );
			}
			FontCache.fontName item = getItem(position);

			if ( item.fontname.length() > 0 ){
				holder.fontname.setText(item.fontname);
				holder.filename.setText(item.filename);
				holder.sample.setText( mFontCache.getSampleString(item.filename) );
				holder.sample.setTypeface(mFontCache.get(item.filename));
			}else{
				holder.fontname.setText(getResources().getString(R.string.addfont));
				holder.sample.setText( "" );

			}
			return view;

		}

	}



}
