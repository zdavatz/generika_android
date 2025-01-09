package org.oddb.generika.model;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.oddb.generika.barcode.EPrescription;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ZurRosePrescription {
    public static class Address {
        public String title; // optional
        public int titleCode = -1; // optional
        public String lastName;
        public String firstName; // optional
        public String street;
        public String zipCode;
        public String city;
        public String kanton; // optional
        public String country; // optional
        public String phoneNrBusiness; // optional
        public String phoneNrHome; // optional
        public String faxNr; // optional
        public String email; // optional

        void writeBodyToXMLElement(Element e) {
            if (this.title != null) {
                e.addAttribute("title", this.title);
            };

            if (this.titleCode != -1) {
                e.addAttribute("titleCode", String.valueOf(this.titleCode));
            }

            e.addAttribute("lastName", this.lastName);

            if (this.firstName != null) {
                e.addAttribute("firstName", this.firstName);
            };

            e.addAttribute("street", this.street);
            e.addAttribute("zipCode", this.zipCode != null ? this.zipCode : "");
            e.addAttribute("city", this.city);

            e.addAttribute("kanton", this.kanton != null ? this.kanton : "");
            if (this.country != null) {
                e.addAttribute("country", this.country);
            };
            if (this.phoneNrBusiness != null) {
                e.addAttribute("phoneNrBusiness", this.phoneNrBusiness);
            };
            if (this.phoneNrHome != null) {
                e.addAttribute("phoneNrHome", this.phoneNrHome);
            };
            if (this.faxNr != null) {
                e.addAttribute("faxNr", this.faxNr);
            };
            if (this.email != null) {
                e.addAttribute("email", this.email);
            };
        }
    }
    public static class PatientAddress extends Address {
        public Date birthday;
        public int langCode; // 1 = de, 2 = fr, 3 = it
        public String coverCardId; // optional
        public int sex; // 1 = m, 2 = f
        public String patientNr;
        public String phoneNrMobile; // optional
        public String room; // optional
        public String section; // optional

        void toXML(Element parent) {
            Element e = parent.addElement("patientAddress");
            super.writeBodyToXMLElement(e);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

            e.addAttribute("birthday", this.birthday == null ? "" : dateFormat.format(this.birthday));

            e.addAttribute("langCode", String.valueOf(this.langCode));

            if (this.coverCardId != null) {
                e.addAttribute("coverCardId", this.coverCardId);
            }
            
            e.addAttribute("sex", String.valueOf(this.sex));
            e.addAttribute("patientNr", this.patientNr);

            if (this.phoneNrMobile != null) {
                e.addAttribute("phoneNrMobile", this.phoneNrMobile);
            }
            if (this.room != null) {
                e.addAttribute("room", this.room);
            }
            if (this.section != null) {
                e.addAttribute("section", this.section);
            }
        }
    }
    public static class PrescriptorAddress extends Address {
        public int langCode; // 1 = de, 2 = fr, 3 = it
        public String clientNrClustertec;
        public String zsrId;
        public String eanId; // optional

        void toXML(Element parent) {
            Element e = parent.addElement("patientAddress");
            super.writeBodyToXMLElement(e);
            e.addAttribute("langCode", String.valueOf(this.langCode));
            e.addAttribute("clientNrClustertec", this.clientNrClustertec);
            e.addAttribute("zsrId", this.zsrId);
            if (this.eanId != null) {
                e.addAttribute("eanId", this.eanId);
            }
        }
    }
    public static class Posology {
        public int qtyMorning = -1; // optional, -1 = null
        public int qtyMidday = -1; // optional, -1 = null
        public int qtyEvening = -1; // optional, -1 = null
        public int qtyNight = -1; // optional, -1 = null
        public String qtyMorningString; // optional
        public String qtyMiddayString; // optional
        public String qtyEveningString; // optional
        public String qtyNightString; // optional
        public String posologyText; // optional
        public int label; // optional, boolean, -1 = null

        void toXML(Element parent) {
            Element e = parent.addElement("posology");

            if (this.qtyMorning != -1) {
                e.addAttribute("qtyMorning", String.valueOf(this.qtyMorning));
            }
            if (this.qtyMidday != -1) {
                e.addAttribute("qtyMidday", String.valueOf(this.qtyMidday));
            }
            if (this.qtyEvening != -1) {
                e.addAttribute("qtyEvening", String.valueOf(this.qtyEvening));
            }
            if (this.qtyNight != -1) {
                e.addAttribute("qtyNight", String.valueOf(this.qtyNight));
            }
            if (this.qtyMorningString != null) {
                e.addAttribute("qtyMorningString", this.qtyMorningString);
            }
            if (this.qtyMiddayString != null) {
                e.addAttribute("qtyMiddayString", this.qtyMiddayString);
            }
            if (this.qtyEveningString != null) {
                e.addAttribute("qtyEveningString", this.qtyEveningString);
            }
            if (this.qtyNightString != null) {
                e.addAttribute("qtyNightString", this.qtyNightString);
            }
            if (this.posologyText != null) {
                e.addAttribute("posologyText", this.posologyText);
            }
            if (this.label != -1) {
                e.addAttribute("label", this.label == 1 ? "true" : "false");
            }
        }
    }
    public static class Product {
        public String pharmacode; // optional
        public String eanId; // optional
        public String description_; // optional
        public boolean repetition;
        public int nrOfRepetitions = -1; // optional, 0 - 99
        public int quantity; // 0 - 999
        public String validityRepetition; // optional
        public int notSubstitutableForBrandName = -1; // optional
        public String remark; // optional
        public int dailymed = -1; // optional boolean
        public int dailymed_mo = -1; // optional boolean
        public int dailymed_tu = -1; // optional boolean
        public int dailymed_we = -1; // optional boolean
        public int dailymed_th = -1; // optional boolean
        public int dailymed_fr = -1; // optional boolean
        public int dailymed_sa = -1; // optional boolean
        public int dailymed_su = -1; // optional boolean

        public String insuranceEanId; // optional
        public String insuranceBsvNr; // optional
        public String insuranceInsuranceName; // optional
        public int insuranceBillingType; // required
        public String insuranceInsureeNr; // optional

        public ArrayList<Posology> posologies;

        void toXML(Element parent) {
            Element e = parent.addElement("product");

            if (this.pharmacode != null) {
                e.addAttribute("pharmacode", this.pharmacode);
            }
            if (this.eanId != null) {
                e.addAttribute("eanId", this.eanId);
            }
            if (this.description_ != null) {
                e.addAttribute("description", this.description_);
            }
            e.addAttribute("repetition", this.repetition ? "true" : "false");
            if (this.nrOfRepetitions >= 0) {
                e.addAttribute("nrOfRepetitions", String.valueOf(this.nrOfRepetitions));
            }
            e.addAttribute("quantity", String.valueOf(this.quantity));
            if (this.validityRepetition != null) {
                e.addAttribute("validityRepetition", this.validityRepetition);
            }
            if (this.notSubstitutableForBrandName >= 0) {
                e.addAttribute("notSubstitutableForBrandName", String.valueOf(this.notSubstitutableForBrandName));
            }
            if (this.remark != null) {
                e.addAttribute("remark", this.remark);
            }
            if (this.dailymed != -1) {
                e.addAttribute("dailymed", this.dailymed == 1 ? "true" : "false");
            }
            if (this.dailymed_mo != -1) {
                e.addAttribute("dailymed_mo", this.dailymed_mo == 1 ? "true" : "false");
            }
            if (this.dailymed_tu != -1) {
                e.addAttribute("dailymed_tu", this.dailymed_tu == 1 ? "true" : "false");
            }
            if (this.dailymed_we != -1) {
                e.addAttribute("dailymed_we", this.dailymed_we == 1 ? "true" : "false");
            }
            if (this.dailymed_th != -1) {
                e.addAttribute("dailymed_th", this.dailymed_th == 1 ? "true" : "false");
            }
            if (this.dailymed_fr != -1) {
                e.addAttribute("dailymed_fr", this.dailymed_fr == 1 ? "true" : "false");
            }
            if (this.dailymed_sa != -1) {
                e.addAttribute("dailymed_sa", this.dailymed_sa == 1 ? "true" : "false");
            }
            if (this.dailymed_su != -1) {
                e.addAttribute("dailymed_su", this.dailymed_su == 1 ? "true" : "false");
            }

            Element insurance = e.addElement("insurance");

            if (this.insuranceEanId != null) {
                insurance.addAttribute("eanId", this.insuranceEanId);
            }
            if (this.insuranceBsvNr != null) {
                insurance.addAttribute("bsvNr", this.insuranceBsvNr);
            }
            if (this.insuranceInsuranceName != null) {
                insurance.addAttribute("insuranceName", this.insuranceInsuranceName);
            }

            insurance.addAttribute("billingType", String.valueOf(this.insuranceBillingType));

            if (this.insuranceInsureeNr != null) {
                insurance.addAttribute("insureeNr", this.insuranceInsureeNr);
            }

            for (Posology p : this.posologies) {
                p.toXML(e);
            }
        }
    }

    public enum DeliveryType {
        Patient(1), Doctor(2), Address(3);
        private final int value;
        DeliveryType(int v) {
            this.value = v;
        }
    };

    public Date issueDate;
    public Date validity;
    public String user;
    public String password;
    public String prescriptionNr; // optional
    public DeliveryType deliveryType;
    public boolean ignoreInteractions;
    public boolean interactionsWithOldPres;
    public String remark; // optional

    public PrescriptorAddress prescriptorAddress;
    public PatientAddress patientAddress;
    //deliveryAddress // optional
    //billingAddress // optional
    //dailymed // optional

    public ArrayList<Product> products;

    public Document toXML() {
        Document document = DocumentHelper.createDocument();
        Element root = document.addElement("prescription");

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        root.addAttribute("issueDate", format.format(this.issueDate));
        root.addAttribute("validity", this.validity != null ? format.format(this.validity): "");
        root.addAttribute("user", this.user);
        root.addAttribute("password", this.password);
        if (this.prescriptionNr != null) {
            root.addAttribute("prescriptionNr", this.prescriptionNr);
        }

        root.addAttribute("deliveryType", String.valueOf(this.deliveryType.value));

        root.addAttribute("ignoreInteractions", this.ignoreInteractions ? "true" : "false");
        root.addAttribute("interactionsWithOldPres", this.interactionsWithOldPres ? "true" : "false");

        if (this.remark != null) {
            root.addAttribute("remark", this.remark);
        }

        if (this.prescriptorAddress != null) {
            this.prescriptorAddress.toXML(root);
        }
        if (this.patientAddress != null) {
            this.patientAddress.toXML(root);
        }

        for (Product product : this.products) {
            product.toXML(root);
        }
        return document;
    }
}
