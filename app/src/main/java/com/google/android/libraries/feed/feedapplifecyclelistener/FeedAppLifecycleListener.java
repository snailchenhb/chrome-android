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

package com.google.android.libraries.feed.feedapplifecyclelistener;

import android.database.Observable;
import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.lifecycle.AppLifecycleListener;
import com.google.android.libraries.feed.common.logging.Logger;
import com.google.android.libraries.feed.feedapplifecyclelistener.FeedLifecycleListener.LifecycleEvent;

/** Default implementation of {@link AppLifecycleListener} */
public class FeedAppLifecycleListener extends Observable<FeedLifecycleListener>
    implements AppLifecycleListener {
  private static final String TAG = "FeedAppLifecycleLstnr";

  private final ThreadUtils threadUtils;

  public FeedAppLifecycleListener(ThreadUtils threadUtils) {
    this.threadUtils = threadUtils;
  }

  @Override
  public void onEnterForeground() {
    threadUtils.checkMainThread();
    Logger.i(TAG, "onEnterForeground called");
    dispatchLifecycleEvent(LifecycleEvent.ENTER_FOREGROUND);
  }

  @Override
  public void onEnterBackground() {
    threadUtils.checkMainThread();
    Logger.i(TAG, "onEnterBackground called");
    dispatchLifecycleEvent(LifecycleEvent.ENTER_BACKGROUND);
  }

  @Override
  public void onClearAll() {
    threadUtils.checkMainThread();
    Logger.i(TAG, "onClearAll called");
    dispatchLifecycleEvent(LifecycleEvent.CLEAR_ALL);
  }

  @Override
  public void initialize() {
    threadUtils.checkMainThread();
    Logger.i(TAG, "checkMainThread called");
    dispatchLifecycleEvent(LifecycleEvent.INITIALIZE);
  }

  private void dispatchLifecycleEvent(@LifecycleEvent String event) {
    synchronized (mObservers) {
      for (FeedLifecycleListener listener : mObservers) {
        listener.onLifecycleEvent(event);
      }
    }
  }
}
