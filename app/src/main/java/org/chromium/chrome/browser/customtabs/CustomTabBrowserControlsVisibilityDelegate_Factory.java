package org.chromium.chrome.browser.customtabs;

import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;
import org.chromium.chrome.browser.ActivityTabProvider;
import org.chromium.chrome.browser.fullscreen.ChromeFullscreenManager;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class CustomTabBrowserControlsVisibilityDelegate_Factory
    implements Factory<CustomTabBrowserControlsVisibilityDelegate> {
  private final Provider<ChromeFullscreenManager> fullscreenManagerProvider;

  private final Provider<ActivityTabProvider> tabProvider;

  public CustomTabBrowserControlsVisibilityDelegate_Factory(
      Provider<ChromeFullscreenManager> fullscreenManagerProvider,
      Provider<ActivityTabProvider> tabProvider) {
    this.fullscreenManagerProvider = fullscreenManagerProvider;
    this.tabProvider = tabProvider;
  }

  @Override
  public CustomTabBrowserControlsVisibilityDelegate get() {
    return provideInstance(fullscreenManagerProvider, tabProvider);
  }

  public static CustomTabBrowserControlsVisibilityDelegate provideInstance(
      Provider<ChromeFullscreenManager> fullscreenManagerProvider,
      Provider<ActivityTabProvider> tabProvider) {
    return new CustomTabBrowserControlsVisibilityDelegate(
        fullscreenManagerProvider.get(), tabProvider.get());
  }

  public static CustomTabBrowserControlsVisibilityDelegate_Factory create(
      Provider<ChromeFullscreenManager> fullscreenManagerProvider,
      Provider<ActivityTabProvider> tabProvider) {
    return new CustomTabBrowserControlsVisibilityDelegate_Factory(
        fullscreenManagerProvider, tabProvider);
  }

  public static CustomTabBrowserControlsVisibilityDelegate
      newCustomTabBrowserControlsVisibilityDelegate(
          ChromeFullscreenManager fullscreenManager, ActivityTabProvider tabProvider) {
    return new CustomTabBrowserControlsVisibilityDelegate(fullscreenManager, tabProvider);
  }
}
