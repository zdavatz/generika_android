package org.oddb.generika;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import android.content.Context;

import org.oddb.generika.CustomTestRunner;

import org.oddb.generika.BuildConfig;
import org.oddb.generika.R;


@RunWith(CustomTestRunner.class)
@Config(constants=BuildConfig.class)
public class ResourceTest {
  private Context context;  // application

  @Before
  public void setup() throws Exception {
    ShadowLog.stream = System.out;

    this.context = RuntimeEnvironment.application;
  }

  @Test
  @Config(qualifiers="")
  public void testResourceInPortrait() throws Exception {
    assertEquals("Settings", context.getString(R.id.settings));
  }

  @Test
  @Config(qualifiers="de")
  public void testResourceInLandscape() throws Exception {
    assertEquals("Einstellungen", context.getString(R.id.settings));
  }
}
