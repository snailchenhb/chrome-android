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

import static com.google.android.libraries.feed.common.Validators.checkState;
import static com.google.android.libraries.feed.host.imageloader.ImageLoaderApi.DIMENSION_UNKNOWN;

import android.content.Context;
import android.support.annotation.VisibleForTesting;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import com.google.android.libraries.feed.common.ui.LayoutUtils;
import com.google.android.libraries.feed.piet.AdapterFactory.SingletonKeySupplier;
import com.google.android.libraries.feed.piet.ui.AspectRatioScalingImageView;
import com.google.search.now.ui.piet.ElementsProto.BindingValue;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ImageElement;
import com.google.search.now.ui.piet.ElementsProto.Visibility;
import com.google.search.now.ui.piet.ErrorsProto.ErrorCode;
import com.google.search.now.ui.piet.ImagesProto.Image;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;

/** An {@link ElementAdapter} for {@code ImageElement} elements. */
class ImageElementAdapter extends ElementAdapter<AspectRatioScalingImageView, ImageElement> {
  private static final String TAG = "ImageElementAdapter";

  /*@Nullable*/ private LoadImageCallback currentlyLoadingImage = null;

  @VisibleForTesting
  ImageElementAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters, createView(context), KeySupplier.SINGLETON_KEY);
  }

  @Override
  ImageElement getModelFromElement(Element baseElement) {
    if (!baseElement.hasImageElement()) {
      throw new PietFatalException(
          ErrorCode.ERR_MISSING_ELEMENT_CONTENTS,
          String.format("Missing ImageElement; has %s", baseElement.getElementsCase()));
    }
    return baseElement.getImageElement();
  }

  @Override
  void onCreateAdapter(ImageElement model, Element baseElement, FrameContext frameContext) {
    StyleProvider style = getElementStyle();

    if (style.hasWidth() || style.hasHeight()) {
      Context context = getContext();

      if (style.hasWidth()) {
        widthPx = (int) LayoutUtils.dpToPx(style.getWidth(), context);
      } else {
        widthPx = DIMENSION_NOT_SET;
      }

      if (style.hasHeight()) {
        heightPx = (int) LayoutUtils.dpToPx(style.getHeight(), context);
      } else {
        // Defaults to a square when only the width is defined.
        heightPx = style.hasWidth() ? widthPx : DIMENSION_NOT_SET;
      }
    } else {
      widthPx = DIMENSION_NOT_SET;
      heightPx = DIMENSION_NOT_SET;
    }
  }

  @Override
  void onBindModel(ImageElement model, Element baseElement, FrameContext frameContext) {
    Image image;
    switch (model.getContentCase()) {
      case IMAGE:
        image = model.getImage();
        break;
      case IMAGE_BINDING:
        BindingValue binding = frameContext.getImageBindingValue(model.getImageBinding());
        if (!binding.hasImage()) {
          if (model.getImageBinding().getIsOptional()) {
            setVisibilityOnView(Visibility.GONE);
            return;
          } else {
            throw new PietFatalException(
                ErrorCode.ERR_MISSING_OR_UNHANDLED_CONTENT,
                String.format("Image binding %s had no content", binding.getBindingId()));
          }
        }
        image = binding.getImage();
        break;
      default:
        throw new PietFatalException(
            ErrorCode.ERR_MISSING_OR_UNHANDLED_CONTENT,
            String.format(
                "Unsupported or missing content in ImageElement: %s", model.getContentCase()));
    }

    if (getElementStyle().hasPreLoadFill()) {
      getBaseView().setImageDrawable(getElementStyle().createPreLoadFill());
    }

    image = frameContext.filterImageSourcesByMediaQueryCondition(image);

    checkState(currentlyLoadingImage == null, "An image loading callback exists; unbind first");
    // TODO: Remove the fallback for Image.getScaleType()
    Image.ScaleType scaleType =
        image.hasScaleType() ? image.getScaleType() : getElementStyle().getScaleType();
    LoadImageCallback loadImageCallback =
        createLoadImageCallback(toAndroidScaleType(scaleType), frameContext);
    currentlyLoadingImage = loadImageCallback;
    getParameters()
        .hostProviders
        .getAssetProvider()
        .getImage(
            image,
            convertDimensionForImageLoader(widthPx),
            convertDimensionForImageLoader(heightPx),
            loadImageCallback);
  }

  private int convertDimensionForImageLoader(int dimension) {
    if (dimension == DIMENSION_NOT_SET) {
      return DIMENSION_UNKNOWN;
    }

    return dimension;
  }

  @SuppressWarnings("UnnecessaryDefaultInEnumSwitch")
  private static ScaleType toAndroidScaleType(Image.ScaleType pietScaleType) {
    switch (pietScaleType) {
      case CENTER_CROP:
        return ScaleType.CENTER_CROP;
      case CENTER_INSIDE:
      case SCALE_TYPE_UNSPECIFIED:
        return ScaleType.FIT_CENTER;
      default:
        throw new PietFatalException(
            ErrorCode.ERR_MISSING_OR_UNHANDLED_CONTENT,
            String.format("Unsupported ScaleType: %s", pietScaleType));
    }
  }

  @Override
  void onUnbindModel() {
    if (currentlyLoadingImage != null) {
      currentlyLoadingImage.cancel();
      currentlyLoadingImage = null;
    }
    ImageView imageView = getBaseView();
    if (imageView != null) {
      imageView.setImageDrawable(null);
    }
  }

  @Override
  StyleIdsStack getElementStyleIdsStack() {
    return getModel().getStyleReferences();
  }

  @VisibleForTesting
  LoadImageCallback createLoadImageCallback(ScaleType scaleType, FrameContext frameContext) {
    return new LoadImageCallback(
        getBaseView(),
        scaleType,
        widthPx,
        heightPx,
        getElementStyle().getFadeInImageOnLoad(),
        getContext(),
        getParameters(),
        frameContext);
  }

  private static AspectRatioScalingImageView createView(Context context) {
    AspectRatioScalingImageView imageView = new AspectRatioScalingImageView(context);
    imageView.setCropToPadding(true);
    return imageView;
  }

  static class KeySupplier extends SingletonKeySupplier<ImageElementAdapter, ImageElement> {
    @Override
    public String getAdapterTag() {
      return TAG;
    }

    @Override
    public ImageElementAdapter getAdapter(Context context, AdapterParameters parameters) {
      return new ImageElementAdapter(context, parameters);
    }
  }
}
