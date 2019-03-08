package org.chromium.chrome.browser.browserservices.trustedwebactivityui.view;

import android.content.res.Resources;
import dagger.Lazy;
import dagger.internal.DoubleCheck;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;
import org.chromium.chrome.browser.browserservices.trustedwebactivityui.TrustedWebActivityModel;
import org.chromium.chrome.browser.snackbar.SnackbarManager;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class TrustedWebActivityDisclosureView_Factory
    implements Factory<TrustedWebActivityDisclosureView> {
  private final Provider<Resources> resourcesProvider;

  private final Provider<SnackbarManager> snackbarManagerProvider;

  private final Provider<TrustedWebActivityModel> modelProvider;

  public TrustedWebActivityDisclosureView_Factory(
      Provider<Resources> resourcesProvider,
      Provider<SnackbarManager> snackbarManagerProvider,
      Provider<TrustedWebActivityModel> modelProvider) {
    this.resourcesProvider = resourcesProvider;
    this.snackbarManagerProvider = snackbarManagerProvider;
    this.modelProvider = modelProvider;
  }

  @Override
  public TrustedWebActivityDisclosureView get() {
    return provideInstance(resourcesProvider, snackbarManagerProvider, modelProvider);
  }

  public static TrustedWebActivityDisclosureView provideInstance(
      Provider<Resources> resourcesProvider,
      Provider<SnackbarManager> snackbarManagerProvider,
      Provider<TrustedWebActivityModel> modelProvider) {
    return new TrustedWebActivityDisclosureView(
        resourcesProvider.get(), DoubleCheck.lazy(snackbarManagerProvider), modelProvider.get());
  }

  public static TrustedWebActivityDisclosureView_Factory create(
      Provider<Resources> resourcesProvider,
      Provider<SnackbarManager> snackbarManagerProvider,
      Provider<TrustedWebActivityModel> modelProvider) {
    return new TrustedWebActivityDisclosureView_Factory(
        resourcesProvider, snackbarManagerProvider, modelProvider);
  }

  public static TrustedWebActivityDisclosureView newTrustedWebActivityDisclosureView(
      Resources resources, Lazy<SnackbarManager> snackbarManager, TrustedWebActivityModel model) {
    return new TrustedWebActivityDisclosureView(resources, snackbarManager, model);
  }
}
