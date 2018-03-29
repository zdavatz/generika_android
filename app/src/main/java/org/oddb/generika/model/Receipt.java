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


public class Receipt extends RealmObject {
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
}
