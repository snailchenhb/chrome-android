package org.chromium.chrome.browser.browserservices.trustedwebactivityui.view;

import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;
import org.chromium.chrome.browser.browserservices.trustedwebactivityui.TrustedWebActivityModel;
import org.chromium.chrome.browser.customtabs.CustomTabBrowserControlsVisibilityDelegate;
import org.chromium.chrome.browser.fullscreen.ChromeFullscreenManager;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class TrustedWebActivityToolbarView_Factory
    implements Factory<TrustedWebActivityToolbarView> {
  private final Provider<ChromeFullscreenManager> fullscreenManagerProvider;

  private final Provider<CustomTabBrowserControlsVisibilityDelegate>
      controlsVisibilityDelegateProvider;

  private final Provider<TrustedWebActivityModel> modelProvider;

  public TrustedWebActivityToolbarView_Factory(
      Provider<ChromeFullscreenManager> fullscreenManagerProvider,
      Provider<CustomTabBrowserControlsVisibilityDelegate> controlsVisibilityDelegateProvider,
      Provider<TrustedWebActivityModel> modelProvider) {
    this.fullscreenManagerProvider = fullscreenManagerProvider;
    this.controlsVisibilityDelegateProvider = controlsVisibilityDelegateProvider;
    this.modelProvider = modelProvider;
  }

  @Override
  public TrustedWebActivityToolbarView get() {
    return provideInstance(
        fullscreenManagerProvider, controlsVisibilityDelegateProvider, modelProvider);
  }

  public static TrustedWebActivityToolbarView provideInstance(
      Provider<ChromeFullscreenManager> fullscreenManagerProvider,
      Provider<CustomTabBrowserControlsVisibilityDelegate> controlsVisibilityDelegateProvider,
      Provider<TrustedWebActivityModel> modelProvider) {
    return new TrustedWebActivityToolbarView(
        fullscreenManagerProvider.get(),
        controlsVisibilityDelegateProvider.get(),
        modelProvider.get());
  }

  public static TrustedWebActivityToolbarView_Factory create(
      Provider<ChromeFullscreenManager> fullscreenManagerProvider,
      Provider<CustomTabBrowserControlsVisibilityDelegate> controlsVisibilityDelegateProvider,
      Provider<TrustedWebActivityModel> modelProvider) {
    return new TrustedWebActivityToolbarView_Factory(
        fullscreenManagerProvider, controlsVisibilityDelegateProvider, modelProvider);
  }

  public static TrustedWebActivityToolbarView newTrustedWebActivityToolbarView(
      ChromeFullscreenManager fullscreenManager,
      CustomTabBrowserControlsVisibilityDelegate controlsVisibilityDelegate,
      TrustedWebActivityModel model) {
    return new TrustedWebActivityToolbarView(fullscreenManager, controlsVisibilityDelegate, model);
  }
}
