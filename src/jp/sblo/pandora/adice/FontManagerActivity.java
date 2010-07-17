package jp.sblo.pandora.adice;

import java.util.ArrayList;
import java.util.List;

import jp.sblo.pandora.adice.DicView.ResultAdapter.ViewHolder;

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
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.fontmanager);

		final ArrayList<String> fontlist = mFontCache.getList();
		fontlist.add( "" );

		setListAdapter( new FontAdapter(this,R.layout.fontlist_row , R.id.fontname01,fontlist) );

		final Activity thisAct = this;
		ListView lv = getListView();
		lv.setOnItemLongClickListener( new ListView.OnItemLongClickListener( ) {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id)
			{
				if ( position < fontlist.size()-1 ){
					new AlertDialog.Builder(thisAct)
					.setIcon(R.drawable.icon)
					.setTitle("Remove font")
					.setMessage("Do you remove the font ?\n" + fontlist.get(position) )
					.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

						public void onClick(DialogInterface dialog, int whichButton) {
					        // YESの処理
							new AlertDialog.Builder(thisAct)
							.setTitle("Remove font")
							.setMessage(fontlist.get(position)+" is removed.")
							.setPositiveButton(R.string.label_ok, null)
							.show();
					    }
					})
					.setNegativeButton("No", new DialogInterface.OnClickListener() {
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
				if ( position == fontlist.size()-1 ){
					// ファイル選択画面呼び出し
					Intent intent = new Intent();
					intent.setClassName("jp.sblo.pandora.adice", "jp.sblo.pandora.adice.FileSelectorActivity");
					intent.putExtra(FileSelectorActivity.INTENT_EXTENSION, new String[] { ".ttf" });
					startActivityForResult(intent, REQUEST_CODE_GETFONT);
				}
			}
		});


	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		// 辞書選択画面からの応答
		if (requestCode == REQUEST_CODE_GETFONT && resultCode == RESULT_OK && data != null) {
			final String dicname = data.getExtras().getString(FileSelectorActivity.INTENT_FILEPATH);

			Typeface tf = Typeface.createFromFile( dicname );


		}
		super.onActivityResult(requestCode, resultCode, data);
	}




	private class FontAdapter extends ArrayAdapter<String>
	{
		private class ViewHolder {
			TextView fontname;
			TextView sample;
		}

		public FontAdapter(Context context, int resource, int textViewResourceId, List<String> objects)
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
				holder.sample = (TextView)view.findViewById(R.id.fontsample);

				view.setTag( holder );
			}
			String item = getItem(position);

			if ( item.length() > 0 ){
				holder.fontname.setText(item);
				holder.sample.setText( mFontCache.getSampleString(item) );
				holder.sample.setTypeface(mFontCache.get(item));
			}else{
				holder.fontname.setText(getResources().getString(R.string.addfont));
				holder.sample.setText( "" );

			}
			return view;

		}

	}



}
