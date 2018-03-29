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

import io.realm.RealmList;
import io.realm.RealmObject;

import org.oddb.generika.model.Product;
import org.oddb.generika.model.Receipt;


public class Data extends RealmObject {
  // sourceType: scanner/prescription
  private String sourceType;

  // NOTE:
  // Realm Java does not have Polymorphism yet. So these fields
  // have each different objects based on sourceType value.
  //
  // https://github.com/realm/realm-java/issues/761
  private RealmList<Product> items;
  private RealmList<Receipt> files;

  public String getSourceType() { return sourceType; }
  public void setSourceType(String sourceType) {
    this.sourceType = sourceType; }

  public RealmList<Product> getItems() {
    // scanned drugs, prescribed medications
    return items;
  }

  public RealmList<Receipt> getFiles() {
    // prescription docs
    return files;
  }
}
