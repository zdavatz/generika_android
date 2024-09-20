/*
 *  Generika Android
 *  Copyright (C) 2018 ywesee GmbH
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.oddb.generika;

import android.app.Application;
import android.content.res.Configuration;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.RealmConfiguration;

import org.oddb.generika.model.Data;
import org.oddb.generika.model.Migration;
import org.oddb.generika.util.AppLocale;


public class GenerikaApplication extends Application {

  private static final int SCHEME_VERSION = 2;

  @Override
  public void onCreate() {
    super.onCreate();

    setLocale();
    initRealm();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);

    setLocale();
  }

  protected void initRealm() {
    Realm.init(this);

    RealmConfiguration config = new RealmConfiguration.Builder()
      .name("generika.realm")
      .schemaVersion(SCHEME_VERSION)
      .migration(new Migration())
      .initialData(new Realm.Transaction() {
        @Override
        public void execute(Realm realm) {
          RealmResults<Data> data = realm.where(Data.class).findAll();
          Data d0, d1;
          if (data.size() != 2) {
            // by patient oneself / via barcode scanner
            d0 = realm.createObject(Data.class);
            d0.setSourceType("barcode");
            // by doctor, pharmacy (operator) / via .amk file
            d1 = realm.createObject(Data.class);
            d1.setSourceType("amkjson");
          }
        }
      })
      .allowWritesOnUiThread(true)
      .build();

    // enable this, if delete all items at boot
    //Realm.deleteRealm(realmConfig);

    Realm.setDefaultConfiguration(config);
  }

  protected void setLocale() {
    AppLocale.setLocale(this);
  }
}
