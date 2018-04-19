package org.oddb.generika;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import android.content.Context;

import java.util.HashMap;

import org.oddb.generika.CustomTestRunner;

import org.oddb.generika.BuildConfig;
import org.oddb.generika.barcode.BarcodeExtractor;



@RunWith(CustomTestRunner.class)
@Config(constants=BuildConfig.class)
public class BarcodeExtractorTest {
  private Context context;

  @Before
  public void setup() throws Exception {
    ShadowLog.stream = System.out;

    this.context = RuntimeEnvironment.application.getApplicationContext();
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testEmptyRawValue() throws Exception {
    String rawValue = "";

    HashMap<String, String> expected = new HashMap<String, String>();
    assertEquals(expected, BarcodeExtractor.extract(rawValue));
  }

  @Test
  public void testFunction1Handling() throws Exception {
    String rawValue;
    HashMap<String, String> expected;

    char fnc1 = (char)29; // gs
    rawValue = fnc1 + "";

    expected = new HashMap<String, String>();
    assertEquals(expected, BarcodeExtractor.extract(rawValue));

    // no FNC1
    rawValue = "";

    expected = new HashMap<String, String>();
    assertEquals(expected, BarcodeExtractor.extract(rawValue));
  }

  @Test
  public void testExtractionWithOneFnc1Char() throws Exception {
    char fnc1 = (char)29; // gs
    String rawValue = fnc1 + "01034531200000111719112510ABCD1234";

    HashMap<String, String> expected = new HashMap<String, String>();
    expected.put("01", "03453120000011");
    expected.put("17", "191125");
    expected.put("10", "ABCD1234");

    assertEquals(expected, BarcodeExtractor.extract(rawValue));
  }

  @Test
  public void testExtractionWithTwoFnc1Chars() throws Exception {
    char fnc1 = (char)29; // gs
    String rawValue = fnc1 + "01095011010209171719050810ABCD1234" +
                      fnc1 + "2110";

    HashMap<String, String> expected = new HashMap<String, String>();
    expected.put("01", "09501101020917");
    expected.put("17", "190508");
    expected.put("10", "ABCD1234");
    expected.put("21", "10");
    assertEquals(expected, BarcodeExtractor.extract(rawValue));
  }

  @Test
  public void testExtractionWithoutNoFNC1() throws Exception {
    // without FNC1 (is valid?)
    String rawValue = "010768065863001417190600101716801";

    HashMap<String, String> expected = new HashMap<String, String>();
    expected.put("01", "07680658630014");
    expected.put("17", "190600");
    expected.put("10", "1716801");
    assertEquals(expected, BarcodeExtractor.extract(rawValue));
  }
}
