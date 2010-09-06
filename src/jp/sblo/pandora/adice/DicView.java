package jp.sblo.pandora.adice;

import java.util.ArrayList;

import android.util.AttributeSet;
import android.util.TypedValue;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class DicView extends ListView {

	static	class Data {
		public static final int WORD = 0;
		public static final int MORE = 1;
		public static final int NONE = 2;
		public static final int NORESULT = 3;
		public static final int FOOTER = 4;


		private int	mDic;
		private int mMode;
		public CharSequence Index;
		public CharSequence Phone;
		public CharSequence Trans;
		public CharSequence Sample;

		public Typeface IndexFont;
		public Typeface PhoneFont;
		public Typeface TransFont;
		public Typeface SampleFont;
		public int	IndexSize;
		public int	PhoneSize;
		public int	TransSize;
		public int	SampleSize;

		public  Data( int mode , int dic ){
			mDic = dic;
			mMode = mode;
		}

		public int getDic()
		{
			return mDic;
		}

		public int getMode()
		{
			return mMode;
		}
	}

	interface Callback {
		void onDicviewItemClicked(int position);
		boolean onDicviewItemLongClicked(int position);
	}

	private Callback mCallback;

	private void init(Context context)
	{
		setSmoothScrollbarEnabled(true);
		setScrollingCacheEnabled  (true);
		setFocusable(true);
		setFocusableInTouchMode(true);
	  	setFastScrollEnabled(true);
	  	setBackgroundColor(Color.WHITE);
	  	setCacheColorHint(Color.WHITE);
	  	setDividerHeight(0);
	  	setOnItemClickListener( new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent , View view, int position, long id)
			{
				if ( mCallback != null ){
					mCallback.onDicviewItemClicked(position);
				}
			}
		});
	  	setOnItemLongClickListener( new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent , View view, int position, long id)
			{
				if ( mCallback != null ){
					return mCallback.onDicviewItemLongClicked(position);
				}
				return false;
			}
		});

	}

	public DicView(Context context) {
		super(context);
		init(context);
	}

	public DicView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public DicView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public void setCallback( Callback cb )
	{
		mCallback = cb;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		requestFocus();
		return super.onTouchEvent(ev);
	}

//	@Override
//	public boolean onTouchEvent(MotionEvent ev) {
//		this.requestFocus();
//		return super.onTouchEvent(ev);
//	}


	static class ResultAdapter extends ArrayAdapter<Data>
	{

		static	class ViewHolder {
			TextView Index;
			TextView Phone;
			TextView Trans;
			TextView Sample;
			Button	moreButton;
			View	BarHr;
		}

		final private int margine_bottom;

		public ResultAdapter(Context context, int resource, int textViewResourceId, ArrayList<Data> objects)
		{
			super(context, resource, textViewResourceId, objects);
			margine_bottom = (int)context.getResources().getDimension(R.dimen.list_margine_bottom);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			final View view;
			ViewHolder holder;
			if (convertView != null && (convertView instanceof LinearLayout)) {
				view = convertView;
				holder = (ViewHolder) view.getTag();

			} else {
				view = inflate(getContext() , R.layout.list_row , null );

				holder = new ViewHolder();
				holder.Index = (TextView)view.findViewById(R.id.ListIndex);
				holder.Phone = (TextView)view.findViewById(R.id.ListPhone);
				holder.Trans = (TextView)view.findViewById(R.id.ListTrans);
				holder.Sample = (TextView)view.findViewById(R.id.ListSample);
				holder.moreButton = (Button)view.findViewById(R.id.ListMoreButton);
				holder.BarHr = (View)view.findViewById(R.id.ListHr);
				holder.moreButton.setOnClickListener( new OnClickListener(){

					@Override
					public void onClick(View v)
					{
						((DicView)v.getParent().getParent()).performItemClick((View)(v.getParent()),((DicView)v.getParent().getParent()).getPositionForView((View)v.getParent()) , 0);
					}
				});

				holder.Index.setTextColor(Color.BLACK);
				holder.Phone.setTextColor(Color.BLACK);
				holder.Trans.setTextColor(Color.BLACK);
				holder.Sample.setTextColor(Color.BLACK);
				holder.BarHr.setBackgroundColor(Color.BLACK);

				view.setTag(holder);
			}
			Data d = getItem(position);

			switch( d.getMode() ){
			case Data.WORD:
				setItem( holder.Index ,d.Index , d.IndexFont ,d.IndexSize );
				setItem( holder.Phone ,d.Phone , d.PhoneFont ,d.PhoneSize);
				setItem( holder.Trans ,d.Trans , d.TransFont ,d.TransSize);
				setItem( holder.Sample ,d.Sample ,d.SampleFont ,d.SampleSize);
				holder.moreButton.setVisibility(View.GONE);
				holder.BarHr.setVisibility(View.GONE);
				view.setPadding(0, 0, 0, margine_bottom);

				break;
			case Data.MORE:
				holder.Index.setVisibility(View.GONE);
				holder.Phone.setVisibility(View.GONE);
				holder.Trans.setVisibility(View.GONE);
				holder.Sample.setVisibility(View.GONE);
				holder.moreButton.setVisibility(View.VISIBLE);
				holder.moreButton.setText(d.Index);
				holder.BarHr.setVisibility(View.GONE);
				view.setPadding(0, 0, 0, 0);
				break;
			case Data.NORESULT:
			case Data.NONE:
				setItem( holder.Index ,d.Index , d.IndexFont ,16);
				holder.Phone.setVisibility(View.GONE);
				holder.Trans.setVisibility(View.GONE);
				holder.Sample.setVisibility(View.GONE);
				holder.moreButton.setVisibility(View.GONE);
				holder.BarHr.setVisibility(View.GONE);
				view.setPadding(0, 0, 0, 0);
				break;
			case Data.FOOTER:
				setItem( holder.Index ,d.Index , d.IndexFont ,16);
				holder.Phone.setVisibility(View.GONE);
				holder.Trans.setVisibility(View.GONE);
				holder.Sample.setVisibility(View.GONE);
				holder.moreButton.setVisibility(View.GONE);
				holder.BarHr.setVisibility(View.VISIBLE);
				view.setPadding(0, 0, 0, 0);
				break;
			default:
				holder.Index.setVisibility(View.GONE);
				holder.Phone.setVisibility(View.GONE);
				holder.Trans.setVisibility(View.GONE);
				holder.Sample.setVisibility(View.GONE);
				holder.moreButton.setVisibility(View.GONE);
				holder.BarHr.setVisibility(View.GONE);
				view.setPadding(0, 0, 0, 0);
			}
			// TODO 自動生成されたメソッド・スタブ
			return view;
		}

		private void setItem( TextView tv , CharSequence str, Typeface tf , int size )
		{
			if (str ==null || str.length()==0 ){
				tv.setVisibility(View.GONE);
			}else{
				tv.setVisibility(View.VISIBLE);
				tv.setText(str);
				tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, size );
				tv.setTypeface( tf );
			}
		}
	}
}
