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

package com.google.android.libraries.feed.piet.host;

import android.graphics.drawable.Drawable;
import com.google.android.libraries.feed.common.functional.Consumer;
import com.google.search.now.ui.piet.ImagesProto.Image;

/** Provide Assets from the host */
public interface AssetProvider {
  /** Constant used when an image's height or width is not known. */
  int DIMENSION_UNKNOWN = -1;

  /**
   * Given an {@link Image}, asynchronously load the {@link Drawable} and return via a {@link
   * Consumer}.
   *
   * <p>The width and the height of the image can be provided preemptively, however it is not
   * guaranteed that both dimensions will be known. In the case that only one dimension is known,
   * the host should be careful to preserve the aspect ratio.
   *
   * @param image The image to load.
   * @param widthPx The width of the {@link Image} in pixels. Will be {@link #DIMENSION_UNKNOWN} if
   *     unknown.
   * @param heightPx The height of the {@link Image} in pixels. Will be {@link #DIMENSION_UNKNOWN}
   *     if unknown.
   * @param consumer Callback to return the {@link Drawable} from an {@link Image} if the load
   *     succeeds. {@literal null} should be passed to this if no source succeeds in loading the
   *     image
   */
  void getImage(Image image, int widthPx, int heightPx, Consumer</*@Nullable*/ Drawable> consumer);

  /**
   * Determines if Piet should try to resize BitmapDrawables retrieved via {@link #getImage(Image,
   * Consumer)} to the drawn size. Hosts should return {@literal false} if they perform their own in
   * memory bitmap cache.
   */
  boolean shouldPietResizeBitmaps();

  /** Return a relative elapsed time string such as "8 minutes ago" or "1 day ago". */
  String getRelativeElapsedString(long elapsedTimeMillis);

  /** Returns the default corner rounding radius in pixels. */
  int getDefaultCornerRadius();

  /** Returns whether the theme for the Piet rendering context is a "dark theme". */
  boolean isDarkTheme();

  /**
   * Fade-in animation will only occur if image loading time takes more than this amount of time.
   */
  int getFadeImageThresholdMs();
}
