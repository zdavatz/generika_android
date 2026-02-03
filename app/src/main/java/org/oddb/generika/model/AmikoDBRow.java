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

import android.database.Cursor;

import java.util.ArrayList;

/**
 * Models a row from the amikodb table.
 */
public class AmikoDBRow {
    public String id;
    public String title;
    public String auth;
    public String atc;
    public String substances;
    public String regnrs;
    public String atc_class;
    public String tindex_str;
    public String application_str;
    public String indications_str;
    public String customer_id;
    public String pack_info_str;
    public String add_info_str;
    public String ids_str;
    public String titles_str;
    public String content;
    public String style_str;
    public String packages;
    public String type;

    public AmikoDBRow(Cursor cursor) {
        this.id = cursor.getString(0);
        this.title = cursor.getString(1);
        this.auth = cursor.getString(2);
        this.atc = cursor.getString(3);
        this.substances = cursor.getString(4);
        this.regnrs = cursor.getString(5);
        this.atc_class = cursor.getString(6);
        this.tindex_str = cursor.getString(7);
        this.application_str = cursor.getString(8);
        this.indications_str = cursor.getString(9);
        this.customer_id = cursor.getString(10);
        this.pack_info_str = cursor.getString(11);
        this.add_info_str = cursor.getString(12);
        this.ids_str = cursor.getString(13);
        this.titles_str = cursor.getString(14);
        this.content = cursor.getString(15);
        this.style_str = cursor.getString(16);
        this.packages = cursor.getString(17);
        this.type = cursor.getString(18);
    }

    public ArrayList<AmikoDBPackage> parsedPackages() {
        if (this.packages == null || this.packages.isEmpty()) {
            return null;
        }
        ArrayList<AmikoDBPackage> results = new ArrayList<>();
        String[] lines = this.packages.split("\\n");
        for (String line : lines) {
            results.add(new AmikoDBPackage(line, this));
        }
        return results;
    }

    public String[] chapterIds() {
        return this.ids_str.split(",");
    }

    public String[] chapterTitles() {
        return this.titles_str.split(";");
    }
}
