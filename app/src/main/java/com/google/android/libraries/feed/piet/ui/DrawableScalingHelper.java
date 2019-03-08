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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ThumbnailUtils;
import com.google.android.libraries.feed.piet.host.AssetProvider;

/** Helper which is able to scale drawables to specified widths/heights. */
public class DrawableScalingHelper {

  /**
   * Scales bitmaps to a more appropriate size if host allows and {@code drawable} is a supported
   * drawable type.
   *
   * @return Drawable which should be used. This could be the same instance as {@code drawable} if
   *     host disabled scaling or {@code drawable} is an unsupported type.
   */
  public static Drawable maybeScaleDrawable(
      Context context, AssetProvider assetProvider, Drawable drawable, int widthPx, int heightPx) {
    if (!assetProvider.shouldPietResizeBitmaps()) {
      // Host is requesting Piet to not resize so just return the drawable back.
      return drawable;
    }

    if (!(drawable instanceof BitmapDrawable)) {
      // Currently can only resize BitmapDrawables.
      return drawable;
    }

    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();

    if (widthPx != bitmap.getWidth() || heightPx != bitmap.getHeight()) {
      // Don't allow ThumbnailUtils to recycle input bitmap.  This bitmap could be own by
      // host and thus we don't want to destroy the instance.
      bitmap = ThumbnailUtils.extractThumbnail(bitmap, widthPx, heightPx, 0);
      drawable = new BitmapDrawable(context.getResources(), bitmap);
    }

    return drawable;
  }
}
