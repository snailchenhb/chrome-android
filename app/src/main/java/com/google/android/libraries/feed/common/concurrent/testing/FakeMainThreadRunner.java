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

package com.google.android.libraries.feed.common.concurrent.testing;

import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import java.util.ArrayList;
import java.util.List;

/** {@link MainThreadRunner} used for tests which is able to queue up tasks. */
public class FakeMainThreadRunner extends MainThreadRunner {

  private final boolean runTasksImmediately;
  private final List<Runnable> tasks = new ArrayList<>();
  private final List<Runnable> completedTasks = new ArrayList<>();

  public static FakeMainThreadRunner runTasksImmediately() {
    return new FakeMainThreadRunner(true);
  }

  public static FakeMainThreadRunner queueAllTasks() {
    return new FakeMainThreadRunner(false);
  }

  FakeMainThreadRunner(boolean runTasksImmediately) {
    this.runTasksImmediately = runTasksImmediately;
  }

  @Override
  public void execute(String name, Runnable runnable) {
    tasks.add(runnable);

    if (runTasksImmediately) {
      runAllTasks();
    }
  }

  public void runAllTasks() {
    for (Runnable task : tasks) {
      task.run();
      completedTasks.add(task);
    }
    tasks.clear();
  }

  public boolean hasTasks() {
    return !tasks.isEmpty();
  }

  public int getCompletedTaskCount() {
    return completedTasks.size();
  }
}
