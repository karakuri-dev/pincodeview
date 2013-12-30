/*
 * Copyright (C) 2013 Karakuri <karakuri.dev@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.karakuri.lib.pincodeview;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
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
import android.widget.ImageView;
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
	/** Input type for pins composed only of numbers. */
	public static final int INPUT_TYPE_NUMERIC = 1;
	/** Input type for pins composed only of letters. */
	public static final int INPUT_TYPE_ALPHA = 2;
	/** Input type for pins composed of letters and numbers. */
	public static final int INPUT_TYPE_ALPHA_NUMERIC = 3;

	private TextView mPinText;
	private int mMaxPinLength;
	private Drawable mIndicatorDrawable;
	private Drawable mIndicatorBackground;

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
		 * @param view The view for which the editor action was invoked
		 * @param actionId Identifier of the action. This will be either the identifier you
		 *            supplied, or {@link EditorInfo#IME_NULL EditorInfo.IME_NULL} if being called
		 *            due to the enter key being pressed.
		 * @param event If triggered by an enter key, this is the event; otherwise, this is null.
		 * @return Return true if you have consumed the action, else return false
		 */
		public boolean onEditorAction(PinCodeView view, int actionId, KeyEvent event);
	}

	public PinCodeView(Context context) {
		this(context, null);
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
				case R.styleable.PinCodeView_pinIndicatorDrawable:
					mIndicatorDrawable = a.getDrawable(attr);
					break;
				case R.styleable.PinCodeView_pinIndicatorBackground:
					mIndicatorBackground = a.getDrawable(attr);
					break;
				}
			}
		} finally {
			a.recycle();
		}

		mPinText = new TextView(context);
		mPinText.addTextChangedListener(mPinTextWatcher);

		setInputType(inputType);
		setImeOptions(imeOptions);
		setMaxPinLength(maxPinLength);

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
			updateIndicators();
		}
	};

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private void createChildViews() {
		removeAllViews();

		Context context = getContext();
		LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1);
		for (int i = 0; i < mMaxPinLength; i++) {
			PinIndicator child = new PinIndicator(context);
			child.setImageDrawable(mIndicatorDrawable);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
				child.setBackground(mIndicatorBackground);
			} else {
				child.setBackgroundDrawable(mIndicatorBackground);
			}

			addView(child, params);
		}

		updateIndicators();
	}

	private void updateIndicators() {
		final int length = mPinText.getText().length();
		for (int i = 0; i < mMaxPinLength; i++) {
			View child = getChildAt(i);
			if (!(child instanceof PinIndicator)) {
				throw new IllegalStateException("PinCodeView cannot have other child views.");
			}

			PinIndicator indicator = (PinIndicator) child;
			indicator.setIsEmpty(i < length);
			indicator.setIsActive(i == length);
		}
	}

	/**
	 * Get the currently entered pin text.
	 */
	public String getPin() {
		return mPinText.getText().toString();
	}

	/**
	 * Get the length of the currently entered pin text (rather than the max length allowed by this
	 * PinCodeView). Clients may find this useful if a pin shorter than the max allowed length is
	 * acceptable input, otherwise you can use {@link #isPinFilled()}.
	 */
	public int getPinLength() {
		return mPinText.getText().length();
	}

	/**
	 * Returns true if the current pin text is empty, false otherwise.
	 */
	public boolean isPinEmpty() {
		return getPinLength() == 0;
	}

	/**
	 * Returns true if the current pin text fills the available indicators, false otherwise.
	 */
	public boolean isPinFilled() {
		return getPinLength() == mMaxPinLength;
	}

	/**
	 * Set the maximum pin length allowed. This will update the number of indicators shown by this
	 * view. The current pin text will be maintained, but it will be truncated if it exceeds the new
	 * maximum.
	 *
	 * @see #getMaxPinLength()
	 * @attr {@link R.styleable#PinCodeView_pinLength}
	 */
	public void setMaxPinLength(int newLength) {
		if (mMaxPinLength != newLength) {
			mMaxPinLength = newLength;
			mPinText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(newLength) });

			CharSequence text = mPinText.getText();
			if (text != null && text.length() > newLength) {
				mPinText.setText(text.subSequence(0, newLength));
				Selection.setSelection(mPinText.getEditableText(), newLength);
			}

			createChildViews();
		}
	}

	/**
	 * Get the maximum pin length allowed. This is equal to the number of indicators shown.
	 *
	 * @see #getMaxPinLength()
	 */
	public int getMaxPinLength() {
		return mMaxPinLength;
	}

	/**
	 * Set the {@link Drawable} used for the pin indicators.
	 *
	 * @see #getPinIndicatorDrawable()
	 * @attr {@link R.styleable#PinCodeView_pinIndicatorDrawable}
	 */
	public void setPinIndicatorDrawable(Drawable d) {
		if (mIndicatorDrawable != d) {
			mIndicatorDrawable = d;

			final int count = getChildCount();
			for (int i = 0; i < count; i++) {
				View child = getChildAt(i);
				if (!(child instanceof PinIndicator)) {
					throw new IllegalStateException("PinCodeView cannot have other child views.");
				}

				((PinIndicator) child).setImageDrawable(d);
			}
		}
	}

	/**
	 * Get the current pin indicator drawable.
	 *
	 * @see #setPinIndicatorDrawable(Drawable)
	 */
	public Drawable getPinIndicatorDrawable() {
		return mIndicatorDrawable;
	}

	/**
	 * Set the background {@link Drawable} used for the pin indicators.
	 *
	 * @see #getPinIndicatorBackground()
	 * @attr {@link R.styleable#PinCodeView_pinIndicatorBackground}
	 */
	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void setPinIndicatorBackground(Drawable d) {
		if (mIndicatorBackground != d) {
			mIndicatorBackground = d;

			final int count = getChildCount();
			for (int i = 0; i < count; i++) {
				View child = getChildAt(i);
				if (!(child instanceof PinIndicator)) {
					throw new IllegalStateException("PinCodeView cannot have other child views.");
				}

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
					child.setBackground(mIndicatorBackground);
				} else {
					child.setBackgroundDrawable(mIndicatorBackground);
				}
			}
		}
	}

	/**
	 * Get the current pin indicator background drawable.
	 *
	 * @see #setPinIndicatorBackground(Drawable)
	 */
	public Drawable getPinIndicatorBackground() {
		return mIndicatorBackground;
	}

	/**
	 * Set the type of character accepted by this view. Must be one of {@link #INPUT_TYPE_NUMERIC},
	 * {@link #INPUT_TYPE_ALPHA}, or {@link #INPUT_TYPE_ALPHA_NUMERIC}.
	 *
	 * @see #getInputType()
	 * @attr {@link R.styleable#PinCodeView_inputType}
	 */
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

		InputMethodManager imm = getInputMethodManager();
		if (imm != null) imm.restartInput(this);
	}

	/**
	 * Get the type of character accepted by this view. This value should be one of
	 * {@link #INPUT_TYPE_NUMERIC}, {@link #INPUT_TYPE_ALPHA}, or {@link #INPUT_TYPE_ALPHA_NUMERIC}.
	 *
	 * @see #setInputType(int)
	 */
	public int getInputType() {
		return mInputContentInfo.inputType;
	}

	/**
	 * Set the editor type integer associated with this view, which will be reported to an IME with
	 * {@link EditorInfo#imeOptions} when it has focus.
	 *
	 * @see #getImeOptions
	 * @see android.view.inputmethod.EditorInfo
	 * @attr {@link android.R.styleable#TextView_imeOptions}
	 */
	@SuppressLint("InlinedApi")
	public void setImeOptions(int options) {
		// try to prevent the IME from hiding the view in landscape
		options = options | EditorInfo.IME_FLAG_NO_EXTRACT_UI | EditorInfo.IME_FLAG_NO_FULLSCREEN;
		mInputContentInfo.imeOptions = options;
	}

	/**
     * Get the editor type integer associated with this view.
     *
     * @see #setImeOptions(int)
     * @see android.view.inputmethod.EditorInfo
     */
	public int getImeOptions() {
		return mInputContentInfo.imeOptions;
	}

	/**
     * Set the custom IME action associated with this view, which
     * will be reported to an IME with {@link EditorInfo#actionLabel}
     * and {@link EditorInfo#actionId} when it has focus.
     *
     * @see #getImeActionLabel
     * @see #getImeActionId
     * @see android.view.inputmethod.EditorInfo
     * @attr {@link android.R.styleable#TextView_imeActionLabel}
     * @attr {@link android.R.styleable#TextView_imeActionId}
     */
	public void setImeActionLabel(CharSequence label, int actionId) {
		mInputContentInfo.imeActionLabel = label;
		mInputContentInfo.imeActionId = actionId;
	}

	/**
     * Get the IME action label previous set with {@link #setImeActionLabel}.
     *
     * @see #setImeActionLabel
     * @see android.view.inputmethod.EditorInfo
     */
	public CharSequence getImeActionLabel() {
		return mInputContentInfo.imeActionLabel;
	}

	/**
     * Get the IME action ID previous set with {@link #setImeActionLabel}.
     *
     * @see #setImeActionLabel
     * @see android.view.inputmethod.EditorInfo
     */
	public int getImeActionId() {
		return mInputContentInfo.imeActionId;
	}

	/**
	 * Set a special listener to be called when an action is performed on this view. This will be
	 * called when the enter key is pressed, or when an action supplied to the IME is selected by
	 * the user.
	 */
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

	/*package*/ TextView getInputTextView() {
		return mPinText;
	}

	@SuppressLint("InlinedApi")
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
				// An action has not been set, but the enter key will move to
				// the next focus, so set the editor action to that.
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

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	/*package*/ void onEditorAction(int actionId) {
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

	/*package*/ void onPrivateIMECommand(String action, Bundle data) {
		Log.d(TAG, "[onPrivateIMECommand]");
		mPinText.onPrivateIMECommand(action, data);
	}

	/*package*/ KeyListener getKeyListener() {
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
						// No target for next focus, but make sure the IME is
						// hidden
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
			// If there is an action listener, given it a chance to consume the
			// event.
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

	@Override
	public void setOnClickListener(OnClickListener listener) {
		super.setOnClickListener(listener);
		mOnClickListener = listener;
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

	private static class PinIndicator extends ImageView {
		private static final int[] STATE_EMPTY = { android.R.attr.state_empty };
		private static final int[] STATE_ACTIVE = { android.R.attr.state_active };

		private boolean mIsEmpty;
		private boolean mIsActive;

		public PinIndicator(Context context) {
			super(context);
		}

		public PinIndicator(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		public PinIndicator(Context context, AttributeSet attrs, int defStyle) {
			super(context, attrs, defStyle);
		}

		public void setIsEmpty(boolean isEmpty) {
			if (mIsEmpty != isEmpty) {
				mIsEmpty = isEmpty;
				refreshDrawableState();
			}
		}

		public void setIsActive(boolean isActive) {
			if (mIsActive != isActive) {
				mIsActive = isActive;
				refreshDrawableState();
			}
		}

		@Override
		public int[] onCreateDrawableState(int extraSpace) {
			int[] state = super.onCreateDrawableState(extraSpace + 2);
			if (mIsEmpty) {
				mergeDrawableStates(state, STATE_EMPTY);
			}
			if (mIsActive) {
				mergeDrawableStates(state, STATE_ACTIVE);
			}
			return state;
		}
	}
}
