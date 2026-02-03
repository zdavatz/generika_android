package org.oddb.generika.model;

import android.util.Log;

import org.oddb.generika.data.AmikoDBManager;

import java.util.*;

public class AmikoDBPriceComparison {
    public AmikoDBPackage package_;
    public double priceDifferenceInPercentage = 0;

    // Static method to compare prices
    public static ArrayList<AmikoDBPriceComparison> comparePrice(AmikoDBManager manager, String gtin) {
        ArrayList<AmikoDBRow> rows = manager.findWithGtin(gtin, null);
        if (rows == null || rows.isEmpty()) {
            return null;
        }

        Map<String, AmikoDBRow> idToRowDict = new HashMap<>();
        String atc = null;
        AmikoDBPackage thePackage = null;

        for (AmikoDBRow row : rows) {
            idToRowDict.put(row.id, row);
            if (row.atc != null && !row.atc.isEmpty()) {
                atc = row.atc;
            }
            for (AmikoDBPackage pkg : row.parsedPackages()) {
                if (gtin.equals(pkg.gtin)) {
                    thePackage = pkg;
                }
            }
        }

        if (atc == null || atc.isEmpty() || thePackage == null) {
            return null;
        }

        List<AmikoDBRow> comparables = manager.findWithATC(atc);
        for (AmikoDBRow comparableRow : comparables) {
            idToRowDict.put(comparableRow.id, comparableRow);
        }

        double baseQuantity = (thePackage.dosage == null || thePackage.dosage.isEmpty()) ? 0 : Double.parseDouble(thePackage.dosage);
        double basePrice = (thePackage.pp == null || thePackage.pp.isEmpty()) ? 0 : Double.parseDouble(
                thePackage.pp.replace("CHF ", "")
        );

        ArrayList<AmikoDBPriceComparison> results = new ArrayList<>();
        HashSet<String> processedGtin = new HashSet<>();

        for (AmikoDBRow row : idToRowDict.values()) {
            for (AmikoDBPackage pkg : row.parsedPackages()) {
                if (gtin.equals(pkg.gtin) || !pkg.units.equals(thePackage.units)) {
                    continue;
                }
                if (!pkg.isDosageEqualsTo(thePackage)) {
                    continue;
                }

                if (processedGtin.contains(pkg.gtin)) {
                    continue;
                }
                processedGtin.add(pkg.gtin);
                AmikoDBPriceComparison c = new AmikoDBPriceComparison();
                results.add(c);
                c.package_ = pkg;

                double thisQuantity = (pkg.dosage == null || pkg.dosage.isEmpty()) ? 0 : Double.parseDouble(pkg.dosage);
                double thisPrice = (pkg.pp == null || pkg.pp.isEmpty()) ? 0 : Double.parseDouble(
                        pkg.pp.replace("CHF ", "")
                );

                if (basePrice <= 0 || thisPrice <= 0 || baseQuantity <= 0 || thisQuantity <= 0) {
                    // We still add it to the results even when numbers are missing
                    continue;
                }

                // cheaper = negative number
                double diff = thisPrice / (basePrice / baseQuantity * thisQuantity) - 1;
                c.priceDifferenceInPercentage = diff * 100;
            }
        }

        // Prepend thePackage
        AmikoDBPriceComparison selfC = new AmikoDBPriceComparison();
        selfC.package_ = thePackage;
        results.add(0, selfC);

        return results;
    }
}