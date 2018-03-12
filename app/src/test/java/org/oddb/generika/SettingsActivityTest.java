package org.oddb.generika;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.mockito.Mockito;

import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.support.v7.widget.Toolbar;

import org.oddb.generika.CustomTestRunner;

import org.oddb.generika.BuildConfig;
import org.oddb.generika.R;


@RunWith(CustomTestRunner.class)
@Config(constants=BuildConfig.class)
public class SettingsActivityTest {
  @Before
  public void setup() throws Exception {
    ShadowLog.stream = System.out;
  }

  @Test
  public void testToolBar() throws Exception {
    Intent intent = new Intent(
      RuntimeEnvironment.application.getApplicationContext(),
      SettingsActivity.class);

    // NOTE:
    // Appearently, Realm and Robolectric don't work together right now :'(
    //
    // - https://github.com/realm/realm-java/issues/904
    // - https://github.com/robolectric/robolectric/issues/1389

    //assertEquals("Robolectric", "Realm");

    SettingsActivity settings = Robolectric.buildActivity(
      SettingsActivity.class).newIntent(intent).create().get();

    Toolbar toolbar = settings.findViewById(R.id.settings);
    assertEquals("Settings", toolbar.getTitle());
  }
}
