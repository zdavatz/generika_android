package org.oddb.generika;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

import java.util.Locale;

import org.oddb.generika.util.AppLocale;
import org.oddb.generika.util.Constant;



@RunWith(CustomTestRunner.class)
@Config(constants=BuildConfig.class)
public class AppLocaleTest {
  private SharedPreferences sharedPreferences;
  private Context context;  // application

  @Before
  public void setup() throws Exception {
    ShadowLog.stream = System.out;

    this.context = RuntimeEnvironment.application.getApplicationContext();
    this.sharedPreferences = PreferenceManager
      .getDefaultSharedPreferences(context);
  }

  @After
  public void tearDown() throws Exception {
    // back to default
    sharedPreferences.edit().putString(
      Constant.kAppLocale, Constant.LANG_DE).commit();
  }

  @Test
  public void testGetLocaleReturnsDeAsDefault() throws Exception {
    AppLocale localePreference = new AppLocale();

    assertEquals(new Locale("de"), localePreference.getLocale(context));
  }

  @Test
  public void testGetLocaleReturnsValidLocaleCorrectlySet() throws Exception {
    AppLocale localePreference = new AppLocale();

    sharedPreferences.edit().putString(
      Constant.kAppLocale, Constant.LANG_DE).commit();
    assertEquals(new Locale("de"), localePreference.getLocale(context));

    sharedPreferences.edit().putString(
      Constant.kAppLocale, Constant.LANG_DE).commit();
    assertEquals(new Locale("de"), localePreference.getLocale(context));

    sharedPreferences.edit().putString(
      Constant.kAppLocale, Constant.LANG_FR).commit();
    assertEquals(new Locale("fr"), localePreference.getLocale(context));

  }

  @Test
  public void testGetLocaleReturnsDeIfUnknownLocaleIsSet() {
    AppLocale localePreference = new AppLocale();

    sharedPreferences.edit().putString(
      Constant.kAppLocale, "it").commit();
    assertEquals(new Locale("de"), localePreference.getLocale(context));
  }
}
