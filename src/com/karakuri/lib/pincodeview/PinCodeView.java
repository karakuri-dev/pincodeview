package com.karakuri.lib.pincodeview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
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

	private static final int DEFAULT_PIN_LENGTH = 4;

	/* values matching enum for R.styleable.PinCodeView_inputType */
	public static final int INPUT_TYPE_TEXT = EditorInfo.TYPE_CLASS_TEXT; // 0x1
	public static final int INPUT_TYPE_NUMBER = EditorInfo.TYPE_CLASS_NUMBER; // 0x2

	private TextView mPinText;
	private int mPinLength;

	private int mInputType;
	private int mImeOptions;
	private int mImeActionId;
	private CharSequence mImeActionLabel;

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

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PinCodeView, defStyle, 0);
		try {
			final int N = a.getIndexCount();
			for (int i = 0; i < N; i++) {
				int attr = a.getIndex(i);

				switch (attr) {
				case R.styleable.PinCodeView_pinLength:
					mPinLength = a.getInt(attr, DEFAULT_PIN_LENGTH);
					break;
				case R.styleable.PinCodeView_inputType:
					mInputType = a.getInt(attr, EditorInfo.TYPE_CLASS_NUMBER);
					break;
				case R.styleable.PinCodeView_android_imeOptions:
					mImeOptions = a.getInt(attr, EditorInfo.IME_ACTION_UNSPECIFIED);
					break;
				case R.styleable.PinCodeView_android_imeActionLabel:
					mImeActionLabel = a.getText(attr);
					break;
				case R.styleable.PinCodeView_android_imeActionId:
					mImeActionId = a.getInt(attr, 0);
					break;
				}
			}
		} finally {
			a.recycle();
		}

		// try to prevent the IME from going full screen in landscape
		mImeOptions |= EditorInfo.IME_FLAG_NO_FULLSCREEN;

		mPinText = new TextView(context);
		mPinText.addTextChangedListener(mPinTextWatcher);

		mPinText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(mPinLength) }); // temp
		mPinText.setImeOptions(mImeOptions); // temp
		mPinText.setImeActionLabel(mImeActionLabel, mImeActionId); // temp

		setInputType(mInputType);

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
			Log.d(TAG, String.format("[afterTextChanged] s = \"%s\"", s));
		}
	};

	public void setPinLength(int newLength) {
		mPinLength = newLength;
	}

	public int getPinLength() {
		return mPinLength;
	}

	public CharSequence getPin() {
		return mPinText.getText();
	}

	public void setInputType(int inputType) {
		switch (inputType) {
		case EditorInfo.TYPE_CLASS_TEXT:
			mInputType =
					EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
							| EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
			break;
		case EditorInfo.TYPE_CLASS_NUMBER:
			mInputType = EditorInfo.TYPE_CLASS_NUMBER | EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD;
			break;
		default:
			throw new IllegalArgumentException("inputType must be either "
					+ "EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_CLASS_NUMBER");
		}

		mPinText.setInputType(mInputType);

		InputMethodManager imm =
				(InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null) imm.restartInput(this);
	}

	public int getInputType() {
		return mInputType;
	}

	public void setImeOptions(int options) {

	}

	public int getImeOptions() {
		return mImeOptions;
	}

	public void setImeActionLabel(CharSequence label, int actionId) {

	}

	public CharSequence getImeActionLabel() {
		return mImeActionLabel;
	}

	public int getImeActionId() {
		return mImeActionId;
	}

	@Override
	public boolean performClick() {
		Log.d(TAG, "[performClick]");
		InputMethodManager imm =
				(InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
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

		outAttrs.inputType = mInputType;
		outAttrs.imeOptions = mImeOptions;
		outAttrs.actionLabel = mImeActionLabel;
		outAttrs.actionId = mImeActionId;

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