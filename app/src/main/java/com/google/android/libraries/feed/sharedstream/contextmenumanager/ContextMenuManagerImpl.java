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

package com.google.android.libraries.feed.sharedstream.contextmenumanager;

import static com.google.android.libraries.feed.common.Validators.checkNotNull;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.view.Gravity;
import android.view.View;
import android.view.ViewParent;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import com.google.android.libraries.feed.sharedstream.publicapi.menumeasurer.MenuMeasurer;
import com.google.android.libraries.feed.sharedstream.publicapi.menumeasurer.Size;
import java.util.List;
import com.google.android.libraries.feed.R;

/** Implementation of {@link ContextMenuManager}. */
public class ContextMenuManagerImpl implements ContextMenuManager {

  private static final String TAG = "ContextMenuManager";

  // Indicates how deep into the card the menu should be set if the menu is showing below a card. IE
  // 4 indicates that the menu should be placed 1/4 of the way from the edge of the card.
  private static final int FRACTION_FROM_EDGE = 4;

  // Indicates how far from the bottom of the card the menu should be set if the menu is showing
  // above a card.
  private static final int FRACTION_FROM_BOTTOM_EDGE = FRACTION_FROM_EDGE - 1;

  private final MenuMeasurer menuMeasurer;
  private final Context context;
  /*@Nullable*/ private View view;

  /*@Nullable*/ private PopupWindow popupWindow;

  public ContextMenuManagerImpl(MenuMeasurer menuMeasurer, Context context) {
    this.menuMeasurer = menuMeasurer;
    this.context = context;
  }

  /**
   * Sets the root view of the window that the context menu is opening in. This, as well as the
   * anchor view that the click happens on, determines where the context menu opens.
   *
   * <p>Note: this is being changed to be settable after the fact for a future version of this
   * library that will use dependency injection.
   *
   * @param view
   */
  public void setView(View view) {
    this.view = view;
  }

  /**
   * Opens a context menu if there is currently no open context menu. Returns whether a menu was
   * opened.
   *
   * @param anchorView The {@link View} to position the menu by.
   * @param items The contents to display.
   * @param handler The {@link ContextMenuClickHandler} that handles the user clicking on an option.
   */
  public boolean openContextMenu(
      View anchorView, List<String> items, ContextMenuClickHandler handler) {
    if (menuShowing()) {
      return false;
    }

    View localView =
        checkNotNull(this.view, "Must set view before attempting to open context menu");

    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, items);

    ListView listView = createListView(items, context);

    Size measurements =
        menuMeasurer.measureAdapterContent(
            listView, adapter, /* windowPadding= */ 0, getStreamWidth(), getStreamHeight());

    PopupWindowWithDimensions popupWindowWithDimensions =
        createPopupWindow(listView, measurements, context);

    listView.setOnItemClickListener(
        (parent, view, position, id) -> {
          handler.handleClick(position);
          popupWindowWithDimensions.getPopupWindow().dismiss();
        });

    int yOffset =
        getYOffsetForContextMenu(anchorView, popupWindowWithDimensions.getDimensions().getHeight());

    // There is a bug in some versions of android with PopupWindow.showAsDropDown that causes the
    // Popup window to appear off screen. This is fixed by converting the relative offsets from the
    // anchorView to absolute location on the screen so that showAtLocation can be used.
    int absoluteOffsetX =
        convertToAbsoluteHorizontalLocation(anchorView, anchorView.getWidth() / FRACTION_FROM_EDGE);
    int absoluteOffsetY = convertToAbsoluteVerticalLocation(anchorView, yOffset);

    popupWindowWithDimensions
        .getPopupWindow()
        .showAtLocation(localView, Gravity.NO_GRAVITY, absoluteOffsetX, absoluteOffsetY);

    // We want to prevent any more touch events from be used (if a user has yet to end their current
    // touch session). This prevents the possibility of scrolling after the context menu opens.
    ViewParent parent = anchorView.getParent();
    if (parent != null) {
      parent.requestDisallowInterceptTouchEvent(true);
    }

    this.popupWindow = popupWindowWithDimensions.getPopupWindow();
    return true;
  }

  /**
   * Converts the relative horizontal offset from the left of a view to the absolute location on
   * screen.
   */
  private int convertToAbsoluteHorizontalLocation(View anchorView, int xOffsetFromAnchorView) {
    int anchorViewX = getXPosition(anchorView);

    return xOffsetFromAnchorView + anchorViewX;
  }

  /**
   * Converts the relative vertical offset from the bottom of a view to the absolute location on
   * screen.
   */
  private int convertToAbsoluteVerticalLocation(View anchorView, int yOffsetFromAnchorView) {
    int anchorViewY = getYPosition(anchorView);

    return yOffsetFromAnchorView + anchorViewY + anchorView.getHeight();
  }

  /**
   * Gets the offset in pixels from the bottom of the anchorview to place the menu.
   *
   * <p>First, we attempt to place the top of the menu {@code 1/FRACTION_FROM_EDGE} down from the
   * top of the anchor view. If the menu cannot fit there, we attempt to put the bottom of the menu
   * {@code 1/FRACTION_FROM_EDGE} up from the bottom of the anchor view. If the menu can fit in
   * neither position it instead is centered in the stream.
   */
  // TODO: Add tests for each case here.
  private int getYOffsetForContextMenu(
      int menuHeight, int anchorViewYInWindow, int anchorViewHeight, int windowHeight) {
    // Check if the menu can fit below the card.
    if (menuHeight + anchorViewYInWindow + anchorViewHeight / FRACTION_FROM_EDGE < windowHeight) {
      // Check if 1/FRACTION_FROM_EDGE of the way down the card is visible.
      if (-FRACTION_FROM_EDGE * anchorViewYInWindow < anchorViewHeight) {
        // 1/FRACTION_FROM_EDGE of the way down the card is visible stream, so offset to that point,
        // from the bottom of the anchor view.
        return -FRACTION_FROM_BOTTOM_EDGE * anchorViewHeight / FRACTION_FROM_EDGE;
      } else {
        // 1/FRACTION_FROM_EDGE down from the top of the anchor view is not visible, offset the menu
        // to the top of the stream.
        return -1 * (anchorViewHeight + anchorViewYInWindow);
      }
      // Check if the menu can have the bottom 1/FRACTION_FROM_EDGE off from the bottom of the
      // anchor.
    } else if (anchorViewYInWindow - menuHeight + anchorViewHeight / FRACTION_FROM_EDGE >= 0) {
      // Check if 1/FRACTION_FROM_EDGE from the bottom of the anchor is visible.
      if (anchorViewYInWindow + FRACTION_FROM_BOTTOM_EDGE * anchorViewHeight / FRACTION_FROM_EDGE
          < windowHeight) {
        // FRACTION_FROM_BOTTOM_EDGE/FRACTION_FROM_EDGE of the way down the card is visible, so
        // position the bottom there.
        return -(menuHeight + anchorViewHeight / FRACTION_FROM_EDGE);
      } else {
        // Less than the top 1/FRACTION_FROM_EDGE of the card is on the screen. Offset so the menu
        // is at the bottom of the screen
        return -(menuHeight + anchorViewHeight - (windowHeight - anchorViewYInWindow));
      }
    } else {
      // The menu will fit neither above, nor below the content. Center it in the middle of the
      // screen.
      return -(menuHeight + anchorViewYInWindow - windowHeight / 2);
    }
  }

  private int getYOffsetForContextMenu(View anchorView, int menuHeight) {
    int anchorViewY = getYPosition(anchorView);
    int anchorViewYInWindow = anchorViewY - getStreamYPosition();

    return getYOffsetForContextMenu(
        menuHeight, anchorViewYInWindow, anchorView.getHeight(), getStreamHeight());
  }

  private PopupWindowWithDimensions createPopupWindow(
      ListView listView, Size measurements, Context context) {
    // While using elevation to create shadows should work in lollipop+, the shadow was not
    // appearing in versions below Android N, so we are using ninepatch below N.
    if (VERSION.SDK_INT >= VERSION_CODES.N) {
      return createPopupWindowWithElevation(listView, measurements, context);
    } else {
      return createPopupWindowWithNinepatch(listView, measurements, context);
    }
  }

  private PopupWindowWithDimensions createPopupWindowWithElevation(
      ListView listView, Size measurements, Context context) {
    PopupWindow popupWindow =
        new PopupWindow(listView, measurements.getWidth(), measurements.getHeight());
    popupWindow.setFocusable(true);
    popupWindow.setBackgroundDrawable(
        context.getResources().getDrawable(R.drawable.feed_popup_window_bg));
    popupWindow.setElevation(
        context.getResources().getDimension(R.dimen.feed_popup_window_elevation));

    return new PopupWindowWithDimensions(popupWindow, measurements);
  }

  private PopupWindowWithDimensions createPopupWindowWithNinepatch(
      ListView listView, Size measurements, Context context) {
    Drawable shadow = context.getResources().getDrawable(android.R.drawable.picture_frame);

    Rect ninePatchPadding = new Rect();
    shadow.getPadding(ninePatchPadding);

    int widthWithPadding = measurements.getWidth() + ninePatchPadding.left + ninePatchPadding.right;
    int heightWithPadding =
        measurements.getHeight() + ninePatchPadding.top + ninePatchPadding.bottom;

    PopupWindow popupWindow = new PopupWindow(listView, widthWithPadding, heightWithPadding);
    popupWindow.setFocusable(true);
    popupWindow.setBackgroundDrawable(shadow);

    return new PopupWindowWithDimensions(
        popupWindow, new Size(widthWithPadding, heightWithPadding));
  }

  private ListView createListView(List<String> items, Context context) {
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(context, R.layout.feed_simple_list_item, items);

    ListView listView = new ListView(context);
    listView.setAdapter(adapter);
    listView.setBackgroundColor(Color.WHITE);
    listView.setDivider(null);
    listView.setDividerHeight(0);
    return listView;
  }

  /**
   * Gets the height of the Stream. This is specifically the height visible on screen, not including
   * anything below the screen.
   */
  int getStreamHeight() {
    return checkNotNull(view).getHeight();
  }

  /** Gets the width of the Stream. */
  private int getStreamWidth() {
    return checkNotNull(view).getWidth();
  }

  /** Gets the Y coordinate of the position of the Stream. */
  private int getStreamYPosition() {
    return getYPosition(checkNotNull(view));
  }

  private boolean menuShowing() {
    return popupWindow != null && popupWindow.isShowing();
  }

  @Override
  public void dismissPopup() {
    if (popupWindow == null) {
      return;
    }

    popupWindow.dismiss();
    popupWindow = null;
  }

  public void onDimensionsChanged() {
    dismissPopup();
  }

  private int getXPosition(View view) {
    int[] viewLocation = new int[2];
    view.getLocationInWindow(viewLocation);

    return viewLocation[0];
  }

  private int getYPosition(View view) {
    int[] viewLocation = new int[2];
    view.getLocationInWindow(viewLocation);

    return viewLocation[1];
  }

  /** Represents a {@link PopupWindow} that accounts for padding caused by shadows. */
  private static class PopupWindowWithDimensions {

    private final PopupWindow popupWindow;
    private final Size size;

    PopupWindowWithDimensions(PopupWindow popupWindow, Size size) {
      this.popupWindow = popupWindow;
      this.size = size;
    }

    PopupWindow getPopupWindow() {
      return popupWindow;
    }

    Size getDimensions() {
      return size;
    }
  }
}
