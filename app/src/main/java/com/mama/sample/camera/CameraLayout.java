package com.mama.sample.camera;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class CameraLayout extends FrameLayout {

    private int mPreviewWidth;
    private int mPreviewHeight;

    public CameraLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraLayout(Context context) {
        super(context);
    }

    public void requestLayoutFromSize(int previewWidth, int previewHeight) {
        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mPreviewWidth == 0 || mPreviewHeight == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        int originalWidth = MeasureSpec.getSize(widthMeasureSpec);
        int originalHeight = MeasureSpec.getSize(heightMeasureSpec);

        int finalWidth = originalWidth;
        int finalHeight = finalWidth * mPreviewWidth / mPreviewHeight;
        setMeasuredDimension(finalWidth, finalHeight);
        measureChildren(MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY));
    }

}