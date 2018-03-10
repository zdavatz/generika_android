package org.oddb.generika;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.mockito.Mockito;
import static org.mockito.Matchers.any;

import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.powermock.modules.junit4.rule.PowerMockRule;

import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

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

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmObject;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.internal.RealmCore;
import io.realm.log.RealmLog;
// fix
import io.realm.FakeRealmInitFixForTestEnvironment;

import org.oddb.generika.BuildConfig;
import org.oddb.generika.R;


@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(RobolectricTestRunner.class)
@Config(constants=BuildConfig.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@SuppressStaticInitializationFor("io.realm.internal.Util")
@PrepareForTest({Realm.class, RealmConfiguration.class, RealmQuery.class, RealmResults.class, RealmCore.class, RealmLog.class})
public class SettingsActivityTest {
  Realm mockRealm;

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  @Before
  public void setup() throws Exception {
    // won't work right now
    // - https://github.com/robolectric/robolectric/issues/1389
    // - https://github.com/realm/realm-java/issues/3808
    ShadowLog.stream = System.out;

		mockStatic(RealmCore.class);
		mockStatic(RealmLog.class);
		mockStatic(Realm.class);
		mockStatic(RealmConfiguration.class);

    FakeRealmInitFixForTestEnvironment.init(RuntimeEnvironment.application);
		//Realm.init(RuntimeEnvironment.application);
		//RealmCore.loadLibrary(any(Context.class));

		final Realm mockRealm = mock(Realm.class);
		final RealmConfiguration mockRealmConfig = mock(RealmConfiguration.class);

		doNothing().when(RealmCore.class);

		whenNew(RealmConfiguration.class).withAnyArguments().thenReturn(mockRealmConfig);
		when(Realm.getDefaultInstance()).thenReturn(mockRealm);

    this.mockRealm = mockRealm;
  }

  @Test
  public void testToolBar() throws Exception {
    Intent intent = new Intent(
      RuntimeEnvironment.application.getApplicationContext(),
      SettingsActivity.class);
    // Appearently, Realm and Robolectric don't work together right now :'(
    //
    // - https://github.com/realm/realm-java/issues/904
    // - https://github.com/robolectric/robolectric/issues/1389

    assertEquals("Robolectric", "Realm");

    //SettingsActivity settings = Robolectric.buildActivity(
    //  SettingsActivity.class).newIntent(intent).create().get();

    //Toolbar toolbar = settings.findViewById(R.id.settings);
    //assertEquals("Settings", toolbar.getTitle());
  }
}
