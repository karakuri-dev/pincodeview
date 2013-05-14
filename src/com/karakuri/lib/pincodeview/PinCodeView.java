package com.karakuri.lib.pincodeview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.karakuri.lib.pincodeview.PinKeyListener.Type;

/**
 * A widget for displaying a pin code entry field.
 */
public class PinCodeView extends LinearLayout {
	private static final String TAG = "PinCodeView";

	private static final int DEFAULT_PIN_LENGTH = 4;

	/* values matching enum for R.styleable.PinCodeView_inputType */
	public static final int INPUT_TYPE_NUMERIC = 1;
	public static final int INPUT_TYPE_ALPHA = 2;
	public static final int INPUT_TYPE_ALPHA_NUMERIC = 3;

	private TextView mPinText;
	private int mMaxPinLength;

	private int mInputType;
	private int mImeOptions;
	private int mImeActionId;
	private CharSequence mImeActionLabel;

	private OnEditorActionListener mOnEditorActionListener;

	/**
	 * Interface definition for a callback to be invoked when an action is performed on the editor.
	 */
	public interface OnEditorActionListener {
		/**
		 * Called when an action is being performed.
		 * 
		 * @param view
		 *            The view for which the editor action was invoked
		 * @param actionId
		 *            Identifier of the action. This will be either the identifier you supplied, or
		 *            {@link EditorInfo#IME_NULL EditorInfo.IME_NULL} if being called due to the
		 *            enter key being pressed.
		 * @param event
		 *            If triggered by an enter key, this is the event; otherwise, this is null.
		 * @return Return true if you have consumed the action, else return false
		 */
		public boolean onEditorAction(PinCodeView view, int actionId, KeyEvent event);
	}

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

		int maxPinLength = DEFAULT_PIN_LENGTH;

		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PinCodeView, defStyle, 0);
		try {
			final int N = a.getIndexCount();
			for (int i = 0; i < N; i++) {
				int attr = a.getIndex(i);

				switch (attr) {
				case R.styleable.PinCodeView_pinLength:
					maxPinLength = a.getInt(attr, maxPinLength);
					break;
				case R.styleable.PinCodeView_inputType:
					mInputType = a.getInt(attr, INPUT_TYPE_NUMERIC);
					break;
				case R.styleable.PinCodeView_android_imeOptions:
					mImeOptions = a.getInt(attr, mImeOptions);
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

		mPinText = new TextView(context);
		mPinText.addTextChangedListener(mPinTextWatcher);

		setMaxPinLength(maxPinLength);
		setImeOptions(mImeOptions);
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

	public void setMaxPinLength(int newLength) {
		if (mMaxPinLength != newLength) {
			mMaxPinLength = newLength;
			mPinText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(newLength) });

			CharSequence text = mPinText.getText();
			if (text != null && text.length() > newLength) {
				mPinText.setText(text.subSequence(0, newLength));
				Selection.setSelection(mPinText.getEditableText(), newLength);
			}
		}
	}

	public int getMaxPinLength() {
		return mMaxPinLength;
	}

	public CharSequence getPin() {
		return mPinText.getText();
	}

	public void setInputType(int inputType) {
		PinKeyListener input;
		switch (inputType) {
		case INPUT_TYPE_NUMERIC:
			input = PinKeyListener.getInstance(Type.NUMERIC);
			break;
		case INPUT_TYPE_ALPHA:
			input = PinKeyListener.getInstance(Type.ALPHA);
			break;
		case INPUT_TYPE_ALPHA_NUMERIC:
			input = PinKeyListener.getInstance(Type.ALPHA_NUMERIC);
			break;
		default:
			throw new IllegalArgumentException("inputType must be one of INPUT_TYPE_NUMERIC,"
					+ "INPUT_TYPE_ALPHA, or INPUT_TYPE_ALPHA_NUMERIC");
		}

		mInputType = input.getInputType();
		mPinText.setKeyListener(input);

		InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
				Context.INPUT_METHOD_SERVICE);
		if (imm != null) imm.restartInput(this);
	}

	public int getInputType() {
		return mInputType;
	}

	public void setImeOptions(int options) {
		// try to prevent the IME from going full screen in landscape
		mImeOptions = options |= EditorInfo.IME_FLAG_NO_FULLSCREEN;
	}

	public int getImeOptions() {
		return mImeOptions;
	}

	public void setImeActionLabel(CharSequence label, int actionId) {
		mImeActionLabel = label;
		mImeActionId = actionId;
	}

	public CharSequence getImeActionLabel() {
		return mImeActionLabel;
	}

	public int getImeActionId() {
		return mImeActionId;
	}

	public void setOnEditorActionListener(OnEditorActionListener listener) {
		mOnEditorActionListener = listener;
	}

	@Override
	public boolean performClick() {
		Log.d(TAG, "[performClick]");
		InputMethodManager imm = getInputMethodManager();
		if (imm != null) {
			imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT);
		}
		return super.performClick();
	}

	private InputMethodManager getInputMethodManager() {
		return (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
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

		outAttrs.inputType = mInputType;
		outAttrs.imeOptions = mImeOptions;
		outAttrs.actionLabel = mImeActionLabel;
		outAttrs.actionId = mImeActionId;
		outAttrs.initialSelStart = Selection.getSelectionStart(mPinText.getText());
		outAttrs.initialSelEnd = Selection.getSelectionEnd(mPinText.getText());

		if (focusSearch(FOCUS_DOWN) != null) {
			outAttrs.imeOptions |= EditorInfo.IME_FLAG_NAVIGATE_NEXT;
		}
		if (focusSearch(FOCUS_UP) != null) {
			outAttrs.imeOptions |= EditorInfo.IME_FLAG_NAVIGATE_PREVIOUS;
		}
		// @formatter:off
		if ((outAttrs.imeOptions & EditorInfo.IME_MASK_ACTION) 
				== EditorInfo.IME_ACTION_UNSPECIFIED) {
			if ((outAttrs.imeOptions & EditorInfo.IME_FLAG_NAVIGATE_NEXT) != 0) {
				// An action has not been set, but the enter key will move to the next focus, so set
				// the editor action to that.
				outAttrs.imeOptions |= EditorInfo.IME_ACTION_NEXT;
			} else {
				// An action has not been set and there is no focus to move to
				outAttrs.imeOptions |= EditorInfo.IME_ACTION_DONE;
			}
		}
		// @formatter:on

		PinCodeInputConnection connection = new PinCodeInputConnection(this);
		return connection;
	}

	public void onEditorAction(int actionId) {
		Log.d(TAG, "[onEditorAction]");
		mPinText.onEditorAction(actionId);

		if (mOnEditorActionListener != null) {
			if (mOnEditorActionListener.onEditorAction(this, actionId, null)) {
				return;
			}
		}

		// Default handling for some standard actions
		if (actionId == EditorInfo.IME_ACTION_NEXT) {
			View v = focusSearch(FOCUS_FORWARD);
			if (v != null) {
				if (!v.requestFocus(FOCUS_FORWARD)) {
					throw new IllegalStateException("focus search returned a view "
							+ "that wasn't able to take focus!");
				}
			}
			return;

		} else if (actionId == EditorInfo.IME_ACTION_PREVIOUS) {
			View v = focusSearch(FOCUS_BACKWARD);
			if (v != null) {
				if (!v.requestFocus(FOCUS_BACKWARD)) {
					throw new IllegalStateException("focus search returned a view "
							+ "that wasn't able to take focus!");
				}
			}
			return;

		} else if (actionId == EditorInfo.IME_ACTION_DONE) {
			InputMethodManager imm = getInputMethodManager();
			if (imm != null && imm.isActive(this)) {
				imm.hideSoftInputFromWindow(getWindowToken(), 0);
			}
			return;
		}

		// unhandled action; pass it to the TextView
		mPinText.onEditorAction(actionId);
	}

	public void onPrivateIMECommand(String action, Bundle data) {
		Log.d(TAG, "[onPrivateIMECommand]");
		mPinText.onPrivateIMECommand(action, data);
	}

	public KeyListener getKeyListener() {
		Log.d(TAG, "[getKeyListener]");
		return mPinText.getKeyListener();
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

	@Override
	protected Parcelable onSaveInstanceState() {
		Log.d(TAG, "[onSaveInstanceState]");
		Parcelable superState = super.onSaveInstanceState();

		CharSequence text = mPinText.getText();
		if (!TextUtils.isEmpty(text)) {
			SavedState ss = new SavedState(superState);
			ss.text = text;
			return ss;
		}

		return superState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		Log.d(TAG, "[onRestoreInstanceState]");
		if (!(state instanceof SavedState)) {
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState ss = (SavedState) state;
		super.onRestoreInstanceState(ss.getSuperState());

		if (ss.text != null) {
			mPinText.setText(ss.text);
			Selection.setSelection(mPinText.getEditableText(), ss.text.length());
		}
	}

	public static class SavedState extends BaseSavedState {
		private CharSequence text;

		public SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel source) {
			super(source);
			text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			Log.d(TAG, "[writeToParcel]");
			super.writeToParcel(dest, flags);
			TextUtils.writeToParcel(text, dest, flags);
		}

		// @formatter:off
		public static final Parcelable.Creator<SavedState> CREATOR =
				new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
		// @formatter:on
	}
}