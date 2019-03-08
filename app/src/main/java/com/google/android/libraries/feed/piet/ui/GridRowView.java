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
import android.support.annotation.VisibleForTesting;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.google.android.libraries.feed.common.logging.Logger;

/** LinearLayout with special measuring code for GridRow */
public class GridRowView extends LinearLayout {
  private static final String TAG = "GridRowView";

  // TODO: call to setOrientation(int) not allowed on the given receiver.
  @SuppressWarnings("nullness:method.invocation.invalid")
  public GridRowView(Context context) {
    super(context);
    super.setOrientation(HORIZONTAL);
  }

  @Override
  public void setOrientation(int orientation) {
    if (orientation != HORIZONTAL) {
      throw new IllegalArgumentException("GridRowView can only be horizontal");
    }
  }

  @Override
  protected LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.0f, false);
  }

  @Override
  protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
    if (lp instanceof LayoutParams) {
      return new LayoutParams((LayoutParams) lp);
    } else if (lp instanceof LinearLayout.LayoutParams) {
      return new LayoutParams((LinearLayout.LayoutParams) lp);
    }
    return new LayoutParams(lp);
  }

  @Override
  protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof LayoutParams;
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (!hasCollapsibleCells()
        || MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED
        || MeasureSpec.getSize(widthMeasureSpec) == 0) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
      return;
    }

    // Here's how much space we actually have; the GridRowView will fill it.
    // (This will not be 0/UNSPECIFIED because that is handled above)
    int maxWidth = MeasureSpec.getSize(widthMeasureSpec);

    int gridRowVerticalPadding = getPaddingTop() + getPaddingBottom();
    int gridRowHorizontalPadding = getPaddingLeft() + getPaddingRight();

    // Max height for child cells; either AT_MOST or UNSPECIFIED
    int maxHeightSpec;
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);
    if (heightMode == MeasureSpec.UNSPECIFIED) {
      maxHeightSpec = heightMeasureSpec;
    } else {
      maxHeightSpec =
          MeasureSpec.makeMeasureSpec(heightSize - gridRowVerticalPadding, MeasureSpec.AT_MOST);
    }

    int unlimitedSpace = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

    // See how much space the fixed-size (WRAP_CONTENT and width) cells take.
    // Ask how much space they would fill if there were no constraints.
    // Also add up the total weight for distribution among the weight cells later.
    int totalReservedWidth = 0;
    float totalWeight = 0;
    for (int i = 0; i < getChildCount(); i++) {
      View view = getChildAt(i);
      LayoutParams params = ((LayoutParams) view.getLayoutParams());
      if (params.weight == 0
          && (params.width > 0 || params.width == LayoutParams.WRAP_CONTENT)
          && !params.isCollapsible) {
        if (params.width == LayoutParams.WRAP_CONTENT) {
          view.measure(unlimitedSpace, getMaxHeightSpecForView(params, maxHeightSpec));
        } else {
          view.measure(
              MeasureSpec.makeMeasureSpec(params.width, MeasureSpec.EXACTLY),
              getMaxHeightSpecForView(params, maxHeightSpec));
        }
        totalReservedWidth += view.getMeasuredWidth();
      } else if (params.weight > 0) {
        totalWeight += params.weight;
      }
      totalReservedWidth += params.getMarginStart() + params.getMarginEnd();
    }
    totalReservedWidth += gridRowHorizontalPadding;

    // Calculate width remaining to the collapsible cell(s)
    int availableWidth = Math.max(maxWidth - totalReservedWidth, 0);

    // Find collapsible cells and assign them the available width.
    // The first cell will get as much width as it needs (or as much as is available), and future
    // cells will get whatever's left.
    for (int i = 0; i < getChildCount(); i++) {
      View view = getChildAt(i);
      LayoutParams params = ((LayoutParams) view.getLayoutParams());
      if (!params.isCollapsible) {
        continue;
      }
      int viewMaxHeight = getMaxHeightSpecForView(params, maxHeightSpec);
      // TODO: Handle margins on collapsible cells
      int desiredWidth;
      if (availableWidth == 0) {
        // Shortcut for when we run out of room.
        desiredWidth = 0;
      } else if (params.width == LayoutParams.WRAP_CONTENT) {
        view.measure(unlimitedSpace, viewMaxHeight);
        desiredWidth = view.getMeasuredWidth();
      } else {
        desiredWidth = params.width;
      }
      view.measure(
          MeasureSpec.makeMeasureSpec(Math.min(availableWidth, desiredWidth), MeasureSpec.EXACTLY),
          viewMaxHeight);

      availableWidth = Math.max(0, availableWidth - view.getMeasuredWidth());
    }

    // Distribute remaining width among the weight cells.
    if (totalWeight > 0) {
      for (int i = 0; i < getChildCount(); i++) {
        View view = getChildAt(i);
        LayoutParams params = ((LayoutParams) view.getLayoutParams());
        if (params.weight > 0) {
          view.measure(
              MeasureSpec.makeMeasureSpec(
                  (int) (availableWidth * params.weight / totalWeight), MeasureSpec.EXACTLY),
              getMaxHeightSpecForView(params, maxHeightSpec));
        }
      }
    }

    // Truncate remaining cells if we ran out of room.
    availableWidth = maxWidth;
    for (int i = 0; i < getChildCount(); i++) {
      View view = getChildAt(i);
      if (view.getMeasuredWidth() > availableWidth) {
        view.measure(
            MeasureSpec.makeMeasureSpec(availableWidth, MeasureSpec.EXACTLY), maxHeightSpec);
      }
      availableWidth = Math.max(0, availableWidth - view.getMeasuredWidth());
    }

    // Set height of the GridRowView based on the max cell height
    int rowHeight = Integer.MAX_VALUE;
    if (heightMode == MeasureSpec.EXACTLY) {
      rowHeight = heightSize;
    } else {
      if (heightMode == MeasureSpec.AT_MOST) {
        rowHeight = MeasureSpec.getSize(heightMeasureSpec);
      }
      int maxCellHeight = 0;
      for (int i = 0; i < getChildCount(); i++) {
        View view = getChildAt(i);
        if (view.getMeasuredHeight() > maxCellHeight) {
          maxCellHeight = view.getMeasuredHeight();
        }
      }
      // We should not need to resize the cells because they were all already smaller than this.
      rowHeight = Math.min(rowHeight, maxCellHeight + gridRowVerticalPadding);
    }

    setMeasuredDimension(maxWidth, rowHeight);
  }

  /** Find either the height specified by LayoutParams, or the height constrained by the parent. */
  private int getMaxHeightSpecForView(LayoutParams viewLayoutParams, int parentMaxHeightSpec) {
    if (viewLayoutParams.height > 0) {
      if (MeasureSpec.getMode(parentMaxHeightSpec) == MeasureSpec.UNSPECIFIED
          || MeasureSpec.getSize(parentMaxHeightSpec) > viewLayoutParams.height) {
        return MeasureSpec.makeMeasureSpec(viewLayoutParams.height, MeasureSpec.EXACTLY);
      }
      return parentMaxHeightSpec;
    } else {
      return parentMaxHeightSpec;
    }
  }

  @VisibleForTesting
  public boolean hasCollapsibleCells() {
    for (int i = 0; i < getChildCount(); i++) {
      View view = getChildAt(i);
      if (((LayoutParams) view.getLayoutParams()).isCollapsible) {
        return true;
      }
    }
    return false;
  }

  /**
   * LayoutParams for GridRowViews. Essentially just LinearLayout.LayoutParams with a boolean to
   * keep track of whether this cell isCollapsible. Collapsible cells behave as WRAP_CONTENT when
   * they have sufficient room, but shrink when there is not enough room for them. Collapsible cells
   * take priority over weight cells; a weight cell will shrink to zero before the collapsible cell
   * starts to shrink.
   */
  public static class LayoutParams extends LinearLayout.LayoutParams {
    private final boolean isCollapsible;

    public LayoutParams(LayoutParams source) {
      super(source);
      isCollapsible = source.getIsCollapsible();
    }

    public LayoutParams(int width, int height, boolean isCollapsible) {
      this(width, height, 0.0f, isCollapsible);
    }

    /**
     * If a cell is collapsible, it doesn't make sense for it to also have weight (since all weight
     * cells are by definition collapsible, and have no intrinsic width).
     */
    public LayoutParams(int width, int height, float weight, boolean isCollapsible) {
      super(width, height, weight);
      if (isCollapsible && weight > 0) {
        isCollapsible = false;
      }
      if (width == MATCH_PARENT) {
        Logger.wtf(TAG, "GridRowView cells cannot have width MATCH_PARENT.");
      }
      this.isCollapsible = isCollapsible;
    }

    public LayoutParams(Context c, AttributeSet attrs) {
      super(c, attrs);
      isCollapsible = false;
    }

    public LayoutParams(int width, int height) {
      super(width, height);
      isCollapsible = false;
    }

    public LayoutParams(int width, int height, float weight) {
      super(width, height, weight);
      isCollapsible = false;
    }

    public LayoutParams(ViewGroup.LayoutParams p) {
      super(p);
      isCollapsible = false;
    }

    public LayoutParams(MarginLayoutParams source) {
      super(source);
      isCollapsible = false;
    }

    public LayoutParams(LinearLayout.LayoutParams source) {
      super((MarginLayoutParams) source);
      weight = source.weight;
      gravity = source.gravity;
      isCollapsible = false;
    }

    public boolean getIsCollapsible() {
      return isCollapsible;
    }
  }
}
