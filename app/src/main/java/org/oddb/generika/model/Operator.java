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


import io.realm.annotations.PrimaryKey;
import io.realm.annotations.LinkingObjects;
import io.realm.Realm;
import io.realm.RealmObject;
import io.realm.RealmResults;

import org.json.JSONObject;
import org.json.JSONException;

import java.lang.IllegalAccessException;
import java.lang.IllegalArgumentException;
import java.lang.NoSuchMethodException;
import java.lang.SecurityException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;


public class Operator extends RealmObject implements Retryable {
  private final static String TAG = "Operator";

  public static final String FIELD_ID = "id";

  @PrimaryKey
  private String id; // UUID

  @LinkingObjects("operator")
  private final RealmResults<Receipt> receipt = null;

  private String givenName;
  private String familyName;
  private String title;
  private String email;
  private String phone;
  private String address;
  private String city;
  private String zipcode;
  private String country;
  private String signature;

  // -- static methods

  public static String generateId() {
    return UUID.randomUUID().toString();
  }

  public static String generateId(Realm realm) {
    String id;
    while (true) {
      id = generateId();
      Operator operator = realm.where(Operator.class)
        .equalTo(FIELD_ID, id).findFirst();
      if (operator == null) {
        break;
      }
    }
    return id;
  }

  private static HashMap<String, String> operatorkeyMap() {
    // property, importing key (.amk)
    HashMap<String, String> keyMap = new HashMap<String, String>();
    keyMap.put("givenName", "given_name");
    keyMap.put("familyName", "family_name");
    keyMap.put("title", "title");
    keyMap.put("email", "email_address");
    keyMap.put("phone", "phone");
    keyMap.put("address", "postal_address");
    keyMap.put("city", "city");
    keyMap.put("zipcode", "zip_code");
    keyMap.put("country", "country");
    keyMap.put("signature", "signature");
    return keyMap;
  }

  // without id
  public static Operator newInstanceFromJSON(JSONObject json) throws
    SecurityException, NoSuchMethodException, IllegalArgumentException,
    IllegalAccessException, InvocationTargetException {
    Operator operator = new Operator();

    Class c = Operator.class;
    Class[] parameterTypes = new Class[]{String.class};
    HashMap<String, String> keyMap = operatorkeyMap();
    for (String key: keyMap.keySet()) {
      String value;
      try {
        value = json.getString(keyMap.get(key));
      } catch (JSONException _e) {
        value = null;
      }
      if (value != null && value != "" && value != "null")  {
        String methodName = "set" +
          key.substring(0, 1).toUpperCase() + key.substring(1);
        Method method = c.getDeclaredMethod(methodName, parameterTypes);
        method.invoke(operator, new Object[]{value});
      }
    }
    return operator;
  }

  public static void withRetry(final int limit, WithRetry f) {
    Retryable.withRetry(limit, f);
  }

  // -- instance methods

  public String getId() { return id; }
  public void setId(String id) { this.id = id; } // UUID

  public Receipt getReceipt() {
    if (receipt == null) { return null; }
    return receipt.where().findFirst();
  }

  public String getGivenName() { return givenName; }
  public void setGivenName(String value) { this.givenName = value; }

  public String getFamilyName() { return familyName; }
  public void setFamilyName(String value) { this.familyName = value; }

  public String getTitle() { return title; }
  public void setTitle(String value) { this.title = value; }

  public String getEmail() { return email; }
  public void setEmail(String value) { this.email = value; }

  public String getPhone() { return phone; }
  public void setPhone(String value) { this.phone = value; }

  public String getAddress() { return address; }
  public void setAddress(String value) { this.address = value; }

  public String getCity() { return city; }
  public void setCity(String value) { this.city = value; }

  public String getZipcode() { return zipcode; }
  public void setZipcode(String value) { this.zipcode = value; }

  public String getCountry() { return country; }
  public void setCountry(String value) { this.country = value; }

  public String getSignature() { return signature; }
  public void setSignature(String value) { this.signature = value; }
}
