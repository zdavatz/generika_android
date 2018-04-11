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


public class Patient extends RealmObject implements Retryable {
  private final static String TAG = "Patient";

  public static final String FIELD_ID = "id";

  @PrimaryKey
  private String id; // UUID

  @LinkingObjects("patient")
  private final RealmResults<Receipt> receipt = null;

  private String identifier;
  private String givenName;
  private String familyName;
  private String birthDate;
  private String gender;
  private String height;
  private String weight;
  private String email;
  private String phone;
  private String address;
  private String city;
  private String zipcode;
  private String country;

  // -- static methods

  public static String generateId() {
    return UUID.randomUUID().toString();
  }

  public static String generateId(Realm realm) {
    String id;
    while (true) {
      id = generateId();
      Patient patient = realm.where(Patient.class)
        .equalTo(FIELD_ID, id).findFirst();
      if (patient == null) {
        break;
      }
    }
    return id;
  }

  private static HashMap<String, String> patientkeyMap() {
    // property, importing key (.amk)
    HashMap<String, String> keyMap = new HashMap<String, String>();
    keyMap.put("identifier", "patient_id");
    keyMap.put("givenName", "given_name");
    keyMap.put("familyName", "family_name");
    keyMap.put("weight", "weight_kg");
    keyMap.put("height", "height_cm");
    keyMap.put("birthDate", "birth_date");
    keyMap.put("gender", "gender");
    keyMap.put("email", "email_address");
    keyMap.put("phone", "phone_number");
    keyMap.put("address", "postal_address");
    keyMap.put("city", "city");
    keyMap.put("zipcode", "zip_code");
    keyMap.put("country", "country");
    return keyMap;
  }

  // without id
  public static Patient newInstanceFromJSON(JSONObject json) throws
    SecurityException, NoSuchMethodException, IllegalArgumentException,
    IllegalAccessException, InvocationTargetException {
    Patient patient = new Patient();

    Class c = Patient.class;
    Class[] parameterTypes = new Class[]{String.class};
    HashMap<String, String> keyMap = patientkeyMap();
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
        method.invoke(patient, new Object[]{value});
      }
    }
    return patient;
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

  public String getIdentifier() { return identifier; }
  public void setIdentifier(String value) { this.identifier = value; }

  public String getGivenName() { return givenName; }
  public void setGivenName(String value) { this.givenName = value; }

  public String getFamilyName() { return familyName; }
  public void setFamilyName(String value) { this.familyName = value; }

  public String getWeight() { return weight; } // kg
  public void setWeight(String value) { this.weight = value; }

  public String getHeight() { return height; } // cm
  public void setHeight(String value) { this.height = value; }

  public String getBirthDate() { return birthDate; }
  public void setBirthDate(String value) { this.birthDate = value; }

  public String getGender() { return gender; }
  public void setGender(String value) { this.gender = value; }

  public String getGenderSign() {
    if (gender == null || gender.equals("")) { return ""; }
    char c = Character.toUpperCase(gender.charAt(0));
    if (c == 'F' || c == 'W') { return "F"; } // female, woman, frau
    if (c == 'M') { return "M"; } // male, man, mann
    return Character.toString(c);
  }

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

  // if this method returns `false`, `realm.cancelTransaction()` should be
  // called.
  public boolean delete() {
    boolean deleted = false;
    try {
      deleteFromRealm();
      deleted = true;
    } catch (IllegalStateException e) {
      deleted = false;
    }
    return deleted;
  }
}
