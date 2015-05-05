/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cropper.lib;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

/**
 * The activity can crop specific region of interest from an image.
 */
public class CropImageActivity extends MonitoredActivity
{
	private final static int IMAGE_MIN_SIZE = 512;
	private static final String TAG = "CropImageActivity";

	// These are various options can be specified in the intent.
	private boolean mCircleCrop = false;
	private boolean mScale;
	// These options specify the output image size and whether we should
	// scale the output to fit it (or just crop it).
	private boolean mScaleUp = true;

	boolean mSaving;  // Whether the "save" button is already clicked.

	private int mAspectX;
	private int mAspectY;
	private int mOutputX;
	private int mOutputY;
	private int highlightColorResId;
	private int highlightSelectedColorResId;
	private int verticalIconResId;
	private int horizontalIconResId;
	private int borderSizeResId;

	private Bitmap.CompressFormat mOutputFormat = Bitmap.CompressFormat.PNG;
	private Uri mSaveUri = null;
	private CropImageView mImageView;
	private ContentResolver mContentResolver;
	private Bitmap mBitmap;

	HighlightView mCrop;

	private final Handler mHandler = new Handler();

	@Override
	public void onCreate(Bundle icicle)
	{
		super.onCreate(icicle);
		mContentResolver = getContentResolver();

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null && extras.containsKey(Cropper.LAYOUT_RES_ID))
		{
			setContentView(extras.getInt(Cropper.LAYOUT_RES_ID));
		}
		else
		{
			setContentView(R.layout.cropimage);
		}

		mImageView = (CropImageView) findViewById(R.id.cropper_image);

		showStorageToast(this);

		if (extras != null)
		{
			if (extras.getString(Cropper.CIRCLE_CROP) != null)
			{
				if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB)
				{
					mImageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
				}

				mCircleCrop = true;
				mAspectX = 1;
				mAspectY = 1;
			}

			if (extras.containsKey(Cropper.OUTPUT_X))
			{
				mOutputX = extras.getInt(Cropper.OUTPUT_X);
			}
			else
			{
				mOutputX = IMAGE_MIN_SIZE;
			}

			if (extras.containsKey(Cropper.OUTPUT_Y))
			{
				mOutputY = extras.getInt(Cropper.OUTPUT_Y);
			}
			else
			{
				mOutputY = IMAGE_MIN_SIZE;
			}

			if (extras.containsKey(Cropper.CROP_AREA_HIGHLIGHT_COLOR_RES_ID))
			{
				highlightColorResId = extras.getInt(Cropper.CROP_AREA_HIGHLIGHT_COLOR_RES_ID);
			}
			else
			{
				highlightColorResId = android.R.color.white;
			}

			if (extras.containsKey(Cropper.CROP_AREA_HIGHLIGHT_SELECTED_COLOR_RES_ID))
			{
				highlightSelectedColorResId = extras.getInt(Cropper.CROP_AREA_HIGHLIGHT_SELECTED_COLOR_RES_ID);
			}
			else
			{
				highlightSelectedColorResId = R.color.green;
			}

			if (extras.containsKey(Cropper.CROP_AREA_HORIZONTAL_ICON_RES_ID))
			{
				horizontalIconResId = extras.getInt(Cropper.CROP_AREA_HORIZONTAL_ICON_RES_ID);
			}
			else
			{
				horizontalIconResId = R.drawable.circle;
			}

			if (extras.containsKey(Cropper.CROP_AREA_VERTICAL_ICON_RES_ID))
			{
				verticalIconResId = extras.getInt(Cropper.CROP_AREA_VERTICAL_ICON_RES_ID);
			}
			else
			{
				verticalIconResId = R.drawable.circle;
			}

			if (extras.containsKey(Cropper.CROP_AREA_BORDER_SIZE_DIMEN_RES_ID))
			{
				borderSizeResId = extras.getInt(Cropper.CROP_AREA_BORDER_SIZE_DIMEN_RES_ID);
			}
			else
			{
				borderSizeResId = R.dimen.border_size;
			}

			Log.i(TAG, "Output X " + mOutputX + ", output Y " + mOutputY);

			mScale = extras.getBoolean(Cropper.SCALE, true);
			mScaleUp = extras.getBoolean(Cropper.SCALE_UP_IF_NEEDED, true);

			String uri = extras.getString(Cropper.IMAGE_PATH);
			if (TextUtils.isEmpty(uri))
			{
				throw new IllegalArgumentException(Cropper.IMAGE_PATH + " argument MUST NOT be NULL");
			}

			String saveUri = extras.getString(Cropper.SAVE_PATH);
			if (TextUtils.isEmpty(saveUri))
			{
				throw new IllegalArgumentException(Cropper.SAVE_PATH + " argument MUST NOT be NULL");
			}
			mSaveUri = Uri.parse(saveUri);

			mBitmap = getBitmap(Uri.parse(uri));

			if (mOutputX > mOutputY)
			{
				mAspectX = (int) Math.floor((float) mOutputX / (float) mOutputY);
				mAspectY = 1;
			}
			else
			{
				mAspectY = (int) Math.floor((float) mOutputY / (float) mOutputX);
				mAspectX = 1;
			}

			if (extras.containsKey(Cropper.ASPECT_X))
			{
				mAspectX = extras.getInt(Cropper.ASPECT_X);
			}

			if (extras.containsKey(Cropper.ASPECT_Y))
			{
				mAspectY = extras.getInt(Cropper.ASPECT_Y);
			}

			Log.i(TAG, "Aspect X " + mAspectX + ", aspect Y " + mAspectY);
		}

		if (mBitmap == null)
		{
			finish();
			return;
		}

		// Make UI fullscreen.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		findViewById(R.id.cropper_discard).setOnClickListener(
			new View.OnClickListener()
			{
				public void onClick(View v)
				{
					setResult(RESULT_CANCELED);
					finish();
				}
			});

		findViewById(R.id.cropper_save).setOnClickListener(
			new View.OnClickListener()
			{
				public void onClick(View v)
				{
					try
					{
						onSaveClicked();
					}
					catch (Exception e)
					{
						finish();
					}
				}
			});
		findViewById(R.id.croppper_rotate_left).setOnClickListener(
			new View.OnClickListener()
			{
				public void onClick(View v)
				{
					mBitmap = Util.rotateImage(mBitmap, -90);
					RotateBitmap rotateBitmap = new RotateBitmap(mBitmap);
					mImageView.setImageRotateBitmapResetBase(rotateBitmap, true);
					mSetupHighlightRunnable.run();
				}
			});

		findViewById(R.id.cropper_rotate_right).setOnClickListener(
			new View.OnClickListener()
			{
				public void onClick(View v)
				{
					mBitmap = Util.rotateImage(mBitmap, 90);
					RotateBitmap rotateBitmap = new RotateBitmap(mBitmap);
					mImageView.setImageRotateBitmapResetBase(rotateBitmap, true);
					mSetupHighlightRunnable.run();
				}
			});
		startFaceDetection();
	}

	private Bitmap getBitmap(Uri uri)
	{
		InputStream in;
		try
		{
			in = mContentResolver.openInputStream(uri);

			//Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;

			BitmapFactory.decodeStream(in, null, o);
			in.close();

			int scale = 1;
			int size = Math.max(mOutputX, mOutputY);
//			size = size < IMAGE_MIN_SIZE ? IMAGE_MIN_SIZE : size;
			if (o.outHeight > size || o.outWidth > size)
			{
				scale = (int) Math.pow(2, (int) Math.round(Math.log(IMAGE_MIN_SIZE / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
			}

			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			o2.inScaled = false;
			in = mContentResolver.openInputStream(uri);
			Bitmap b = BitmapFactory.decodeStream(in, null, o2);
			in.close();

			return b;
		}
		catch (IOException e)
		{
			Log.e(TAG, "file " + uri.toString() + " not found");
		}
		return null;
	}

	private void startFaceDetection()
	{
		if (isFinishing())
		{
			return;
		}

		mImageView.setImageBitmapResetBase(mBitmap, true);

		Util.startBackgroundJob(this,
			new Runnable()
			{
				public void run()
				{
					final CountDownLatch latch = new CountDownLatch(1);
					final Bitmap b = mBitmap;
					mHandler.post(new Runnable()
					{
						public void run()
						{
							if (b != mBitmap && b != null)
							{
								mImageView.setImageBitmapResetBase(b, true);
								mBitmap = null;
								mBitmap = b;
							}
							if (mImageView.getScale() == 1F)
							{
								mImageView.center(true, true);
							}
							latch.countDown();
						}
					});
					try
					{
						latch.await();
					}
					catch (InterruptedException e)
					{
						throw new RuntimeException(e);
					}
					mSetupHighlightRunnable.run();
				}
			}, mHandler);
	}

	private void onSaveClicked() throws Exception
	{
		// TODO this code needs to change to use the decode/crop/encode single
		// step api so that we don't require that the whole (possibly large)
		// bitmap doesn't have to be read into memory
		if (mSaving) return;

		if (mCrop == null)
		{
			return;
		}

		mSaving = true;

		Rect r = mCrop.getCropRect();

		int width = r.width();
		int height = r.height();

		Log.i(TAG, "Rect width/height " + width + "/" + height);

		// If we are circle cropping, we want alpha channel, which is the
		// third param here.
		Bitmap croppedImage;
		try
		{
			croppedImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		}
		catch (Exception e)
		{
			throw e;
		}
		if (croppedImage == null)
		{
			return;
		}

		Canvas canvas = new Canvas(croppedImage);
		Rect dstRect = new Rect(0, 0, width, height);
		canvas.drawBitmap(mBitmap, r, dstRect, null);

		if (mCircleCrop)
		{
			// OK, so what's all this about?
			// Bitmaps are inherently rectangular but we want to return
			// something that's basically a circle.  So we fill in the
			// area around the circle with alpha.  Note the all important
			// PortDuff.Mode.CLEAR.
			Canvas c = new Canvas(croppedImage);
			Path p = new Path();
			p.addCircle(width / 2F, height / 2F, width / 2F,
				Path.Direction.CW);
			c.clipPath(p, Region.Op.DIFFERENCE);
			c.drawColor(0x00000000, PorterDuff.Mode.CLEAR);
		}

		/* If the output is required to a specific size then scale or fill */
		if (mOutputX != 0 && mOutputY != 0)
		{
			if (mScale)
			{
			    /* Scale the image to the required dimensions */
				croppedImage = Util.transform(new Matrix(), croppedImage, mOutputX, mOutputY, mScaleUp);
			}
			else
			{

				/* Don't scale the image crop it to the size requested.
				 * Create an new image with the cropped image in the center and
				 * the extra space filled.
				 */

				// Don't scale the image but instead fill it so it's the
				// required dimension
				Bitmap b = Bitmap.createBitmap(mOutputX, mOutputY, Bitmap.Config.ARGB_8888);
				canvas = new Canvas(b);

				Rect srcRect = mCrop.getCropRect();
				dstRect = new Rect(0, 0, mOutputX, mOutputY);

				int dx = (srcRect.width() - dstRect.width()) / 2;
				int dy = (srcRect.height() - dstRect.height()) / 2;

				/* If the srcRect is too big, use the center part of it. */
				srcRect.inset(Math.max(0, dx), Math.max(0, dy));

				/* If the dstRect is too big, use the center part of it. */
				dstRect.inset(Math.max(0, -dx), Math.max(0, -dy));

				/* Draw the cropped bitmap in the center */
				canvas.drawBitmap(mBitmap, srcRect, dstRect, null);

				/* Set the cropped bitmap as the new bitmap */
				croppedImage = b;
			}
		}

		final Bitmap b = croppedImage;
		Util.startBackgroundJob(this,
			new Runnable()
			{
				public void run()
				{
					saveOutput(b);
				}
			}, mHandler);
	}

	private void saveOutput(Bitmap croppedImage)
	{
		if (mSaveUri != null)
		{
			OutputStream outputStream = null;

			try
			{
				outputStream = mContentResolver.openOutputStream(mSaveUri);
				if (outputStream != null)
				{
					croppedImage.compress(mOutputFormat, 90, outputStream);
				}
			}
			catch (IOException ex)
			{

				Log.e(TAG, "Cannot open file: " + mSaveUri, ex);
				setResult(RESULT_CANCELED);
				finish();
				return;
			}
			finally
			{

				Util.closeSilently(outputStream);
			}

			Bundle extras = new Bundle();

			Intent intent = new Intent();
			intent.putExtras(extras);
			intent.putExtra(Cropper.SAVE_PATH, mSaveUri.toString());
			intent.putExtra(Cropper.ORIENTATION_IN_DEGREES, Util.getOrientationInDegree(this));
			setResult(RESULT_OK, intent);
		}
		else
		{

			Log.e(TAG, "not defined image url");
		}
		croppedImage = null;
		finish();
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		mBitmap = null;
	}

	Runnable mSetupHighlightRunnable = new Runnable()
	{
		@SuppressWarnings("hiding")
		float mScale = 1F;
		Matrix mImageMatrix;

		// Create a default HightlightView
		private void makeDefault()
		{
			HighlightView hv = new HighlightView(mImageView);

			int width = mBitmap.getWidth();
			int height = mBitmap.getHeight();

			Rect imageRect = new Rect(0, 0, width, height);

			// make the default size about 4/5 of the width or height
			int cropWidth = Math.min(width, height) * 4 / 5;
			int cropHeight = cropWidth;

			if (mAspectX != 0 && mAspectY != 0)
			{

				if (mAspectX > mAspectY)
				{

					cropHeight = cropWidth * mAspectY / mAspectX;
				}
				else
				{

					cropWidth = cropHeight * mAspectX / mAspectY;
				}
			}

			int x = (width - cropWidth) / 2;
			int y = (height - cropHeight) / 2;

			RectF cropRect = new RectF(x, y, x + cropWidth, y + cropHeight);
			hv.setup(mImageMatrix, imageRect, cropRect, mCircleCrop,
				mAspectX != 0 && mAspectY != 0, highlightColorResId, highlightSelectedColorResId,
				verticalIconResId, horizontalIconResId, borderSizeResId);

			mImageView.setHighlightView(hv);
		}

		public void run()
		{
			mImageMatrix = mImageView.getImageMatrix();
			mScale = 1.0F / mScale;

			mHandler.post(new Runnable()
			{
				public void run()
				{
					makeDefault();

					mImageView.invalidate();
					mCrop = mImageView.getHiglightView();
					mCrop.setFocus(true);
				}
			});
		}
	};

	public static final int NO_STORAGE_ERROR = -1;
	public static final int CANNOT_STAT_ERROR = -2;

	public static void showStorageToast(Activity activity)
	{
		showStorageToast(activity, calculatePicturesRemaining(activity));
	}

	public static void showStorageToast(Activity activity, int remaining)
	{
		String noStorageText = null;

		if (remaining == NO_STORAGE_ERROR)
		{
			String state = Environment.getExternalStorageState();
			if (state.equals(Environment.MEDIA_CHECKING))
			{
				noStorageText = activity.getString(R.string.preparing_card);
			}
			else
			{
				noStorageText = activity.getString(R.string.no_storage_card);
			}
		}
		else if (remaining < 1)
		{
			noStorageText = activity.getString(R.string.not_enough_space);
		}

		if (noStorageText != null)
		{
			Toast.makeText(activity, noStorageText, Toast.LENGTH_LONG).show();
		}
	}

	public static int calculatePicturesRemaining(Activity activity)
	{
		try
		{
			String storageDirectory;
			String state = Environment.getExternalStorageState();
			if (Environment.MEDIA_MOUNTED.equals(state))
			{
				storageDirectory = Environment.getExternalStorageDirectory().toString();
			}
			else
			{
				storageDirectory = activity.getFilesDir().toString();
			}
			StatFs stat = new StatFs(storageDirectory);
			float remaining = ((float) stat.getAvailableBlocks()
				* (float) stat.getBlockSize()) / 400000F;
			return (int) remaining;
		}
		catch (Exception ex)
		{
			// if we can't stat the filesystem then we don't know how many
			// pictures are remaining.  it might be zero but just leave it
			// blank since we really don't know.
			return CANNOT_STAT_ERROR;
		}
	}
}
