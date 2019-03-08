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

package com.google.android.libraries.feed.basicstream.internal;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.helper.ItemTouchHelper;
import com.google.android.libraries.feed.basicstream.internal.viewholders.SwipeableViewHolder;

/** {@link ItemTouchHelper.Callback} to allow dismissing cards via swiping. */
public final class StreamItemTouchCallbacks extends ItemTouchHelper.Callback {

  @Override
  public int getMovementFlags(RecyclerView recyclerView, ViewHolder viewHolder) {
    int swipeFlags = 0;
    if (viewHolder instanceof SwipeableViewHolder
        && ((SwipeableViewHolder) viewHolder).canSwipe()) {
      swipeFlags = ItemTouchHelper.START | ItemTouchHelper.END;
    }
    return makeMovementFlags(/* dragFlags= */ 0, swipeFlags);
  }

  @Override
  public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder, ViewHolder viewHolder1) {
    throw new AssertionError(
        "onMove not supported."); // Drag and drop not supported, the method will never be called.
  }

  @Override
  public void onSwiped(ViewHolder viewHolder, int i) {
    // Only PietViewHolders support swipe to dismiss. If new ViewHolders support swipe to dismiss,
    // this should instead use an interface.
    ((SwipeableViewHolder) viewHolder).onSwiped();
  }
}
