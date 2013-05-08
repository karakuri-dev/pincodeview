package com.karakuri.lib.pincodeview;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * A widget for displaying a pin code entry field.
 */
public class PinCodeView extends LinearLayout {
	private static final String TAG = "PinCodeView";

	private TextView mPinText;
	private int mPinLength;

	public PinCodeView(Context context) {
		super(context);
	}

	public PinCodeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs, R.attr.pinCodeViewStyle);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public PinCodeView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs, defStyle);
	}

	private void init(Context context, AttributeSet attrs, int defStyle) {
		Log.d(TAG, "[init]");
		mPinText = new TextView(context);
	}
	
	public void setPinLength(int newLength) {
		mPinLength = newLength;
	}
	
	public int getPinLength() {
		return mPinLength;
	}
	
	@Override
	public boolean onCheckIsTextEditor() {
		Log.d(TAG, "[onCheckIsTextEditor]");
		return true;
	}
	
	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		Log.d(TAG, "[onCreateInputConnection]");
		return super.onCreateInputConnection(outAttrs);
	}

}