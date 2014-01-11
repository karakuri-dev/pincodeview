/*
 * Copyright (C) 2013 Karakuri <karakuri.dev@gmail.com>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.karakuri.lib.pincodeview;

import android.os.Bundle;
import android.text.Editable;
import android.text.method.KeyListener;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.widget.TextView;

public class PinCodeInputConnection extends BaseInputConnection {
	private static final String TAG = "PinCodeInputConnection";

	private final PinCodeView mTargetView;
	private final TextView mTextView;

	public PinCodeInputConnection(PinCodeView targetView) {
		super(targetView, true);
		mTargetView = targetView;
		mTextView = targetView.getInputTextView();
	}

	public Editable getEditable() {
		if (mTextView != null) {
			return mTextView.getEditableText();
		}
		return null;
	}

	public boolean beginBatchEdit() {
		mTextView.beginBatchEdit();
		return true;
	}

	public boolean endBatchEdit() {
		mTextView.endBatchEdit();
		return true;
	}

	public boolean clearMetaKeyStates(int states) {
		final Editable content = getEditable();
		if (content == null) return false;
		KeyListener kl = mTargetView.getKeyListener();
		if (kl != null) {
			try {
				kl.clearMetaKeyState(mTargetView, content, states);
			} catch (AbstractMethodError e) {
				// This is an old listener that doesn't implement the
				// new method.
			}
		}
		return true;
	}

	public boolean commitCompletion(CompletionInfo text) {
		mTextView.beginBatchEdit();
		mTextView.onCommitCompletion(text);
		mTextView.endBatchEdit();
		return true;
	}

	public boolean performEditorAction(int actionCode) {
		mTargetView.onEditorAction(actionCode);
		return true;
	}

	public boolean performContextMenuAction(int id) {
		mTextView.beginBatchEdit();
		mTextView.onTextContextMenuItem(id);
		mTextView.endBatchEdit();
		return true;
	}

	public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
		return null;
	}

	public boolean performPrivateCommand(String action, Bundle data) {
		mTargetView.onPrivateIMECommand(action, data);
		return true;
	}

	@Override
	public boolean commitText(CharSequence text, int newCursorPosition) {
		if (mTextView == null) {
			return super.commitText(text, newCursorPosition);
		}

		CharSequence errorBefore = mTextView.getError();
		boolean success = super.commitText(text, newCursorPosition);
		CharSequence errorAfter = mTextView.getError();

		if (errorAfter != null && errorBefore == errorAfter) {
			mTextView.setError(null, null);
		}

		return success;
	}
}