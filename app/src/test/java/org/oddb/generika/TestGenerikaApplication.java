package org.oddb.generika;

import org.robolectric.TestLifecycleApplication;

import java.lang.reflect.Method;

import org.oddb.generika.GenerikaApplication;


public class TestGenerikaApplication extends GenerikaApplication
  implements TestLifecycleApplication {
  @Override
  public void onCreate() {
    super.onCreate();
  }

  @Override
  public void beforeTest(Method method) {}

  @Override
  public void prepareTest(Object test) {}

  @Override
  public void afterTest(Method method) {}

  @Override
  protected void initRealm() {
    // :'(
  }

  @Override
  protected void setLocale() {
    super.setLocale();
  }
}
