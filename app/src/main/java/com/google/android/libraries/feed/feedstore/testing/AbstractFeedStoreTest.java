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

package com.google.android.libraries.feed.feedstore.testing;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.android.libraries.feed.api.common.PayloadWithId;
import com.google.android.libraries.feed.api.common.SemanticPropertiesWithId;
import com.google.android.libraries.feed.api.common.testing.ContentIdGenerators;
import com.google.android.libraries.feed.api.store.ActionMutation.ActionType;
import com.google.android.libraries.feed.api.store.ContentMutation;
import com.google.android.libraries.feed.api.store.SessionMutation;
import com.google.android.libraries.feed.api.store.Store;
import com.google.android.libraries.feed.common.Result;
import com.google.android.libraries.feed.common.testing.RunnableSubject;
import com.google.android.libraries.feed.common.time.Clock;
import com.google.android.libraries.feed.common.time.TimingUtils;
import com.google.android.libraries.feed.host.storage.CommitResult;
import com.google.protobuf.ByteString;
import com.google.search.now.feed.client.StreamDataProto.StreamAction;
import com.google.search.now.feed.client.StreamDataProto.StreamDataOperation;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload;
import com.google.search.now.feed.client.StreamDataProto.StreamPayload.Builder;
import com.google.search.now.feed.client.StreamDataProto.StreamSession;
import com.google.search.now.feed.client.StreamDataProto.StreamSharedState;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure.Operation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.mockito.Mock;

/** Tests of the {@link FeedStore} classes. */
public abstract class AbstractFeedStoreTest {
  @Mock protected Clock clock;
  protected final TimingUtils timingUtils = new TimingUtils();

  private static final long START_TIME = 50;
  private static final long START_TIME_MILLIS = TimeUnit.SECONDS.toMillis(START_TIME);
  private static final long THREE_DAYS_AFTER_START_TIME = START_TIME + TimeUnit.DAYS.toSeconds(3);
  private static final long THREE_DAYS_AFTER_START_TIME_MILLIS =
      TimeUnit.SECONDS.toMillis(THREE_DAYS_AFTER_START_TIME);

  private static final ContentIdGenerators idGenerators = new ContentIdGenerators();
  private static final int PAYLOAD_ID = 12345;
  private static final int OPERATION_ID = 67890;
  private static final String PAYLOAD_CONTENT_ID = idGenerators.createFeatureContentId(PAYLOAD_ID);
  private static final String OPERATION_CONTENT_ID =
      idGenerators.createFeatureContentId(OPERATION_ID);
  private static final Builder STREAM_PAYLOAD =
      StreamPayload.newBuilder()
          .setStreamFeature(
              StreamFeature.newBuilder()
                  .setContentId(PAYLOAD_CONTENT_ID)
                  .setParentId(idGenerators.createRootContentId(0)));
  private static final StreamStructure STREAM_STRUCTURE =
      StreamStructure.newBuilder()
          .setContentId(OPERATION_CONTENT_ID)
          .setParentContentId(idGenerators.createRootContentId(0))
          .setOperation(Operation.UPDATE_OR_APPEND)
          .build();
  private static final StreamDataOperation STREAM_DATA_OPERATION =
      StreamDataOperation.newBuilder()
          .setStreamStructure(STREAM_STRUCTURE)
          .setStreamPayload(STREAM_PAYLOAD)
          .build();

  /** Provides an instance of the store */
  protected abstract Store getStore();

  @Test
  public void testMinimalStore() {
    Store store = getStore();
    assertThat(store.getHeadSession()).isNotNull();
  }

  @Test
  public void testContentMutation() {
    Store store = getStore();
    ContentMutation contentMutation = store.editContent();
    assertThat(contentMutation).isNotNull();
  }

  @Test
  public void newStoreHasHeadSession() throws Exception {
    Store store = getStore();
    StreamSession newSession = store.getHeadSession();
    assertThat(newSession).isNotNull();
  }

  @Test
  public void addStructureOperationToSession() throws Exception {
    Store store = getStore();
    StreamSession headSession = store.getHeadSession();
    SessionMutation mutation = store.editSession(headSession);
    mutation.add(STREAM_DATA_OPERATION.getStreamStructure());
    mutation.commit();

    Result<List<StreamStructure>> streamStructuresResult = store.getStreamStructures(headSession);

    assertThat(streamStructuresResult.isSuccessful()).isTrue();
    List<StreamStructure> streamStructures = streamStructuresResult.getValue();
    assertThat(streamStructures).hasSize(1);
    assertThat(streamStructures.get(0).getContentId()).isEqualTo(OPERATION_CONTENT_ID);
  }

  @Test
  public void addContentOperationToSession() throws Exception {
    Store store = getStore();
    ContentMutation mutation = store.editContent();
    mutation.add(PAYLOAD_CONTENT_ID, STREAM_DATA_OPERATION.getStreamPayload());
    CommitResult result = mutation.commit();

    assertThat(result).isEqualTo(CommitResult.SUCCESS);
  }

  @Test
  public void createNewSession() throws Exception {
    Store store = getStore();
    StreamSession headSession = store.getHeadSession();
    SessionMutation mutation = store.editSession(headSession);
    mutation.add(STREAM_STRUCTURE);
    mutation.commit();

    Result<StreamSession> sessionResult = store.createNewSession();
    assertThat(sessionResult.isSuccessful()).isTrue();
    StreamSession session = sessionResult.getValue();

    Result<List<StreamStructure>> streamStructuresResult = store.getStreamStructures(session);

    assertThat(streamStructuresResult.isSuccessful()).isTrue();
    List<StreamStructure> streamStructures = streamStructuresResult.getValue();
    assertThat(streamStructures).hasSize(1);
    StreamStructure streamStructure = streamStructures.get(0);
    assertThat(streamStructure.getContentId()).contains(Integer.toString(OPERATION_ID));
  }

  @Test
  public void removeSession() throws Exception {
    Store store = getStore();
    StreamSession headSession = store.getHeadSession();
    SessionMutation mutation = store.editSession(headSession);
    mutation.add(STREAM_STRUCTURE);
    mutation.commit();

    Result<StreamSession> sessionResult = store.createNewSession();
    assertThat(sessionResult.isSuccessful()).isTrue();
    StreamSession session = sessionResult.getValue();

    store.removeSession(session);

    Result<List<StreamStructure>> streamStructuresResult = store.getStreamStructures(session);

    assertThat(streamStructuresResult.isSuccessful()).isTrue();
    assertThat(streamStructuresResult.getValue()).isEmpty();
  }

  @Test
  public void clearHead() throws Exception {
    Store store = getStore();
    StreamSession headSession = store.getHeadSession();
    SessionMutation mutation = store.editSession(headSession);
    mutation.add(STREAM_STRUCTURE);
    mutation.commit();

    store.clearHead();

    Result<List<StreamStructure>> streamStructuresResult =
        store.getStreamStructures(store.getHeadSession());

    assertThat(streamStructuresResult.isSuccessful()).isTrue();
    assertThat(streamStructuresResult.getValue()).isEmpty();
  }

  @Test
  public void getSessions() throws Exception {
    Store store = getStore();
    StreamSession headSession = store.getHeadSession();
    SessionMutation mutation = store.editSession(headSession);
    mutation.add(STREAM_STRUCTURE);
    mutation.commit();

    Result<List<StreamSession>> sessionsResult = store.getAllSessions();
    assertThat(sessionsResult.isSuccessful()).isTrue();
    List<StreamSession> sessions = sessionsResult.getValue();
    assertThat(sessions).isEmpty();

    Result<StreamSession> sessionResult = store.createNewSession();
    assertThat(sessionResult.isSuccessful()).isTrue();
    StreamSession session = sessionResult.getValue();

    sessionsResult = store.getAllSessions();
    assertThat(sessionsResult.isSuccessful()).isTrue();
    sessions = sessionsResult.getValue();
    assertThat(sessions).hasSize(1);
    assertThat(sessions.get(0).getStreamToken()).isEqualTo(session.getStreamToken());

    sessionResult = store.createNewSession();
    assertThat(sessionResult.isSuccessful()).isTrue();
    session = sessionResult.getValue();

    sessionsResult = store.getAllSessions();
    assertThat(sessionsResult.isSuccessful()).isTrue();
    sessions = sessionsResult.getValue();
    assertThat(sessions).hasSize(2);
  }

  @Test
  public void editContent() throws Exception {
    StreamPayload streamPayload =
        StreamPayload.newBuilder()
            .setStreamFeature(StreamFeature.newBuilder().setContentId(PAYLOAD_CONTENT_ID))
            .build();
    Store store = getStore();

    CommitResult commitResult = store.editContent().add(PAYLOAD_CONTENT_ID, streamPayload).commit();
    assertThat(commitResult).isEqualTo(CommitResult.SUCCESS);

    Result<List<PayloadWithId>> payloadsResult =
        store.getPayloads(Collections.singletonList(PAYLOAD_CONTENT_ID));
    assertThat(payloadsResult.isSuccessful()).isTrue();
    assertThat(payloadsResult.getValue()).hasSize(1);
    assertThat(payloadsResult.getValue().get(0).contentId).isEqualTo(PAYLOAD_CONTENT_ID);
    assertThat(payloadsResult.getValue().get(0).payload).isEqualTo(streamPayload);
  }

  @Test
  public void getStreamFeatures() throws Exception {}

  @Test
  public void getSharedStates() throws Exception {
    StreamSharedState streamSharedState =
        StreamSharedState.newBuilder().setContentId(PAYLOAD_CONTENT_ID).build();
    Store store = getStore();
    store
        .editContent()
        .add(
            String.valueOf(PAYLOAD_ID),
            StreamPayload.newBuilder().setStreamSharedState(streamSharedState).build())
        .commit();
    Result<List<StreamSharedState>> sharedStatesResult = store.getSharedStates();
    assertThat(sharedStatesResult.isSuccessful()).isTrue();
    List<StreamSharedState> sharedStates = sharedStatesResult.getValue();
    assertThat(sharedStates).hasSize(1);
    assertThat(sharedStates.get(0)).isEqualTo(streamSharedState);
  }

  @Test
  public void getPayloads_noPayload() throws Exception {
    List<String> contentIds = new ArrayList<>();
    contentIds.add(PAYLOAD_CONTENT_ID);

    Store store = getStore();
    Result<List<PayloadWithId>> payloadsResult = store.getPayloads(contentIds);
    assertThat(payloadsResult.isSuccessful()).isTrue();
    List<PayloadWithId> payloads = payloadsResult.getValue();
    assertThat(payloads).isEmpty();
  }

  @Test
  public void deleteHead_notAllowed() throws Exception {
    RunnableSubject.assertThatRunnable(
            () -> {
              Store store = getStore();
              store.removeSession(StreamSession.newBuilder().setStreamToken("$HEAD").build());
            })
        .throwsAnExceptionOfType(IllegalStateException.class);
  }

  @Test
  public void editSemanticProperties() {
    Store store = getStore();
    assertThat(store.editSemanticProperties()).isNotNull();
  }

  @Test
  public void getSemanticProperties() {
    ByteString semanticData = ByteString.copyFromUtf8("helloWorld");
    Store store = getStore();
    store.editSemanticProperties().add(PAYLOAD_CONTENT_ID, semanticData).commit();
    Result<List<SemanticPropertiesWithId>> semanticPropertiesResult =
        store.getSemanticProperties(Collections.singletonList(PAYLOAD_CONTENT_ID));
    assertThat(semanticPropertiesResult.isSuccessful()).isTrue();
    List<SemanticPropertiesWithId> semanticProperties = semanticPropertiesResult.getValue();
    assertThat(semanticProperties).hasSize(1);
    assertThat(semanticProperties.get(0).contentId).isEqualTo(PAYLOAD_CONTENT_ID);
    assertThat(semanticProperties.get(0).semanticData).isEqualTo(semanticData.toByteArray());
  }

  @Test
  public void getSemanticProperties_requestDifferentKey() {
    ByteString semanticData = ByteString.copyFromUtf8("helloWorld");
    Store store = getStore();
    store.editSemanticProperties().add(PAYLOAD_CONTENT_ID, semanticData).commit();
    Result<List<SemanticPropertiesWithId>> semanticPropertiesResult =
        store.getSemanticProperties(Collections.singletonList(OPERATION_CONTENT_ID));
    assertThat(semanticPropertiesResult.isSuccessful()).isTrue();
    List<SemanticPropertiesWithId> semanticProperties = semanticPropertiesResult.getValue();
    assertThat(semanticProperties).isEmpty();
  }

  @Test
  public void getSemanticProperties_doesNotExist() {
    Store store = getStore();
    Result<List<SemanticPropertiesWithId>> semanticPropertiesResult =
        store.getSemanticProperties(Collections.singletonList(PAYLOAD_CONTENT_ID));
    assertThat(semanticPropertiesResult.isSuccessful()).isTrue();
    List<SemanticPropertiesWithId> semanticProperties = semanticPropertiesResult.getValue();
    assertThat(semanticProperties).isEmpty();
  }

  @Test
  public void getDismissActions() {
    when(clock.currentTimeMillis()).thenReturn(START_TIME_MILLIS);
    Store store = getStore();
    store.editActions().add(ActionType.DISMISS, OPERATION_CONTENT_ID).commit();
    Result<List<StreamAction>> dismissActionsResult = store.getAllDismissActions();
    assertThat(dismissActionsResult.isSuccessful()).isTrue();
    List<StreamAction> dismissActions = dismissActionsResult.getValue();
    assertThat(dismissActions).isNotEmpty();
    assertThat(dismissActions.get(0).getAction()).isEqualTo(ActionType.DISMISS);
    assertThat(dismissActions.get(0).getFeatureContentId()).isEqualTo(OPERATION_CONTENT_ID);
    assertThat(dismissActions.get(0).getTimestampSeconds()).isEqualTo(START_TIME);
  }

  @Test
  public void getDismissActions_notIncludedInSessions() {
    when(clock.currentTimeMillis()).thenReturn(START_TIME_MILLIS);
    Store store = getStore();
    store.editActions().add(ActionType.DISMISS, OPERATION_CONTENT_ID).commit();
    Result<List<StreamSession>> allSessionsResult = store.getAllSessions();
    assertThat(allSessionsResult.isSuccessful()).isTrue();
    List<StreamSession> allSessions = allSessionsResult.getValue();
    assertThat(allSessions).isEmpty();
  }

  @Test
  public void getDismissActions_multipleDismisses() {
    when(clock.currentTimeMillis()).thenReturn(START_TIME_MILLIS);
    Store store = getStore();
    store
        .editActions()
        .add(ActionType.DISMISS, OPERATION_CONTENT_ID)
        .add(ActionType.DISMISS, PAYLOAD_CONTENT_ID)
        .commit();
    Result<List<StreamAction>> dismissActionsResult = store.getAllDismissActions();
    assertThat(dismissActionsResult.isSuccessful()).isTrue();
    List<StreamAction> dismissActions = dismissActionsResult.getValue();
    assertThat(dismissActions).isNotEmpty();
    assertThat(dismissActions.get(0).getAction()).isEqualTo(ActionType.DISMISS);
    assertThat(dismissActions.get(0).getFeatureContentId()).isEqualTo(OPERATION_CONTENT_ID);
    assertThat(dismissActions.get(0).getTimestampSeconds()).isEqualTo(START_TIME);
    assertThat(dismissActions.get(1).getAction()).isEqualTo(ActionType.DISMISS);
    assertThat(dismissActions.get(1).getFeatureContentId()).isEqualTo(PAYLOAD_CONTENT_ID);
    assertThat(dismissActions.get(1).getTimestampSeconds()).isEqualTo(START_TIME);
  }

  @Test
  public void getDismissActions_expired() {
    when(clock.currentTimeMillis()).thenReturn(START_TIME_MILLIS);
    Store store = getStore();
    store.editActions().add(ActionType.DISMISS, OPERATION_CONTENT_ID).commit();
    when(clock.currentTimeMillis()).thenReturn(THREE_DAYS_AFTER_START_TIME_MILLIS);
    Result<List<StreamAction>> dismissActionsResult = store.getAllDismissActions();
    assertThat(dismissActionsResult.isSuccessful()).isTrue();
    List<StreamAction> dismissActions = dismissActionsResult.getValue();

    // Should still return expired actions.
    assertThat(dismissActions).isNotEmpty();
    assertThat(dismissActions.get(0).getAction()).isEqualTo(ActionType.DISMISS);
    assertThat(dismissActions.get(0).getFeatureContentId()).isEqualTo(OPERATION_CONTENT_ID);
    assertThat(dismissActions.get(0).getTimestampSeconds()).isEqualTo(START_TIME);
  }
}
