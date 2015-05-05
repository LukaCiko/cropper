/*
 * Copyright 2015 Luka Cindro
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cropper.lib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/**
 * Image cropping library for Android.
 */
@SuppressWarnings("UnusedDeclaration")
public final class Cropper
{
	/**
	 * Request code for {@code pick}.
	 */
	public static final int PICK = 23001;
	/**
	 * Request code for {@code crop}.
	 */
	public static final int CROP = 23002;
	/**
	 * Name of the cropped bitmap file path string extra return after {@code crop}.
	 */
	public static final String SAVE_PATH = "save-path";

	static final String IMAGE_PATH = "image-path";
	static final String SCALE = "scale";
	static final String ORIENTATION_IN_DEGREES = "orientation_in_degrees";
	static final String ASPECT_X = "aspectX";
	static final String ASPECT_Y = "aspectY";
	static final String OUTPUT_X = "outputX";
	static final String OUTPUT_Y = "outputY";
	static final String SCALE_UP_IF_NEEDED = "scaleUpIfNeeded";
	static final String CIRCLE_CROP = "circleCrop";
	static final String LAYOUT_RES_ID = "layoutResourceId";
	static final String CROP_AREA_HIGHLIGHT_COLOR_RES_ID = "highlightColor";
	static final String CROP_AREA_HIGHLIGHT_SELECTED_COLOR_RES_ID = "highlightSelectedColor";
	static final String CROP_AREA_VERTICAL_ICON_RES_ID = "verticalIcon";
	static final String CROP_AREA_HORIZONTAL_ICON_RES_ID = "horizontalIcon";
	static final String CROP_AREA_BORDER_SIZE_DIMEN_RES_ID = "borderSize";

	private Cropper()
	{
		// Hiding constructor
	}

	/**
	 * Launch an activity to pick an image. The result is returned with request code {@code PICK}.
	 *
	 * @param activity Activity that will get the result.
	 */
	public static void pick(Activity activity)
	{
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		activity.startActivityForResult(intent, PICK);
	}

	/**
	 * Launch an activity to pick an image. The result is returned with request code {@code PICK}.
	 *
	 * @param context     Application or other context.
	 * @param imageSource URI of the image being cropped.
	 * @param savePath    URI used for saving the cropped image.
	 */
	public static Builder crop(Context context, Uri imageSource, Uri savePath)
	{
		return new Builder(context, imageSource, savePath);
	}

	/**
	 * An builder providing fluent API to specify additional configuration. Use {@code crop} to
	 * get an instance of it.
	 */
	public static class Builder
	{
		private Intent intent;

		Builder(Context context, Uri imageSource, Uri savePath)
		{
			this.intent = new Intent(context, CropImageActivity.class);
			this.intent.putExtra(IMAGE_PATH, imageSource.toString());
			this.intent.putExtra(SAVE_PATH, savePath.toString());
		}

		public Builder aspectRatio(int aspectX, int aspectY)
		{
			this.intent.putExtra(ASPECT_X, aspectX);
			this.intent.putExtra(ASPECT_Y, aspectY);
			return this;
		}

		public Builder outputSize(int width, int height)
		{
			this.intent.putExtra(OUTPUT_X, width);
			this.intent.putExtra(OUTPUT_Y, height);
			return this;
		}

		public Builder scale(boolean scale)
		{
			this.intent.putExtra(SCALE, scale);
			return this;
		}

		public Builder circleCrop(boolean circleCrop)
		{
			this.intent.putExtra(CIRCLE_CROP, circleCrop);
			return this;
		}

		public Builder layoutResourceId(int id)
		{
			this.intent.putExtra(LAYOUT_RES_ID, id);
			return this;
		}

		public Builder cropAreaHighlightColorResId(int id)
		{
			this.intent.putExtra(CROP_AREA_HIGHLIGHT_COLOR_RES_ID, id);
			return this;
		}

		public Builder cropAreaHighlightSelectedColorResId(int id)
		{
			this.intent.putExtra(CROP_AREA_HIGHLIGHT_SELECTED_COLOR_RES_ID, id);
			return this;
		}

		public Builder cropAreaVerticalIconResId(int id)
		{
			this.intent.putExtra(CROP_AREA_VERTICAL_ICON_RES_ID, id);
			return this;
		}

		public Builder cropAreaHorizontalIconResId(int id)
		{
			this.intent.putExtra(CROP_AREA_HORIZONTAL_ICON_RES_ID, id);
			return this;
		}

		public Builder cropAreaBorderSize(int id)
		{
			this.intent.putExtra(CROP_AREA_BORDER_SIZE_DIMEN_RES_ID, id);
			return this;
		}

		/**
		 * Start the image cropping activity.
		 *
		 * @param activity Activity that will get the result.
		 */
		public void start(Activity activity)
		{
			activity.startActivityForResult(intent, CROP);
		}
	}
}
