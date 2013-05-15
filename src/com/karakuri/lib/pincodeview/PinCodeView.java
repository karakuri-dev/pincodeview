package com.karakuri.lib.pincodeview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Selection;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyCharacterMap;
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

	// for backwards compatible hasOnClickListeners
	private OnClickListener mOnClickListener;

	private InputContentInfo mInputContentInfo;

	private static class InputContentInfo {
		int inputType;
		int imeOptions;
		int imeActionId;
		CharSequence imeActionLabel;
		OnEditorActionListener onEditorActionListener;
		boolean enterDown;
	}

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
		mInputContentInfo = new InputContentInfo();

		int maxPinLength = DEFAULT_PIN_LENGTH;
		int inputType = INPUT_TYPE_NUMERIC;
		int imeOptions = EditorInfo.TYPE_NULL;

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
					inputType = a.getInt(attr, inputType);
					break;
				case R.styleable.PinCodeView_android_imeOptions:
					imeOptions = a.getInt(attr, imeOptions);
					break;
				case R.styleable.PinCodeView_android_imeActionLabel:
					mInputContentInfo.imeActionLabel = a.getText(attr);
					break;
				case R.styleable.PinCodeView_android_imeActionId:
					mInputContentInfo.imeActionId = a.getInt(attr, 0);
					break;
				}
			}
		} finally {
			a.recycle();
		}

		mPinText = new TextView(context);
		mPinText.addTextChangedListener(mPinTextWatcher);

		setMaxPinLength(maxPinLength);
		setInputType(inputType);
		setImeOptions(imeOptions);

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

		mInputContentInfo.inputType = input.getInputType();
		mPinText.setKeyListener(input);

		InputMethodManager imm = (InputMethodManager) getContext().getSystemService(
				Context.INPUT_METHOD_SERVICE);
		if (imm != null) imm.restartInput(this);
	}

	public int getInputType() {
		return mInputContentInfo.inputType;
	}

	public void setImeOptions(int options) {
		// try to prevent the IME from hiding the view in landscape
		options = options | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN;
		mInputContentInfo.imeOptions = options;
	}

	public int getImeOptions() {
		return mInputContentInfo.imeOptions;
	}

	public void setImeActionLabel(CharSequence label, int actionId) {
		mInputContentInfo.imeActionLabel = label;
		mInputContentInfo.imeActionId = actionId;
	}

	public CharSequence getImeActionLabel() {
		return mInputContentInfo.imeActionLabel;
	}

	public int getImeActionId() {
		return mInputContentInfo.imeActionId;
	}

	public void setOnEditorActionListener(OnEditorActionListener listener) {
		mInputContentInfo.onEditorActionListener = listener;
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
		if (!isEnabled()) {
			return null;
		}

		outAttrs.inputType = mInputContentInfo.inputType;
		outAttrs.imeOptions = mInputContentInfo.imeOptions;
		outAttrs.actionLabel = mInputContentInfo.imeActionLabel;
		outAttrs.actionId = mInputContentInfo.imeActionId;
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
		if (mInputContentInfo.onEditorActionListener != null) {
			if (mInputContentInfo.onEditorActionListener.onEditorAction(this, actionId, null)) {
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

		// unhandled action; dispatch Enter key
		long eventTime = SystemClock.uptimeMillis();
		int keyCharMap = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				? KeyCharacterMap.VIRTUAL_KEYBOARD : 0;
		// @formatter:off
		dispatchKeyEvent(new KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN,
				KeyEvent.KEYCODE_ENTER, 0, 0, keyCharMap, 0, KeyEvent.FLAG_SOFT_KEYBOARD
				| KeyEvent.FLAG_KEEP_TOUCH_MODE | KeyEvent.FLAG_EDITOR_ACTION));
		dispatchKeyEvent(new KeyEvent(SystemClock.uptimeMillis(), eventTime, KeyEvent.ACTION_UP,
				KeyEvent.KEYCODE_ENTER, 0, 0, keyCharMap, 0, KeyEvent.FLAG_SOFT_KEYBOARD
				| KeyEvent.FLAG_KEEP_TOUCH_MODE | KeyEvent.FLAG_EDITOR_ACTION));
		// @formatter:on
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
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		Log.d(TAG, "[onKeyPreIme]");
		if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
			return super.onKeyPreIme(keyCode, event);
		}
		return mPinText.onKeyPreIme(keyCode, event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "[onKeyDown]");
		int which = doKeyDown(keyCode, event, null);
		if (which == 0) {
			// go through default dispatching
			return super.onKeyDown(keyCode, event);
		}
		return true;
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.d(TAG, "[onKeyUp]");
		if (!isEnabled()) {
			return super.onKeyUp(keyCode, event);
		}

		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
			/*
			 * If there is a click listener, just call through to super, which will invoke it. If
			 * not, try to show the soft input method. (It will also call performClick(), but that
			 * won't do anything in this case.)
			 */
			if (!hasOnClickListeners()) {
				InputMethodManager imm = getInputMethodManager();
				if (imm != null) {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
						imm.viewClicked(this);
					}
					imm.showSoftInput(this, 0);
				}
			}
			return super.onKeyUp(keyCode, event);

		case KeyEvent.KEYCODE_ENTER:
			if (mInputContentInfo.onEditorActionListener != null && mInputContentInfo.enterDown) {
				mInputContentInfo.enterDown = false;
				if (mInputContentInfo.onEditorActionListener.onEditorAction(this,
						EditorInfo.IME_NULL, event)) {
					return true;
				}
			}

			if ((event.getFlags() & KeyEvent.FLAG_EDITOR_ACTION) != 0 || true) {
				/*
				 * If there is a click listener, just call through to super, which will invoke it.
				 * If not, try to advance focus, but still call through to super, which will reset
				 * the pressed state and longpress state. (It will also call performClick(), but
				 * that won't do anything in this case.)
				 */
				if (!hasOnClickListeners()) {
					View v = focusSearch(FOCUS_DOWN);

					if (v != null) {
						if (!v.requestFocus(FOCUS_DOWN)) {
							throw new IllegalStateException("focus search returned a view "
									+ "that wasn't able to take focus!");
						}

						// Return true because we handled the key
						// Super will return false because there was no click listener.
						super.onKeyUp(keyCode, event);
						return true;
					} else if ((event.getFlags() & KeyEvent.FLAG_EDITOR_ACTION) != 0) {
						// No target for next focus, but make sure the IME is hidden
						// if this came from it.
						InputMethodManager imm = getInputMethodManager();
						if (imm != null && imm.isActive(this)) {
							imm.hideSoftInputFromWindow(getWindowToken(), 0);
						}
					}
				}
			}
			return super.onKeyUp(keyCode, event);
		}

		if (mPinText.getKeyListener() != null) {
			if (mPinText.getKeyListener().onKeyUp(mPinText, mPinText.getEditableText(), keyCode,
					event)) {
				return true;
			}
		}
		return super.onKeyUp(keyCode, event);
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
		KeyEvent down = KeyEvent.changeAction(event, KeyEvent.ACTION_DOWN);

		int which = doKeyDown(keyCode, down, event);
		if (which == 0) {
			// go through default dispatching
			return super.onKeyMultiple(keyCode, repeatCount, event);
		}
		if (which == -1) {
			return true; // consumed
		}

		repeatCount--;

		// Dispatch the remaining events to the input method.
		KeyEvent up = KeyEvent.changeAction(event, KeyEvent.ACTION_UP);
		if (which == 1) {
			// keyListener not null from doKeyDown
			KeyListener keyListener = mPinText.getKeyListener();
			Editable text = mPinText.getEditableText();

			keyListener.onKeyUp(mPinText, text, keyCode, up);
			while (--repeatCount > 0) {
				keyListener.onKeyDown(mPinText, text, keyCode, down);
				keyListener.onKeyUp(mPinText, text, keyCode, up);
			}
		}

		return true;
	}

	@Override
	public boolean onKeyShortcut(int keyCode, KeyEvent event) {
		Log.d(TAG, "[onKeyShortcut]");
		return mPinText.onKeyShortcut(keyCode, event);
	}

	/*
	 * Helper for other key event callbacks.
	 */
	private int doKeyDown(int keyCode, KeyEvent event, KeyEvent otherEvent) {
		if (!isEnabled()) {
			return 0;
		}

		switch (keyCode) {
		case KeyEvent.KEYCODE_ENTER:
			// If there is an action listener, given it a chance to consume the event.
			if (mInputContentInfo.onEditorActionListener != null) {
				if (mInputContentInfo.onEditorActionListener.onEditorAction(this,
						EditorInfo.IME_NULL, event)) {
					return -1; // consumed
				}
			}

			if (hasOnClickListeners()) {
				return 0; // dispatch to super
			}
			return -1; // consumed

		case KeyEvent.KEYCODE_DPAD_CENTER:
			return 0; // dispatch to super

		case KeyEvent.KEYCODE_TAB:
			return 0; // dispatch to super
		}

		// key listener should always be non-null
		KeyListener keyListener = mPinText.getKeyListener();
		if (keyListener != null) {
			boolean doDown = true;
			if (otherEvent != null) {
				mPinText.beginBatchEdit();
				final boolean handled = keyListener.onKeyOther(mPinText,
						mPinText.getEditableText(), otherEvent);
				doDown = false;
				mPinText.endBatchEdit();
				if (handled) return -1; // consumed
			}

			if (doDown) {
				mPinText.beginBatchEdit();
				final boolean handled = keyListener.onKeyDown(mPinText, mPinText.getEditableText(),
						keyCode, event);
				mPinText.endBatchEdit();
				if (handled) return 1; // edited text
			}
		}

		return 0; // dispatch to super
	}

	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
	@Override
	public boolean hasOnClickListeners() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
			return super.hasOnClickListeners();
		} else {
			return mOnClickListener != null;
		}
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