package com.cropper.lib;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

class CropImageView extends ImageViewTouchBase
{
	private float mLastX, mLastY;
	private int mMotionEdge;
	private Context mContext;
	private ScaleGestureDetector mScaleGestureDetector;
	private HighlightView mHiglightView;

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom)
	{
		super.onLayout(changed, left, top, right, bottom);
		if (mBitmapDisplayed.getBitmap() != null)
		{
			if (mHiglightView != null)
			{
				mHiglightView.mMatrix.set(getImageMatrix());
				mHiglightView.invalidate();
				if (mHiglightView.mIsFocused)
				{
					centerBasedOnHighlightView(mHiglightView);
				}
			}
		}
	}

	public CropImageView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		this.mContext = context;
		ScaleGestureDetector.SimpleOnScaleGestureListener mOnScaleGestureListener = new ScaleGestureDetector.SimpleOnScaleGestureListener()
		{
			@Override
			public boolean onScaleBegin(ScaleGestureDetector detector)
			{
				if (mHiglightView != null)
				{
					mHiglightView.setMode(HighlightView.ModifyMode.Grow);
				}
				return true;
			}

			@Override
			public boolean onScale(ScaleGestureDetector detector)
			{
				if (mHiglightView != null)
				{
					int width = mHiglightView.getCropRect().width();
					int height = mHiglightView.getCropRect().height();

					int newWidth = (int) (width * detector.getScaleFactor());
					int newHeight = (int) (height * detector.getScaleFactor());

					int dx = newWidth - width;
					int dy = newHeight - height;

					mHiglightView.growBy(dx, dy);
				}
				return true;
			}

			@Override
			public void onScaleEnd(ScaleGestureDetector detector)
			{
				super.onScaleEnd(detector);
				if (mHiglightView != null)
				{
					mHiglightView.setMode(HighlightView.ModifyMode.None);
					centerBasedOnHighlightView(mHiglightView);
				}
			}
		};
		mScaleGestureDetector = new ScaleGestureDetector(context, mOnScaleGestureListener);
	}

	@Override
	protected void zoomTo(float scale, float centerX, float centerY)
	{
		super.zoomTo(scale, centerX, centerY);

		if (mHiglightView != null)
		{
			mHiglightView.mMatrix.set(getImageMatrix());
			mHiglightView.invalidate();
		}
	}

	@Override
	protected void zoomIn()
	{
		super.zoomIn();

		if (mHiglightView != null)
		{
			mHiglightView.mMatrix.set(getImageMatrix());
			mHiglightView.invalidate();
		}
	}

	@Override
	protected void zoomOut()
	{
		super.zoomOut();

		if (mHiglightView != null)
		{
			mHiglightView.mMatrix.set(getImageMatrix());
			mHiglightView.invalidate();
		}
	}

	@Override
	protected void postTranslate(float deltaX, float deltaY)
	{
		super.postTranslate(deltaX, deltaY);

		if (mHiglightView != null)
		{
			mHiglightView.mMatrix.postTranslate(deltaX, deltaY);
			mHiglightView.invalidate();
		}
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event)
	{
		CropImageActivity cropImage = (CropImageActivity) mContext;
		if (cropImage.mSaving)
		{
			return false;
		}

		mScaleGestureDetector.onTouchEvent(event);

		switch (event.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				int edge = mHiglightView.getHit(event.getX(), event.getY());
				if (edge != HighlightView.GROW_NONE)
				{
					mMotionEdge = edge;
					mLastX = event.getX();
					mLastY = event.getY();
					mHiglightView.setMode(
						(edge == HighlightView.MOVE)
							? HighlightView.ModifyMode.Move
							: HighlightView.ModifyMode.Grow);
					break;
				}
				break;
			case MotionEvent.ACTION_UP:
				centerBasedOnHighlightView(mHiglightView);
				mHiglightView.setMode(
					HighlightView.ModifyMode.None);
				break;
			case MotionEvent.ACTION_MOVE:
				mHiglightView.handleMotion(mMotionEdge,
					event.getX() - mLastX,
					event.getY() - mLastY);
				mLastX = event.getX();
				mLastY = event.getY();

				// This section of code is optional. It has some user
				// benefit in that moving the crop rectangle against
				// the edge of the screen causes scrolling but it means
				// that the crop rectangle is no longer fixed under
				// the user's finger.
				ensureVisible(mHiglightView);
				break;
		}

		switch (event.getAction())
		{
			case MotionEvent.ACTION_UP:
				center(true, true);
				break;

			case MotionEvent.ACTION_MOVE:
				// if we're not zoomed then there's no point in even allowing
				// the user to move the image around.  This call to center puts
				// it back to the normalized location (with false meaning don't
				// animate).
				if (getScale() == 1F)
				{
					center(true, true);
				}
				break;
		}

		return true;
	}

	// Pan the displayed image to make sure the cropping rectangle is visible.
	private void ensureVisible(HighlightView hv)
	{
		Rect r = hv.mDrawRect;

		int panDeltaX1 = Math.max(0, mLeft - r.left);
		int panDeltaX2 = Math.min(0, mRight - r.right);

		int panDeltaY1 = Math.max(0, mTop - r.top);
		int panDeltaY2 = Math.min(0, mBottom - r.bottom);

		int panDeltaX = panDeltaX1 != 0 ? panDeltaX1 : panDeltaX2;
		int panDeltaY = panDeltaY1 != 0 ? panDeltaY1 : panDeltaY2;

		if (panDeltaX != 0 || panDeltaY != 0)
		{
			panBy(panDeltaX, panDeltaY);
		}
	}

	// If the cropping rectangle's size changed significantly, change the
	// view's center and scale according to the cropping rectangle.
	private void centerBasedOnHighlightView(HighlightView hv)
	{
		Rect drawRect = hv.mDrawRect;

		float width = drawRect.width();
		float height = drawRect.height();

		float thisWidth = getWidth();
		float thisHeight = getHeight();

		float z1 = thisWidth / width * .6F;
		float z2 = thisHeight / height * .6F;

		float zoom = Math.min(z1, z2);
		zoom = zoom * this.getScale();
		zoom = Math.max(1F, zoom);
		if ((Math.abs(zoom - getScale()) / zoom) > .1)
		{
			float[] coordinates = new float[]{hv.mCropRect.centerX(),
				hv.mCropRect.centerY()};
			getImageMatrix().mapPoints(coordinates);
			zoomTo(zoom, coordinates[0], coordinates[1], 300F);
		}

		ensureVisible(hv);
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas)
	{
		super.onDraw(canvas);
		if (mHiglightView != null)
		{
			mHiglightView.draw(canvas);
		}
	}

	public void setHighlightView(HighlightView hv)
	{
		mHiglightView = hv;
		invalidate();
	}

	public HighlightView getHiglightView()
	{
		return mHiglightView;
	}
}
