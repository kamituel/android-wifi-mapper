package pl.kamituel.wifimapper.views;

import java.util.List;

import pl.kamituel.wifimapper.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class DotSquareView extends SurfaceView 
implements SurfaceView.OnTouchListener, SurfaceHolder.Callback {
	private final static String TAG = DotSquareView.class.getCanonicalName();
	
	private float mDotRadius;
	private int mDotRingColor;

	private SelfState mState;
	
	private Circle[] mCircles;
	private int mDimension;

	public DotSquareView(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.DotSquareView);
		mDotRadius = styledAttrs.getFraction(R.styleable.DotSquareView_dot_radius, 1, 1, 
				context.getResources().getFraction(R.fraction.DotSquareView_dot_radius_default, 1, 1));
		mDotRingColor = styledAttrs.getColor(R.styleable.DotSquareView_dot_ring_color, 
				context.getResources().getColor(R.color.DotSquareView_dot_ring_color_default));

		init();
	}

	private void init() {
		setZOrderOnTop(true);
		getHolder().setFormat(PixelFormat.TRANSPARENT);
		getHolder().addCallback(this);
		setWillNotDraw(false);
		setOnTouchListener(this);
	}
	
	public void setDots(int dimension, List<Dot> dots) {
		if (dimension * dimension != dots.size()) {
			Log.e(TAG, "Invalid dimension " + dimension + " for list of size " + dots.size());
		}
		
		mDimension = dimension;
		mCircles = new Circle[dots.size()];
		for (int d = 0; d < dots.size(); d += 1) {
			mCircles[d] = new Circle(dots.get(d), dimension, d);
		}
		
		if (mState != null) {
			recalculateState(mState.width, mState.height);
		}
		postInvalidate();
	}
	
	public Dot getSelectedDot() {		
		for (int c = 0; c < mCircles.length; c += 1) {
			if (mCircles[c].isSelected()) {
				return mCircles[c].getDot();
			}
		}
		
		return null;
	}
	
	private class Circle {
		private Dot mDot;
		private int mDimension;
		private int mPosition;
		private boolean mSelected;
		
		public Circle(Dot dot, int dimension, int position) {
			mDot = dot;
			mDimension = dimension;
			mPosition = position;
			mSelected = false;
		}
		
		public boolean isSelected() {
			return mSelected;
		}
		
		public void setSelected(boolean selected) {
			mSelected = selected;
		}
		
		public float getCenterX() {
			float width = mState.dot_ring.right - mState.dot_ring.left;
			float columnCirclesHeight = mDimension * width;
			float columnSpacing = (mState.height - columnCirclesHeight) / (mDimension - 1);

			return getColumn() * (columnSpacing + width) + width / 2;
		}
		
		public float getCenterY() {
			float height = mState.dot_ring.bottom - mState.dot_ring.top;
			float rowCirclesWidth = mDimension * height;
			float rowSpacing = (mState.width - rowCirclesWidth) / (mDimension - 1);

			return getRow() * (rowSpacing + height) + height / 2;
		}
		
		public float distanceTo(float x, float y) {
			float dx = x - getCenterX();
			float dy = y - getCenterY();
			return (float) Math.sqrt(dx * dx + dy * dy);
		}
		
		public int getRow() {
			return mPosition / mDimension;
		}
		
		public int getColumn() {
			return mPosition % mDimension;
		}
		
		public Dot getDot() {
			return mDot;
		}
	}
	

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_UP:
			float minDistance = Float.MAX_VALUE;
			int closestCircleId = -1;
			
			for (int c = 0; c < mCircles.length; c += 1) {				
				float distance = mCircles[c].distanceTo(event.getX(), event.getY());
				Log.d("xxx", "distance for " + mCircles[c].getRow()+","+mCircles[c].getColumn() + "(" + c + ") is " + distance);
				if (distance < minDistance) {
					minDistance = distance;
					closestCircleId = c;
				}
			}
			
			for (int c = 0; c < mCircles.length; c += 1) {
				Circle circle = mCircles[c];
				if (c != closestCircleId) {
					circle.setSelected(false);
				} else {
					circle.setSelected(!circle.isSelected());
				}
			}
			postInvalidate();
			break;
		}

		return true;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		Paint backgroundPaint = new Paint();
		backgroundPaint.setColor(Color.BLACK);
		backgroundPaint.setStyle(Paint.Style.FILL);
		canvas.drawRect(new RectF(0, 0, mState.width, mState.height), backgroundPaint);

		if (mCircles == null) {
			return;
		}
		
		for (int c = 0; c < mCircles.length; c += 1) {
			canvas.save();
			canvas.translate(mCircles[c].getCenterX(), mCircles[c].getCenterY());
		
			Paint labelPaint;
			Paint dotRingPaint;
			RectF dotRingRect;
			
			if (mCircles[c].isSelected()) {
				dotRingPaint = mState.dot_ring_paint_selected;
				dotRingRect = mState.dot_ring_selected;
				labelPaint = mState.label_text_selected;
			} else {
				if ("0".equals(mCircles[c].getDot().getLabel())) {
					dotRingPaint = mState.dot_ring_empty_paint;
				} else {
					dotRingPaint = mState.dot_ring_paint;
				}
				dotRingRect = mState.dot_ring;
				labelPaint = mState.label_text;
			}
			
			canvas.drawOval(mState.dot_ring, dotRingPaint);
			
			String label = mCircles[c].getDot().getLabel();
			float labelWidth = mState.label_text.measureText(label);
			canvas.drawText(label, -labelWidth / 2, mState.label_text.getTextSize() / 2, labelPaint);
			
			canvas.restore();
		}
	}
	
	private class SelfState {
		int width;
		int height;

		int center_x;
		int center_y;

		RectF dot_ring;
		RectF dot_ring_selected;
		Paint dot_ring_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Paint dot_ring_empty_paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		Paint dot_ring_paint_selected = new Paint(Paint.ANTI_ALIAS_FLAG);
		
		Paint label_text = new Paint(Paint.ANTI_ALIAS_FLAG);
		Paint label_text_selected = new Paint(Paint.ANTI_ALIAS_FLAG);
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		recalculateState(width, height);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d("xxx", "surfaceCreated(holder)");
		//new AnimateThread().start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}
	
	private void recalculateState(int width, int height) {
		mState = new SelfState();

		mState.width = width;
		mState.height = height;

		mState.center_x = width / 2;
		mState.center_y = height / 2;

		float dot_w = mDotRadius * width;
		float dot_h = mDotRadius * height;
		
		//if (dot_w * mDimension > width) {
			dot_w = (int) (width / mDimension);
			dot_h = (int) (height / mDimension);
		//}
		
		mState.dot_ring = new RectF(-dot_w / 2, -dot_h / 2, dot_w / 2, dot_h / 2);
		
		mState.dot_ring_empty_paint.setStyle(Paint.Style.STROKE);
		mState.dot_ring_empty_paint.setStrokeWidth(dot_w / 12);
		// TODO: move color to attrs as dot_ring_color 
		mState.dot_ring_empty_paint.setColor(Color.RED);

		mState.dot_ring_paint.setStyle(Paint.Style.STROKE);
		mState.dot_ring_paint.setStrokeWidth(dot_w / 12);
		// TODO: move color to attrs as dot_ring_color 
		mState.dot_ring_paint.setColor(Color.GREEN);
		
		float dot_selection_w = dot_w * 2;
		float dot_selection_h = dot_h * 2;
		mState.dot_ring_selected = new RectF(-dot_selection_w / 2, -dot_selection_h / 2, 
				dot_selection_w /2, dot_selection_h / 2);

		mState.dot_ring_paint_selected.setStyle(Paint.Style.FILL);
		mState.dot_ring_paint_selected.setStrokeWidth(dot_w / 5);
		// TODO: move color to attrs as dot_ring_color 
		mState.dot_ring_paint_selected.setColor(Color.RED);
		
		mState.label_text.setStrokeWidth(1);
		mState.label_text.setColor(Color.RED);
		mState.label_text.setTextSize(mDotRadius / 2 * width);
		
		mState.label_text_selected.setStrokeWidth(1);
		mState.label_text_selected.setColor(Color.WHITE);
		mState.label_text_selected.setTextSize(mDotRadius / 2 * width);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (widthMeasureSpec > heightMeasureSpec) {
			super.onMeasure(heightMeasureSpec, heightMeasureSpec);
		} else {
			super.onMeasure(widthMeasureSpec, widthMeasureSpec);
		}
	}


	/*private class AnimateThread extends Thread {
		private boolean mIsRunning = true;
		private boolean mRepaint = true;

		public void run () {
			int i = 0;
			Canvas canvas = null;
			Log.d("xxx", "AnimateThread.run()" + mIsRunning);
			while (mIsRunning) {
				try {
					if ( i++ > 30*1000/20 || mRepaint ) {
						i = 0;
						canvas = getHolder().lockCanvas();
						synchronized(getHolder()) {
							//mRepaint = false;
							postInvalidate();
						}
						Log.d("xxx", "postInvalidate()");
					}
					Thread.sleep(20);
				} catch (InterruptedException e) {
					Log.e("xxx", "", e);
				} finally {
					getHolder().unlockCanvasAndPost(canvas);
				}
			}
			Log.d("xxx", "animateThread.stop()");
		}
	}*/
}
