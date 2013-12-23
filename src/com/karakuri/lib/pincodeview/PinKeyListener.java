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

import android.annotation.SuppressLint;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.method.QwertyKeyListener;
import android.text.method.TextKeyListener.Capitalize;
import android.view.inputmethod.EditorInfo;

public class PinKeyListener extends QwertyKeyListener implements InputFilter {
	private static final String TAG = "PinKeyListener";

	@SuppressLint("InlinedApi")
	private static final int INPUT_TYPE_TEXT = EditorInfo.TYPE_CLASS_TEXT
			| EditorInfo.TYPE_TEXT_VARIATION_PASSWORD | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
	@SuppressLint("InlinedApi")
	private static final int INPUT_TYPE_NUMBER = EditorInfo.TYPE_CLASS_NUMBER
			| EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD;
	private static final PinKeyListener[] sInstance = new PinKeyListener[3];

	private Type mType;

	public enum Type {
		NUMERIC {
			@Override
			boolean acceptChar(char c) {
				return Character.isDigit(c);
			}

			@Override
			int getInputType() {
				return INPUT_TYPE_NUMBER;
			}
		},
		ALPHA {
			@Override
			boolean acceptChar(char c) {
				return Character.isLetter(c);
			}

			@Override
			int getInputType() {
				return INPUT_TYPE_TEXT;
			}
		},
		ALPHA_NUMERIC {
			@Override
			boolean acceptChar(char c) {
				return Character.isLetterOrDigit(c);
			}

			@Override
			int getInputType() {
				return INPUT_TYPE_TEXT;
			}
		};

		abstract boolean acceptChar(char c);

		abstract int getInputType();
	}

	public PinKeyListener(Type type) {
		super(Capitalize.NONE, false);
		mType = type;
	}

	public static PinKeyListener getInstance(Type type) {
		int index = type.ordinal();
		if (sInstance[index] == null) {
			sInstance[index] = new PinKeyListener(type);
		}
		return sInstance[index];
	}

	@Override
	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart,
			int dend) {
		final int length = source.length();
		if (length == 0) {
			return source;
		}

		StringBuilder filtered = new StringBuilder(source);
		for (int i = length - 1; i >= 0; i--) {
			if (!mType.acceptChar(filtered.charAt(i))) {
				filtered.deleteCharAt(i);
			}
		}
		return filtered;
	}

	@Override
	public int getInputType() {
		return mType.getInputType();
	}
}
