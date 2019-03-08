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
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Space;
import com.google.android.libraries.feed.piet.AdapterFactory.SingletonKeySupplier;
import com.google.search.now.ui.piet.ElementsProto.Content;
import com.google.search.now.ui.piet.ElementsProto.Element;
import com.google.search.now.ui.piet.ElementsProto.ElementList;
import com.google.search.now.ui.piet.ErrorsProto.ErrorCode;
import com.google.search.now.ui.piet.StylesProto.StyleIdsStack;
import java.util.List;

/** An {@link ElementContainerAdapter} which manages vertical lists of elements. */
class ElementListAdapter extends ElementContainerAdapter<LinearLayout, ElementList> {
  private static final String TAG = "ElementListAdapter";

  private ElementListAdapter(Context context, AdapterParameters parameters) {
    super(context, parameters, createView(context), KeySupplier.SINGLETON_KEY);
  }

  @Override
  ElementList getModelFromElement(Element baseElement) {
    if (!baseElement.hasElementList()) {
      throw new PietFatalException(
          ErrorCode.ERR_MISSING_ELEMENT_CONTENTS,
          String.format("Missing ElementList; has %s", baseElement.getElementsCase()));
    }
    return baseElement.getElementList();
  }

  @Override
  void onCreateAdapter(ElementList model, Element baseElement, FrameContext frameContext) {
    super.onCreateAdapter(model, baseElement, frameContext);
    setupGravityViews();
  }

  @Override
  void onBindModel(ElementList model, Element baseElement, FrameContext frameContext) {
    super.onBindModel(model, baseElement, frameContext);
    for (ElementAdapter<? extends View, ?> adapter : childAdapters) {
      updateChildLayoutParams(adapter);
    }
  }

  @Override
  List<Content> getContentsFromModel(ElementList model) {
    return model.getContentsList();
  }

  @Override
  StyleIdsStack getElementStyleIdsStack() {
    return getModel().getStyleReferences();
  }

  @Override
  void onUnbindModel() {
    ViewUtils.clearOnClickActions(getView());
    super.onUnbindModel();
  }

  @Override
  public void setLayoutParams(ViewGroup.LayoutParams layoutParams) {
    super.setLayoutParams(layoutParams);
    for (ElementAdapter<? extends View, ?> adapter : childAdapters) {
      updateChildLayoutParams(adapter);
    }
  }

  /**
   * Creates spacer views on top and/or bottom to make gravity work in a GridCell. These will be
   * destroyed on releaseAdapter.
   */
  void setupGravityViews() {
    LinearLayout listView = getBaseView();
    // Based on gravity, we may need to add empty cells above or below the content that fill the
    // parent cell, to ensure that backgrounds and actions apply to the entire cell.
    View topView;
    View bottomView;
    switch (getVerticalGravity()) {
      case GRAVITY_BOTTOM:
        topView = new Space(getContext());
        listView.addView(topView, 0);
        ((LayoutParams) topView.getLayoutParams()).weight = 1.0f;
        setFirstAdapterViewOffset(1);
        break;
      case GRAVITY_MIDDLE:
        topView = new Space(getContext());
        listView.addView(topView, 0);
        ((LayoutParams) topView.getLayoutParams()).weight = 1.0f;
        bottomView = new Space(getContext());
        listView.addView(bottomView);
        ((LayoutParams) bottomView.getLayoutParams()).weight = 1.0f;
        setFirstAdapterViewOffset(1);
        break;
      case GRAVITY_TOP:
        bottomView = new Space(getContext());
        listView.addView(bottomView);
        ((LayoutParams) bottomView.getLayoutParams()).weight = 1.0f;
        break;
      default:
        // do nothing
    }
  }

  private void updateChildLayoutParams(ElementAdapter<? extends View, ?> adapter) {
    LayoutParams childParams = new LayoutParams(0, 0);

    setChildWidthAndHeightOnParams(adapter, childParams);

    adapter.getElementStyle().applyMargins(getContext(), childParams);

    childParams.gravity = ViewUtils.gravityHorizontalToGravity(adapter.getHorizontalGravity());

    adapter.setLayoutParams(childParams);
  }

  @VisibleForTesting
  static LinearLayout createView(Context context) {
    LinearLayout viewGroup = new LinearLayout(context);
    viewGroup.setOrientation(LinearLayout.VERTICAL);
    viewGroup.setLayoutParams(
        new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    viewGroup.setClipToPadding(false);
    return viewGroup;
  }

  static class KeySupplier extends SingletonKeySupplier<ElementListAdapter, ElementList> {
    @Override
    public String getAdapterTag() {
      return TAG;
    }

    @Override
    public ElementListAdapter getAdapter(Context context, AdapterParameters parameters) {
      return new ElementListAdapter(context, parameters);
    }
  }
}
