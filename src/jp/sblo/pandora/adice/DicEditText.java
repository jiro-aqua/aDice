package jp.sblo.pandora.adice;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

public class DicEditText extends EditText {

	public DicEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public DicEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DicEditText(Context context) {
		super(context);
	}

	@Override
	protected void onFocusChanged(boolean focused, int direction,
			Rect previouslyFocusedRect) {
		if (!focused) {
			InputMethodManager inputMethodManager = (InputMethodManager) getContext()
					.getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(getWindowToken(), 0);
		}
		super.onFocusChanged(focused, direction, previouslyFocusedRect);
	}
}
