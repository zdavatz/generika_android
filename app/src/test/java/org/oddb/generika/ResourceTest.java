package org.oddb.generika;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.robolectric.Robolectric;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import android.content.Context;

import org.oddb.generika.BuildConfig;
import org.oddb.generika.R;


@RunWith(RobolectricTestRunner.class)
@Config(constants=BuildConfig.class)
public class ResourceTest {
  @Before
  public void setup() throws Exception {
    ShadowLog.stream = System.out;
  }

  @Test
  @Config(qualifiers="")
  public void testResourceInPortrait() throws Exception {
    final Context context = RuntimeEnvironment.application;
    assertEquals("Settings", context.getString(R.id.settings));
  }

  @Test
  @Config(qualifiers="de")
  public void testResourceInLandscape() throws Exception {
    final Context context = RuntimeEnvironment.application;
    assertEquals("Einstellungen", context.getString(R.id.settings));
  }
}
