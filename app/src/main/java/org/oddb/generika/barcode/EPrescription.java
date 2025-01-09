package org.oddb.generika.barcode;

import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oddb.generika.util.Hash;
import org.oddb.generika.util.StreamReader;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public class EPrescription {
    private final static String TAG = "EPrescription";

    class PatientId {
        int type;
        String value;
    }
    class PField {
        String nm;
        String value;
    }
    class TakingTime {
        int off;
        int du;
        int doFrom;
        int doTo;
        int a;
        int ma;
    }
    class Posology {
        Date dtFrom;
        Date dtTo;
        int cyDu;
        int inRes;
        ArrayList<Integer> d;
        ArrayList<TakingTime> tt;
    }
    class Medicament {
        String appInstr;
        String medicamentId;
        int idType;
        String unit;
        int rep;
        int nbPack;
        int subs;
        ArrayList<Posology> pos;
    }
    String auth;
    Date date;
    String prescriptionId;
    int medType;
    String zsr;
    ArrayList<PField> pfields;
    String rmk;
    String valBy; // The GLN of the healthcare professional who has validated the medication plan.
    Date valDt; // Date of validation

    String patientFirstName;
    String patientLastName;
    Date patientBirthdate;
    int patientGender;
    String patientStreet;
    String patientCity;
    String patientZip;
    String patientLang; // Patient’s language (ISO 639-19 language code) (e.g. de)
    String patientPhone;
    String patientEmail;
    String patientReceiverGLN;
    ArrayList<PatientId> patientIds;
    ArrayList<PField> patientPFields;

    ArrayList<Medicament> medicaments;
    public EPrescription(String qrCodeString) throws IllegalArgumentException, IOException, JSONException {
        if (qrCodeString.startsWith("https://eprescription.hin.ch")) {
            int sharpIndex = qrCodeString.indexOf("#");
            qrCodeString = qrCodeString.substring(sharpIndex + 1);
            int andIndex = qrCodeString.indexOf("&");
            qrCodeString = qrCodeString.substring(0, andIndex);
        }
        String prefix = "CHMED16A1";
        if (!qrCodeString.startsWith(prefix)) {
            throw new IllegalArgumentException();
        }
        qrCodeString = qrCodeString.substring(prefix.length());
        byte[] gzipped = Base64.decode(qrCodeString, 0);
        ByteArrayInputStream inStream = new ByteArrayInputStream(gzipped);

        GZIPInputStream gzipStream = new GZIPInputStream(inStream);
        StreamReader sr = new StreamReader();
        sr.setStream(gzipStream);
        String jsonStr = sr.read();
        JSONObject obj = new JSONObject(jsonStr);

        this.auth = obj.optString("Auth", "");
        this.date = this.parseDateString(obj.optString("Dt"));
        this.prescriptionId = obj.optString("Id", "");
        this.medType = obj.optInt("MedType", 3); // Default 3 = Prescription
        this.zsr = obj.optString("Zsr", "");
        this.rmk = obj.optString("rmk", "");

        ArrayList<PField> pfs = new ArrayList<>();
        JSONArray pfieldsArray = obj.optJSONArray("PFields");
        for (int i = 0; pfieldsArray != null && i < pfieldsArray.length(); i++) {
            JSONObject pfield = pfieldsArray.optJSONObject(i);
            if (pfield == null) break;
            PField pf = new PField();
            pf.nm = pfield.optString("Nm");
            pf.value = pfield.optString("Val");
            pfs.add(pf);
        }
        this.pfields = pfs;

        JSONObject jsonPatient = obj.optJSONObject("Patient");

        this.patientBirthdate = parseDateString(jsonPatient.optString("BDt"));
        this.patientCity = jsonPatient.optString("City", "");
        this.patientFirstName = jsonPatient.optString("FName", "");
        this.patientLastName = jsonPatient.optString("LName", "");
        this.patientGender = jsonPatient.optInt("Gender", 1);
        this.patientPhone = jsonPatient.optString("Phone", "");
        this.patientStreet = jsonPatient.optString("Street", "");
        this.patientZip = jsonPatient.optString("Zip", "");
        this.patientEmail = jsonPatient.optString("Email", "");
        this.patientReceiverGLN = jsonPatient.optString("Rcv", "");
        this.patientLang = jsonPatient.optString("Lng", "");

        this.patientIds = new ArrayList<>();
        JSONArray patientIds = jsonPatient.optJSONArray("Ids");
        for (int i = 0; patientIds != null && i < patientIds.length(); i++) {
            JSONObject patientId = patientIds.optJSONObject(i);
            if (patientId == null) break;
            PatientId pid = new PatientId();
            pid.value = patientId.optString("Val", "");
            pid.type = patientId.optInt("Type", 1);
            this.patientIds.add(pid);
        }

        this.patientPFields = new ArrayList<>();
        JSONArray patientPFields = jsonPatient.optJSONArray("PFields");
        for (int i = 0; patientPFields != null && i < patientPFields.length(); i++) {
            JSONObject patientPField = patientPFields.optJSONObject(i);
            if (patientPField == null) break;
            PField pField = new PField();
            pField.nm = patientPField.optString("Nm", "");
            pField.value = patientPField.optString("Val", "");
            this.patientPFields.add(pField);
        }

        this.medicaments = new ArrayList<>();
        JSONArray jsonMedicaments = obj.optJSONArray("Medicaments");
        for (int i = 0; jsonMedicaments != null && i < jsonMedicaments.length(); i++) {
            JSONObject jsonMedicament = jsonMedicaments.getJSONObject(i);
            if (jsonMedicament == null) break;
            Medicament m = new Medicament();
            m.appInstr = jsonMedicament.optString("AppInstr", "");
            m.medicamentId = jsonMedicament.optString("Id", "");
            m.idType = jsonMedicament.optInt("IdType", 1); // Default 1 = None
            m.unit = jsonMedicament.optString("Unit", "");
            m.rep = jsonMedicament.optInt("rep", 0);
            m.nbPack = jsonMedicament.optInt("NbPack", 1);
            m.subs = jsonMedicament.optInt("Subs", 0);

            m.pos = new ArrayList<>();
            JSONArray jsonPoses = jsonMedicament.optJSONArray("Pos");
            for (int j = 0; jsonPoses != null && j < jsonPoses.length(); j++) {
                JSONObject jsonPos = jsonPoses.optJSONObject(j);
                if (jsonPos == null) break;
                Posology p = new Posology();
                m.pos.add(p);

                p.dtFrom = this.parseDateString(jsonPos.optString("DtFrom"));
                p.dtTo = this.parseDateString(jsonPos.optString("DtTo"));
                p.cyDu = jsonPos.optInt("CyDu", 0);
                p.inRes = jsonPos.optInt("InRes", 0);

                p.d = new ArrayList<>();
                JSONArray jsonPosD = jsonPos.optJSONArray("D");
                for (int k = 0; jsonPosD != null && k < jsonPosD.length(); k++) {
                    p.d.add(jsonPosD.getInt(k));
                }

                p.tt = new ArrayList<>();
                JSONArray jsonTTs = jsonPos.optJSONArray("TT");
                for (int k = 0; jsonTTs != null && k < jsonTTs.length(); k++) {
                    JSONObject jsonTT = jsonTTs.optJSONObject(k);
                    if (jsonTT == null) break;
                    TakingTime tt = new TakingTime();
                    tt.off = jsonTT.optInt("Off", 0);
                    tt.du = jsonTT.optInt("Du", 0);
                    tt.doFrom = jsonTT.optInt("DoFrom", 0);
                    tt.doTo = jsonTT.optInt("DoTo", tt.doFrom);
                    tt.a = jsonTT.optInt("A", 0);
                    tt.ma = jsonTT.optInt("MA", 0);
                    p.tt.add(tt);
                }
            }
            this.medicaments.add(m);
        }
    }

    private Date parseDateString(String str) {
        if (str == null) return null;

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        // The specification says it's ISO8601, but I got a non-standard date as the sample input
        SimpleDateFormat ePrescriptionDateFormat = new SimpleDateFormat("yyyy-MM-ddHH:mm:ssZ");

        try {
            Date d = isoFormat.parse(str);
            return d;
        } catch (ParseException e) {
            Log.d(TAG, e.getMessage());
        }

        try {
            Date d = ePrescriptionDateFormat.parse(str);
            return d;
        } catch (ParseException e) {
            Log.d(TAG, e.getMessage());
        }

        try {
            Date d = isoDateFormat.parse(str);
            return d;
        } catch (ParseException e) {
            Log.d(TAG, e.getMessage());
        }

        return null;
    }

    public JSONObject amkJSON() throws JSONException {
        SimpleDateFormat birthDateDateFormatter = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat placeDateFormatter = new SimpleDateFormat("dd.MM.yyyy (HH:mm:ss)");

        JSONObject result = new JSONObject();
        JSONArray medicaments = new JSONArray();
        for (Medicament medicament : this.medicaments) {
            JSONObject mDict = new JSONObject();
            mDict.put("eancode", medicament.medicamentId);
            medicaments.put(mDict);
        }
        result.put("medications", medicaments);

        result.put("prescription_hash", UUID.randomUUID().toString());

        // Normally place_date is composed with doctor's name or city,
        // however it's not available in ePrescription, instead we put the ZSR nummber here
        result.put("place_date", String.format("%s,%s", this.zsr, placeDateFormatter.format(this.date)));

        JSONObject operator = new JSONObject();
        operator.put("gln", this.auth); // when null?
        operator.put("zsr_number", this.zsr); // when null?
        result.put("operator", operator);

        JSONObject patient = new JSONObject();
        patient.put("patient_id", generatePatientUniqueID());
        patient.put("given_name", this.patientFirstName);
        patient.put("family_name", this.patientLastName);
        patient.put("birth_date", birthDateDateFormatter.format(this.patientBirthdate));
        patient.put("gender", this.patientGender == 1 ? "M" : "F");
        patient.put("email_address", this.patientEmail);
        patient.put("phone_number", this.patientPhone);
        patient.put("postal_address", this.patientStreet);
        patient.put("city", this.patientCity);
        patient.put("zip_code", this.patientZip);
        patient.put("insurance_gln", this.patientReceiverGLN);
        result.put("patient", patient);

        return result;
    }

    public void importReceipt(Context context) throws IOException, JSONException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm.ss");
        String dateString = format.format(new Date());
        String filename = "RZ_" + dateString.replace(":", "").replace(".", "");
        File f = File.createTempFile(filename, ".amk");
        String p = f.getPath();
        FileOutputStream outputStream = new FileOutputStream(f);
        JSONObject json = this.amkJSON();
        byte[] encoded = Base64.encode(json.toString().getBytes(StandardCharsets.UTF_8), 0);
        outputStream.write(encoded);
        outputStream.close();
        Receipt.importFromFileAndJson(context, Uri.parse(f.toURI().toString()), json);
    }

    private String generatePatientUniqueID() {
        String birthdayString = "";
        SimpleDateFormat birthDateDateFormatter = new SimpleDateFormat("dd.MM.yyyy");
        String[] parts = birthDateDateFormatter.format(this.patientBirthdate).split("\\.");
        for (String part : parts) {
            if (birthdayString.length() > 0) {
                birthdayString += ".";
            }
            birthdayString += Integer.valueOf(part).toString(); // remove leading 0
        }

        String str = String.format("%s.%s.%s", this.patientLastName.toLowerCase(), this.patientFirstName.toLowerCase(), birthdayString);
        try {
            return Hash.sha256(str);
        } catch (Exception e) {
            Log.e("Amiko.Patient", e.toString());
            return  "";
        }
    }

    private String swissKantonFromZip(Context context, String zip) throws IOException, JSONException {
        if (context == null || zip == null) return null;
        InputStream s = context.getAssets().open("swiss-zip-to-kanton.json");
        StreamReader sr = new StreamReader();
        sr.setStream(s);
        String string = sr.read();
        JSONObject jsonObj = new JSONObject(string);
        return jsonObj.optString(zip);
    }
}
