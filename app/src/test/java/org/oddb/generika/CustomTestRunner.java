package org.oddb.generika;

import org.robolectric.DefaultTestLifecycle;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.TestLifecycle;

import org.junit.runners.model.InitializationError;


public class CustomTestRunner extends RobolectricTestRunner {
  public CustomTestRunner(
    final Class<?> testClass) throws InitializationError {
    super(testClass);
  }

  @Override
  protected Class<? extends TestLifecycle> getTestLifecycleClass() {
    return CustomTestLifecycle.class;
  }

  public static class CustomTestLifecycle extends DefaultTestLifecycle {
    // pass
  }
}
