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

import com.google.android.libraries.feed.api.modelprovider.ModelProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProvider.ViewDepthProvider;
import com.google.android.libraries.feed.api.modelprovider.ModelProviderFactory;
import com.google.android.libraries.feed.common.functional.Predicate;
import com.google.search.now.feed.client.StreamDataProto.StreamStructure;

/**
 * Fake for tests using {@link ModelProviderFactory}. Functionality should be added to this class as
 * needed.
 */
public class FakeModelProviderFactory implements ModelProviderFactory {

  private FakeModelProvider modelProvider;

  @Override
  public ModelProvider create(String sessionToken) {
    this.modelProvider = new FakeModelProvider();
    return modelProvider;
  }

  @Override
  public ModelProvider createNew(/*@Nullable*/ ViewDepthProvider viewDepthProvider) {
    this.modelProvider = new FakeModelProvider();
    return modelProvider;
  }

  @Override
  public ModelProvider createNew(
      /*@Nullable*/ ViewDepthProvider viewDepthProvider,
      /*@Nullable*/ Predicate<StreamStructure> filterPredicate) {
    this.modelProvider = new FakeModelProvider();
    return modelProvider;
  }

  public FakeModelProvider getLatestModelProvider() {
    return modelProvider;
  }
}
