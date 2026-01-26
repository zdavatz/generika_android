package org.oddb.generika.model;

import android.database.Cursor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AmikoDBPackage {
    public String name;
    public String dosage;
    public String units;
    public String efp;
    public String pp;
    public String fap;
    public String fep;
    public String vat;
    public String flags;
    public String gtin;
    public String phar;

    public AmikoDBRow parent;

    public AmikoDBPackage(String packageString, AmikoDBRow parent) {
        String[] parts = packageString.split("\\|");
        this.name = parts[0];
        this.dosage = parts[1];
        this.units = parts[2];
        this.efp = parts[3];
        this.pp = parts[4];
        this.fap = parts[5];
        this.fep = parts[6];
        this.vat = parts[7];
        this.flags = parts[8];
        this.gtin = parts[9];
        this.phar = parts[10];
        this.parent = parent;
    }

    public String[] parsedFlags() {
        if (flags == null || flags.isEmpty()) {
            return new String[0];
        }
        return flags.split(",");
    }

    public String selbstbehalt() {
        for (String flag : parsedFlags()) {
            if (flag.startsWith("SB ")) {
                return flag.substring(3);
            }
        }
        return null;
    }

    public boolean isGeneric() {
        for (String flag : parsedFlags()) {
            if ("G".equals(flag)) {
                return true;
            }
        }
        return false;
    }

    public boolean isOriginal() {
        for (String flag : parsedFlags()) {
            if ("O".equals(flag)) {
                return true;
            }
        }
        return false;
    }

    public String parsedDosageFromName() {
        if (this.name == null) return "";
        Pattern regex1 = Pattern.compile("((\\d+)(\\.\\d+)?\\s*(ml|mg|g))");
        Matcher match1 = regex1.matcher(this.name);
        String dosage1 = match1.find() ? match1.group(0) : "";

        Pattern regex2 = Pattern.compile("(((\\d+)(\\.\\d+)?(Ds|ds|mg)?)(\\/(\\d+)(\\.\\d+)?\\s*(Ds|ds|mg|ml|mg|g)?)+)");
        Matcher match2 = regex2.matcher(this.name);
        String dosage2 = match2.find() ? match2.group(0) : "";

        if (dosage1.isEmpty() || dosage2.contains(dosage1)) {
            return dosage2;
        }

        return dosage1;
    }

    public boolean isDosageEqualsTo(AmikoDBPackage other) {
        if (other == null) return false;
        String dosage1 = this.parsedDosageFromName().replace(" ", "");
        String dosage2 = other.parsedDosageFromName().replace(" ", "");
        if (dosage1.equals(dosage2)) {
            return true;
        }
        String numOnly1 = takeNumOnly(dosage1);
        String numOnly2 = takeNumOnly(dosage2);

        boolean is1WithoutUnit = dosage1.toLowerCase().endsWith("ds") || (numOnly1 != null && numOnly1.equals(dosage1));
        boolean is2WithoutUnit = dosage2.toLowerCase().endsWith("ds") || (numOnly2 != null && numOnly2.equals(dosage2));

        if (is1WithoutUnit || is2WithoutUnit) {
            return numOnly1 != null && numOnly1.equals(numOnly2);
        }
        return false;
    }

    public static String takeNumOnly(String str) {
        if (str == null) return null;
        Pattern regex = Pattern.compile("^\\s*(\\d+)");
        Matcher match = regex.matcher(str);
        if (match.find() && match.groupCount() >= 1) {
            return match.group(1);
        }
        return null;
    }
}
