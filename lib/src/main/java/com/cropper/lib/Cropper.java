package com.cropper.lib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class Cropper
{
	public static final int PICK = 1001;
	public static final int CROP = 1002;

	public static final String IMAGE_PATH = "image-path";
	public static final String SAVE_PATH = "save-path";
	public static final String SCALE = "scale";
	public static final String ORIENTATION_IN_DEGREES = "orientation_in_degrees";
	public static final String ASPECT_X = "aspectX";
	public static final String ASPECT_Y = "aspectY";
	public static final String OUTPUT_X = "outputX";
	public static final String OUTPUT_Y = "outputY";
	public static final String SCALE_UP_IF_NEEDED = "scaleUpIfNeeded";
	public static final String CIRCLE_CROP = "circleCrop";
	public static final String LAYOUT_RES_ID = "layoutResourceId";
	public static final String CROP_AREA_HIGHTLIGHT_COLOR_RES_ID = "highlightColor";
	public static final String CROP_AREA_HIGHLIGHT_SELECTED_COLOR_RES_ID = "highlightSelectedColor";
	public static final String CROP_AREA_VERTICAL_ICON_RES_ID = "verticalIcon";
	public static final String CROP_AREA_HORIZONTAL_ICON_RES_ID = "horizontalIcon";
	public static final String CROP_AREA_BORDER_SIZE_DIMEN_RES_ID = "borderSize";

	private Cropper()
	{

	}

	public static void pick(Activity activity)
	{
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		activity.startActivityForResult(intent, PICK);
	}

	public static Builder crop(Context context, Uri imageSource, Uri savePath)
	{
		return new Builder(context, imageSource, savePath);
	}

	public static class Builder
	{
		private Intent intent;

		public Builder(Context context, Uri imageSource, Uri savePath)
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
			this.intent.putExtra(CROP_AREA_HIGHTLIGHT_COLOR_RES_ID, id);
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

		public void start(Activity activity)
		{
			activity.startActivityForResult(intent, CROP);
		}
	}
}

