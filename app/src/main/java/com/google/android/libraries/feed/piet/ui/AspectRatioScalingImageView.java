// Copyright 2018 The Feed Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.android.libraries.feed.piet.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

/** {@link ImageView} which always aspect-ratio scales the image to fit its container. */
public class AspectRatioScalingImageView extends ImageView {

  public AspectRatioScalingImageView(Context context) {
    super(context);
  }

  /**
   * This custom onMeasure scales the image to fill the container. If the container has only one
   * constrained dimension, the image is aspect ratio scaled to its max possible size given the
   * constraining dimension.
   *
   * <p>This is overridden because adjustViewBounds does not scale up small images in API 17-, and
   * because we want the image to scale independent of the dimensions of the Drawable - the image
   * should not change size based on the resolution of the Drawable, only the aspect ratio.
   */
  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    // If we don't have any drawable, there's nothing special we can do.
    Drawable drawable = getDrawable();
    if (drawable == null
        || drawable.getIntrinsicWidth() == -1
        || drawable.getIntrinsicHeight() == -1) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      return;
    }

    int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
    int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

    int drawableWidth = drawable.getIntrinsicWidth();
    int drawableHeight = drawable.getIntrinsicHeight();

    int measuredWidth = MeasureSpec.getSize(widthMeasureSpec);
    int measuredHeight = MeasureSpec.getSize(heightMeasureSpec);

    if (widthSpecMode == MeasureSpec.UNSPECIFIED && heightSpecMode != MeasureSpec.UNSPECIFIED) {
      // Aspect ratio scale the width
      measuredWidth = aspectRatioScaleWidth(measuredHeight, drawableWidth, drawableHeight);
    } else if (heightSpecMode == MeasureSpec.UNSPECIFIED
        && widthSpecMode != MeasureSpec.UNSPECIFIED) {
      // Aspect ratio scale the height
      measuredHeight = aspectRatioScaleHeight(measuredWidth, drawableWidth, drawableHeight);
    } else if (heightSpecMode == MeasureSpec.UNSPECIFIED
        && widthSpecMode == MeasureSpec.UNSPECIFIED) {
      // If both are UNSPECIFIED, take up as much room as possible.
      measuredWidth = Integer.MAX_VALUE;
      measuredHeight = Integer.MAX_VALUE;
    } else if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.EXACTLY) {
      measuredWidth =
          Math.min(
              aspectRatioScaleWidth(measuredHeight, drawableWidth, drawableHeight), measuredWidth);
    } else if (heightSpecMode == MeasureSpec.AT_MOST && widthSpecMode == MeasureSpec.EXACTLY) {
      measuredHeight =
          Math.min(
              aspectRatioScaleHeight(measuredWidth, drawableWidth, drawableHeight), measuredHeight);
    } else if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
      int desiredWidth = aspectRatioScaleWidth(measuredHeight, drawableWidth, drawableHeight);
      int desiredHeight = aspectRatioScaleHeight(measuredWidth, drawableWidth, drawableHeight);
      if (desiredWidth < measuredWidth) {
        measuredWidth = desiredWidth;
      } else if (desiredHeight < measuredHeight) {
        measuredHeight = desiredHeight;
      }
    }
    // else keep values from the MeasureSpec because both modes are EXACTLY.

    setMeasuredDimension(measuredWidth, measuredHeight);
  }

  private int aspectRatioScaleWidth(int constrainingHeight, int drawableWidth, int drawableHeight) {
    int imageHeight = constrainingHeight - getPaddingTop() - getPaddingBottom();
    int imageWidth = imageHeight * drawableWidth / drawableHeight;
    return imageWidth + getPaddingRight() + getPaddingLeft();
  }

  private int aspectRatioScaleHeight(int constrainingWidth, int drawableWidth, int drawableHeight) {
    int imageWidth = constrainingWidth - getPaddingRight() - getPaddingLeft();
    int imageHeight = imageWidth * drawableHeight / drawableWidth;
    return imageHeight + getPaddingTop() + getPaddingBottom();
  }
}
