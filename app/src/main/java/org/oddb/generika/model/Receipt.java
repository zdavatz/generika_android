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
package org.oddb.generika.model;

import android.util.Log;

import io.realm.annotations.PrimaryKey;
import io.realm.annotations.LinkingObjects;
import io.realm.annotations.Required;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmList;
import io.realm.RealmResults;

import java.lang.IllegalAccessException;
import java.lang.IllegalArgumentException;
import java.lang.NoSuchMethodException;
import java.lang.SecurityException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;


public class Receipt extends RealmObject implements Retryable {
  private final static String TAG = "Receipt";

  public static final String FIELD_ID = "id";

  @PrimaryKey
  private String id; // UUID

  @LinkingObjects("files")
  private final RealmResults<Data> source = null;

  @Required
  private String hashedKey; // prescription_hash

  private String placeDate; // place_date

  private String givenName;
  private String familyName;

  private RealmList<Product> medications;

  // private Operator operator;
  // private Patient patient;

  // -- static methods

  private static String generateId() {
    return UUID.randomUUID().toString();
  }

  private static String generateId(Realm realm) {
    String id;
    while (true) {
      id = generateId();
      Receipt receipt = realm.where(Receipt.class)
        .equalTo(FIELD_ID, id).findFirst();
      if (receipt == null) {
        break;
      }
    }
    return id;
  }

  public static void withRetry(final int limit, WithRetry f) {
    Retryable.withRetry(limit, f);
  }

  public static class Amkfile {
    String value;
    String filepath;

    public void setValue(String value_) {
      this.value = value_;
    }
    public void setFilepath(String filepath_) {
      this.filepath = filepath_;
    }
  }

  public static Receipt insertNewAmkfileIntoSource(
    Realm realm,
    Receipt.Amkfile amkfile, Data data, boolean withUniqueCheck) {
    // TODO
    return new Receipt();
  }

  // -- instance methods

  public String getId() { return id; }
  public void setId(String id) { this.id = id; } // UUID

  public Data getSource() {
    if (source == null) { return null; }
    return source.where().findFirst();
  }

  public String getHashedKey() { return hashedKey; }
  public void setHashedKey(String hashedKey) { this.hashedKey = hashedKey; }

  // TODO
  public String getIssuedDate() { return ""; } // (via place_date)
  public String getIssuedPlace() { return ""; } // (via place_date)

  public String getName() { return givenName; }

  // NOTE: it must be called in realm transaction
  public void updateProperties(HashMap<String, String> properties) throws
    SecurityException, NoSuchMethodException, IllegalArgumentException {
    // TODO
  };

  public boolean delete() {
    boolean deleted = true;
    if (deleted) {
      // this method returns nothing...
      deleteFromRealm();
    } else {
      Log.d(TAG, "(delete) id: " + id);
    }
    // TODO:
    // (for now) return image is deleted or not
    return deleted;
  }
}
