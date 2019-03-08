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

package com.google.android.libraries.feed.piet;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.Callback;
import android.graphics.drawable.TransitionDrawable;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.android.libraries.feed.piet.ui.DrawableScalingHelper;
import com.google.android.libraries.feed.piet.ui.RoundedCornerWrapperView;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles loading images from the host. In particular, handles the resizing of images as well as
 * the fading animation.
 */
public class LoadImageCallback implements Consumer</*@Nullable*/ Drawable> {

  static final int FADE_IN_ANIMATION_TIME_MS = 300;

  private final ImageView imageView;
  private final ScaleType scaleType;
  private final long initialTime;
  private final int widthPx;
  private final int heightPx;
  private final boolean fadeImage;
  private final AdapterParameters parameters;
  private final Context context;
  private final FrameContext frameContext;

  private boolean cancelled;

  @VisibleForTesting /*@Nullable*/ Callback transitionCallback;

  /*@Nullable*/ private Drawable finalDrawable;

  LoadImageCallback(
      ImageView imageView,
      ScaleType scaleType,
      int widthPx,
      int heightPx,
      boolean fadeImage,
      Context context,
      AdapterParameters parameters,
      FrameContext frameContext) {
    this.imageView = imageView;
    this.scaleType = scaleType;
    this.widthPx = widthPx;
    this.heightPx = heightPx;
    this.fadeImage = fadeImage;
    this.parameters = parameters;
    this.context = context;
    this.initialTime = parameters.clock.elapsedRealtime();
    this.frameContext = frameContext;
  }

  @Override
  public void accept(/*@Nullable*/ Drawable drawable) {
    if (cancelled || drawable == null) {
      return;
    }

    // Only perform image rescaling if both width/height are known as parameters.
    if (widthPx > 0 && heightPx > 0) {
      drawable =
          DrawableScalingHelper.maybeScaleDrawable(
              context, parameters.hostProviders.getAssetProvider(), drawable, widthPx, heightPx);
    }
    imageView.setScaleType(scaleType);

    this.finalDrawable = drawable;

    // If we are in the process of binding when we get the image, we should not fade in the
    // image as the image was cached.
    if (!shouldFadeInImage()) {
      imageView.setImageDrawable(drawable);
      // Invalidating the view as the view doesn't update if not manually updated here.
      imageView.invalidate();
      return;
    }

    Drawable initialDrawable =
        imageView.getDrawable() != null
            ? imageView.getDrawable()
            : new ColorDrawable(Color.TRANSPARENT);

    TransitionDrawable transitionDrawable =
        new TransitionDrawable(new Drawable[] {initialDrawable, drawable});
    imageView.setImageDrawable(transitionDrawable);
    transitionDrawable.setCrossFadeEnabled(true);
    transitionDrawable.startTransition(FADE_IN_ANIMATION_TIME_MS);

    // Storing callback so it won't be garbage collected by Drawable which holds it in a
    // WeakReference
    transitionCallback =
        new Callback() {
          @Override
          public void invalidateDrawable(Drawable who) {
            invalidateBelowRoundedCorners();
          }

          @Override
          public void scheduleDrawable(Drawable who, Runnable what, long when) {}

          @Override
          public void unscheduleDrawable(Drawable who, Runnable what) {}
        };
    transitionDrawable.setCallback(transitionCallback);

    imageView.postDelayed(
        () -> {
          if (cancelled) {
            return;
          }

          // Allows GC of the initial drawable and the transition drawable. Additionally
          // fixes the issue where the transition sometimes doesn't occur, which would
          // result in blank images.
          imageView.setImageDrawable(finalDrawable);
          invalidateBelowRoundedCorners();
          transitionCallback = null;
        },
        FADE_IN_ANIMATION_TIME_MS);
  }

  /**
   * s Invalidates all views between the base view and a {@link RoundedCornerWrapperView}, which is
   * required for the animations to succeed. This is necessary as the {@link
   * RoundedCornerWrapperView} masks its image and only redraws when explicitly told it needs to.
   * While invalidating the child view should result in the parent being invalidated, that is not
   * working in this context for some reason.
   */
  private void invalidateBelowRoundedCorners() {
    View view = imageView;
    // Always invalidate the base view.
    view.invalidate();
    List<View> viewsToMaybeInvalidate = new ArrayList<>();
    while (view != null && view != frameContext.getFrameView()) {
      viewsToMaybeInvalidate.add(view);

      if (view instanceof RoundedCornerWrapperView
          && ((RoundedCornerWrapperView) view).hasRoundedCorners()) {
        for (View viewToInvalidate : viewsToMaybeInvalidate) {
          viewToInvalidate.invalidate();
        }
        viewsToMaybeInvalidate.clear();
      }

      view = view.getParent() instanceof View ? (View) view.getParent() : null;
    }
  }

  private boolean shouldFadeInImage() {
    return fadeImage
        && (parameters.clock.elapsedRealtime() - initialTime)
            > parameters.hostProviders.getAssetProvider().getFadeImageThresholdMs();
  }

  void cancel() {
    this.cancelled = true;
    transitionCallback = null;
  }
}
