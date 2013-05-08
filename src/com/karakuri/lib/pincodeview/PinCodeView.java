package com.karakuri.lib.pincodeview;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
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
		mPinText.setInputType(EditorInfo.TYPE_CLASS_NUMBER); // temp
		mPinText.addTextChangedListener(mPinTextWatcher);

		setClickable(true);
		setFocusableInTouchMode(true);
	}

	private TextWatcher mPinTextWatcher = new TextWatcher() {

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void afterTextChanged(Editable s) {
			Log.d(TAG, "[afterTextChanged] s = " + s);
		}
	};

	public void setPinLength(int newLength) {
		mPinLength = newLength;
	}

	public int getPinLength() {
		return mPinLength;
	}

	@Override
	public boolean performClick() {
		Log.d(TAG, "[performClick]");
		InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
				Context.INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
		}
		return super.performClick();
	}

	@Override
	public boolean onCheckIsTextEditor() {
		Log.d(TAG, "[onCheckIsTextEditor]");
		return true;
	}

	public TextView getInputTextView() {
		return mPinText;
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		Log.d(TAG, "[onCreateInputConnection]");

		PinCodeInputConnection connection = new PinCodeInputConnection(this);

		outAttrs.inputType = EditorInfo.TYPE_CLASS_NUMBER; // temp
		outAttrs.imeOptions = EditorInfo.IME_ACTION_UNSPECIFIED; // temp

		return connection;
	}

	public KeyListener getKeyListener() {
		Log.d(TAG, "[getKeyListener]");
		return mPinText.getKeyListener();
	}

	public void onEditorAction(int actionCode) {
		Log.d(TAG, "[onEditorAction]");
		mPinText.onEditorAction(actionCode);
	}

	public void onPrivateIMECommand(String action, Bundle data) {
		Log.d(TAG, "[onPrivateIMECommand]");
		mPinText.onPrivateIMECommand(action, data);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "[onKeyDown]");
		return mPinText.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.d(TAG, "[onKeyUp]");
		return mPinText.onKeyUp(keyCode, event);
	}

	@TargetApi(Build.VERSION_CODES.ECLAIR)
	@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		Log.d(TAG, "[onKeyLongPress]");
		return mPinText.onKeyLongPress(keyCode, event);
	}

	@Override
	public boolean onKeyMultiple(int keyCode, int repeatCount, KeyEvent event) {
		Log.d(TAG, "[onKeyMultiple]");
		return mPinText.onKeyMultiple(keyCode, repeatCount, event);
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		Log.d(TAG, "[onKeyPreIme]");
		return mPinText.onKeyPreIme(keyCode, event);
	}

	@Override
	public boolean onKeyShortcut(int keyCode, KeyEvent event) {
		Log.d(TAG, "[onKeyShortcut]");
		return mPinText.onKeyShortcut(keyCode, event);
	}
}