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

package com.google.android.libraries.feed.common.testing;

import com.google.android.libraries.feed.api.common.ThreadUtils;
import com.google.android.libraries.feed.api.lifecycle.AppLifecycleListener;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.api.protocoladapter.ProtocolAdapter;
import com.google.android.libraries.feed.api.sessionmanager.SessionManager;
import com.google.android.libraries.feed.common.concurrent.MainThreadRunner;
import com.google.android.libraries.feed.common.concurrent.TaskQueue;
import com.google.android.libraries.feed.common.protoextensions.FeedExtensionRegistry;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.SystemClockImpl;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.feedapplifecyclelistener.FeedAppLifecycleListener;
import com.google.android.libraries.feed.feedmodelprovider.FeedModelProviderFactory;
import com.google.android.libraries.feed.feedprotocoladapter.FeedProtocolAdapter;
import com.google.android.libraries.feed.feedsessionmanager.FeedSessionManager;
import com.google.android.libraries.feed.feedsessionmanager.FeedSessionManagerFactory;
import com.google.android.libraries.feed.feedstore.FeedStore;
import com.google.android.libraries.feed.host.config.Configuration;
import com.google.android.libraries.feed.host.config.Configuration.ConfigKey;
import com.google.android.libraries.feed.host.proto.ProtoExtensionProvider;
import com.google.android.libraries.feed.host.scheduler.SchedulerApi;
import com.google.android.libraries.feed.host.storage.ContentStorageDirect;
import com.google.android.libraries.feed.host.storage.JournalStorageDirect;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryContentStorage;
import com.google.android.libraries.feed.hostimpl.storage.InMemoryJournalStorage;
import com.google.protobuf.GeneratedMessageLite.GeneratedExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This is a Scope type object which is used in the Infrastructure Integration tests. It sets the
 * Feed objects from ProtocolAdapter through the SessionManager.
 */
public class InfrastructureIntegrationScope {
  private static final boolean USE_TIMEOUT_SCHEDULER = true;
  private static final long TIMEOUT_STORIES_ARE_CURRENT = 2;
  private static final long TIMEOUT_STORIES_CURRENT_WITH_REFRESH = 4;
  private static final long TIMEOUT_TIMEOUT_MS = 2;

  /**
   * For the TimeoutSession tests, this is how long we allow it to run before declaring a timeout
   * error.
   */
  public static final long TIMEOUT_TEST_TIMEOUT = TimeUnit.SECONDS.toMillis(20);

  /** Scope which disables making requests when $HEAD is empty. */
  private static final SchedulerApi DISABLED_EMPTY_HEAD_REQUEST =
      new SchedulerApi() {

        @Override
        @RequestBehavior
        public int shouldSessionRequestData(SessionManagerState sessionManagerState) {
          return RequestBehavior.NO_REQUEST_WITH_CONTENT;
        }

        @Override
        public void onReceiveNewContent(long contentCreationDateTimeMs) {
          // Do nothing
        }

        @Override
        public void onRequestError(int networkResponseCode) {
          // Do nothing
        }
      };

  private final FeedSessionManager feedSessionManager;
  private final FeedProtocolAdapter feedProtocolAdapter;
  private final FeedModelProviderFactory modelProviderFactory;
  private final FakeRequestManager fakeRequestManager;
  private final FeedAppLifecycleListener appLifecycleListener;
  private final TaskQueue taskQueue;
  private final FeedStore store;

  private InfrastructureIntegrationScope(
      ThreadUtils threadUtils,
      ExecutorService executorService,
      SchedulerApi schedulerApi,
      Clock clock,
      Configuration configuration,
      long requestDelayMs) {
    TimingUtils timingUtils = new TimingUtils();
    MainThreadRunner mainThreadRunner = new MainThreadRunner();
    appLifecycleListener = new FeedAppLifecycleListener(threadUtils);

    FeedExtensionRegistry extensionRegistry = new FeedExtensionRegistry(new ExtensionProvider());
    ContentStorageDirect contentStorage = new InMemoryContentStorage();
    JournalStorageDirect journalStorage = new InMemoryJournalStorage();
    taskQueue = new TaskQueue(executorService, clock, false);
    store =
        new FeedStore(
            timingUtils,
            extensionRegistry,
            contentStorage,
            journalStorage,
            threadUtils,
            taskQueue,
            clock);
    feedProtocolAdapter = new FeedProtocolAdapter(timingUtils);
    fakeRequestManager = new FakeRequestManager(feedProtocolAdapter, requestDelayMs);
    feedSessionManager =
        new FeedSessionManagerFactory(
                taskQueue,
                store,
                timingUtils,
                threadUtils,
                feedProtocolAdapter,
                fakeRequestManager,
                schedulerApi,
                configuration,
                clock,
                appLifecycleListener)
            .create();
    feedSessionManager.initialize();
    modelProviderFactory =
        new FeedModelProviderFactory(
            feedSessionManager,
            threadUtils,
            timingUtils,
            taskQueue,
            mainThreadRunner,
            configuration);
  }

  public ProtocolAdapter getProtocolAdapter() {
    return feedProtocolAdapter;
  }

  public SessionManager getSessionManager() {
    return feedSessionManager;
  }

  public ModelProviderFactory getModelProviderFactory() {
    return modelProviderFactory;
  }

  public FeedStore getStore() {
    return store;
  }

  public TaskQueue getTaskQueue() {
    return taskQueue;
  }

  public FakeRequestManager getRequestManager() {
    return fakeRequestManager;
  }

  public AppLifecycleListener getAppLifecycleListener() {
    return appLifecycleListener;
  }

  private static class ExtensionProvider implements ProtoExtensionProvider {
    @Override
    public List<GeneratedExtension<?, ?>> getProtoExtensions() {
      return new ArrayList<>();
    }
  }

  public static Configuration getTimeoutSchedulerConfig() {
    return new Configuration.Builder()
        .put(ConfigKey.USE_TIMEOUT_SCHEDULER, USE_TIMEOUT_SCHEDULER)
        .put(ConfigKey.TIMEOUT_STORIES_ARE_CURRENT, TIMEOUT_STORIES_ARE_CURRENT)
        .put(ConfigKey.TIMEOUT_STORIES_CURRENT_WITH_REFRESH, TIMEOUT_STORIES_CURRENT_WITH_REFRESH)
        .put(ConfigKey.TIMEOUT_TIMEOUT_MS, TIMEOUT_TIMEOUT_MS)
        .put(ConfigKey.USE_DIRECT_STORAGE, Boolean.FALSE)
        .build();
  }

  /** Builder for creating the {@link InfrastructureIntegrationScope} */
  public static class Builder {
    private final ThreadUtils mockThreadUtils;
    private final ExecutorService executorService;

    private SchedulerApi schedulerApi = DISABLED_EMPTY_HEAD_REQUEST;
    private Configuration configuration = new Configuration.Builder().build();
    private Clock clock = new SystemClockImpl();
    private long requestDelayMs = 0;

    public Builder(ThreadUtils mockThreadUtils, ExecutorService executorService) {
      this.mockThreadUtils = mockThreadUtils;
      this.executorService = executorService;
    }

    public Builder setConfiguration(Configuration configuration) {
      this.configuration = configuration;
      return this;
    }

    public Builder setClock(Clock clock) {
      this.clock = clock;
      return this;
    }

    public Builder setSchedulerApi(SchedulerApi schedulerApi) {
      this.schedulerApi = schedulerApi;
      return this;
    }

    public Builder setRequestDelayMs(long requestDelayMs) {
      this.requestDelayMs = requestDelayMs;
      return this;
    }

    public InfrastructureIntegrationScope build() {
      return new InfrastructureIntegrationScope(
          mockThreadUtils, executorService, schedulerApi, clock, configuration, requestDelayMs);
    }
  }
}
