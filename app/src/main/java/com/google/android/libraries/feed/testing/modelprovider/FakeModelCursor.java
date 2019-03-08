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

package com.google.android.libraries.feed.testing.modelprovider;

import com.google.android.libraries.feed.api.modelprovider.ModelChild;
import com.google.android.libraries.feed.api.modelprovider.ModelCursor;
import com.google.android.libraries.feed.api.modelprovider.ModelFeature;
import com.google.android.libraries.feed.api.modelprovider.ModelToken;
import com.google.common.collect.ImmutableList;
import com.google.search.now.feed.client.StreamDataProto.StreamFeature;
import com.google.search.now.ui.stream.StreamStructureProto.Card;
import com.google.search.now.ui.stream.StreamStructureProto.Cluster;
import com.google.search.now.ui.stream.StreamStructureProto.Content;
import java.util.ArrayList;
import java.util.List;

/** A fake {@link ModelCursor} for testing. */
public class FakeModelCursor implements ModelCursor {

  private List<ModelChild> modelChildren;
  private int currentIndex = 0;

  public FakeModelCursor(List<ModelChild> modelChildren) {
    currentIndex = 0;
    this.modelChildren = modelChildren;
  }

  public void setModelChildren(List<ModelChild> modelChildren) {
    this.modelChildren = modelChildren;
  }

  @Override
  /*@Nullable*/
  public ModelChild getNextItem() {
    if (isAtEnd()) {
      return null;
    }
    return modelChildren.get(currentIndex++);
  }

  @Override
  public boolean isAtEnd() {
    return currentIndex >= modelChildren.size();
  }

  public ModelChild getChildAt(int i) {
    return modelChildren.get(i);
  }

  public List<ModelChild> getModelChildren() {
    return ImmutableList.copyOf(modelChildren);
  }

  public static class Builder {
    List<ModelChild> cursorChildren = new ArrayList<>();

    public Builder addCard() {
      final FakeModelCursor build = new Builder().build();
      return addChildWithModelFeature(getCardModelFeatureWithCursor(build));
    }

    public Builder addChild(ModelFeature feature) {
      return addChild(new FakeModelChild.Builder().setModelFeature(feature).build());
    }

    public Builder addChild(ModelChild child) {
      cursorChildren.add(child);
      return this;
    }

    public Builder addCluster() {
      final FakeModelCursor cursor = new FakeModelCursor(new ArrayList<>());
      return addChildWithModelFeature(getClusterModelFeatureWithCursor(cursor));
    }

    public Builder addChildWithModelFeature(FakeModelFeature modelFeature) {
      ModelChild cardChild = new FakeModelChild.Builder().setModelFeature(modelFeature).build();
      cursorChildren.add(cardChild);
      return this;
    }

    public Builder addContent(Content content) {
      final FakeModelCursor cursor = new FakeModelCursor(new ArrayList<>());
      return addChildWithModelFeature(getContentModelFeatureWithCursor(content, cursor));
    }

    public Builder addToken(boolean isSynthetic) {
      return addToken(new FakeModelToken.Builder().setIsSynthetic(isSynthetic).build());
    }

    public Builder addToken() {
      return addToken(/* isSynthetic= */ false);
    }

    public Builder addToken(ModelToken token) {
      ModelChild tokenChild = new FakeModelChild.Builder().setModelToken(token).build();

      cursorChildren.add(tokenChild);

      return this;
    }

    public Builder addSyntheticToken() {
      return addToken(/* isSynthetic= */ true);
    }

    public FakeModelCursor build() {
      return new FakeModelCursor(cursorChildren);
    }
  }

  public static FakeModelFeature getCardModelFeatureWithCursor(ModelCursor modelCursor) {
    return new FakeModelFeature.Builder()
        .setStreamFeature(StreamFeature.newBuilder().setCard(Card.getDefaultInstance()).build())
        .setModelCursor(modelCursor)
        .build();
  }

  public static FakeModelFeature getClusterModelFeatureWithCursor(ModelCursor cursor) {
    return new FakeModelFeature.Builder()
        .setStreamFeature(
            StreamFeature.newBuilder().setCluster(Cluster.getDefaultInstance()).build())
        .setModelCursor(cursor)
        .build();
  }

  public static FakeModelFeature getContentModelFeatureWithCursor(
      Content content, ModelCursor cursor) {
    return new FakeModelFeature.Builder()
        .setStreamFeature(StreamFeature.newBuilder().setContent(content).build())
        .setModelCursor(cursor)
        .build();
  }

  public static FakeModelFeature getModelFeatureWithEmptyCursor() {
    return new FakeModelFeature.Builder().setModelCursor(new Builder().build()).build();
  }
}
