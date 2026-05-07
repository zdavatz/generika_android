/*
 *  Generika Android
 *  Copyright (C) 2026 ywesee GmbH
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package org.oddb.generika.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI for issue #102 — Indikationscode (BAG XXXXX.NN) integration.
 * Drop-in section view for KostengutspracheActivity.
 *
 * Port of iOS IndCSection.swift.
 */
public class IndCSectionView extends LinearLayout {

  // -- Model

  public static class IndCEntry {
    public final String code;   // e.g. "18923.01"
    public final String title;  // first sentence / short label
    public final String text;   // full limitations text

    public IndCEntry(String code, String title, String text) {
      this.code = code;
      this.title = title;
      this.text = text;
    }

    public boolean requiresKoGu() {
      String lower = text != null ? text.toLowerCase() : "";
      return lower.contains("kostengutsprache") || lower.contains("vertrauensarzt");
    }
  }

  /**
   * Parses comma-separated `INDIKATIONSCODE` and newline-separated
   * `INDIKATIONSCODE_TEXT` columns shipped by rust2xml >= 3.1.12 / cpp2sqlite
   * --fhir.
   *
   * Code sections start with `^\d+\.\d+: ` at the beginning of a line.
   * Continuation lines (e.g. multi-paragraph BAG limitations text) are
   * appended to the current code's text. This keeps the full multi-line
   * limitations intact so the "Kostengutsprache erforderlich" detector
   * sees keywords that appear below the first line.
   */
  public static List<IndCEntry> parse(String codeStr, String textStr) {
    List<IndCEntry> result = new ArrayList<>();
    if (codeStr == null || codeStr.isEmpty()) return result;

    String[] codeParts = codeStr.split(",");
    Map<String, String> textByCode = new HashMap<>();
    if (textStr != null) {
      String currentCode = null;
      StringBuilder buf = new StringBuilder();
      for (String line : textStr.split("\n", -1)) {
        String matched = matchCodePrefix(line);
        if (matched != null) {
          if (currentCode != null) {
            textByCode.put(currentCode, buf.toString().trim());
          }
          currentCode = matched;
          buf = new StringBuilder(line.substring(matched.length() + 2));
        } else if (currentCode != null) {
          buf.append('\n').append(line);
        }
      }
      if (currentCode != null) {
        textByCode.put(currentCode, buf.toString().trim());
      }
    }

    for (String c : codeParts) {
      String code = c.trim();
      if (code.isEmpty()) continue;
      String full = textByCode.containsKey(code) ? textByCode.get(code) : "";
      String firstLine = full;
      int nl = firstLine.indexOf('\n');
      if (nl >= 0) firstLine = firstLine.substring(0, nl);
      String title;
      int dot = firstLine.indexOf(". ");
      if (dot >= 0) {
        title = firstLine.substring(0, dot);
      } else if (firstLine.length() > 80) {
        title = firstLine.substring(0, 80) + "…";
      } else {
        title = firstLine;
      }
      result.add(new IndCEntry(code, title, full));
    }
    return result;
  }

  /**
   * If `line` starts with a BAG IndC code prefix `<digits>.<digits>: `,
   * returns the code portion. Otherwise null.
   */
  private static String matchCodePrefix(String line) {
    if (line == null || line.length() < 4) return null;
    int i = 0;
    int len = line.length();
    int dotPos = -1;
    while (i < len) {
      char ch = line.charAt(i);
      if (ch >= '0' && ch <= '9') {
        i++;
      } else if (ch == '.' && dotPos < 0 && i > 0) {
        dotPos = i;
        i++;
      } else {
        break;
      }
    }
    if (dotPos < 0 || i == dotPos + 1) return null; // need digits on both sides
    if (i + 2 > len) return null;
    if (line.charAt(i) != ':' || line.charAt(i + 1) != ' ') return null;
    return line.substring(0, i);
  }

  // -- Listener

  public interface Listener {
    void onIndCEntrySelected(IndCEntry entry);
    void onOffLabelTextChanged(String text);
  }

  // -- View

  private Listener listener;
  private final List<IndCEntry> entries = new ArrayList<>();
  private IndCEntry selectedEntry;
  private String productName = "";
  private String gtin = "";

  private TextView headerLabel;
  private TextView countLabel;
  private TextView productLabel;
  private LinearLayout limContainer;
  private TextView limTextLabel;
  private LinearLayout warningContainer;
  private TextView offLabelHint;
  private EditText offLabelEdit;
  private final List<RadioRow> radioRows = new ArrayList<>();
  private int radiosInsertIndex; // index in this LinearLayout

  public IndCSectionView(Context context) { super(context); setup(); }
  public IndCSectionView(Context context, AttributeSet attrs) { super(context, attrs); setup(); }
  public IndCSectionView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle); setup();
  }

  public void setListener(Listener l) { this.listener = l; }

  public String getSelectedCode() { return selectedEntry != null ? selectedEntry.code : null; }
  public String getSelectedTitle() { return selectedEntry != null ? selectedEntry.title : null; }
  public String getSelectedText() { return selectedEntry != null ? selectedEntry.text : null; }
  public String getOffLabelText() { return offLabelEdit.getText().toString(); }

  private int dp(int v) {
    return (int) TypedValue.applyDimension(
      TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
  }

  private void setup() {
    setOrientation(VERTICAL);

    LinearLayout headerRow = new LinearLayout(getContext());
    headerRow.setOrientation(HORIZONTAL);
    headerRow.setGravity(Gravity.CENTER_VERTICAL);
    headerLabel = new TextView(getContext());
    headerLabel.setText("Indikationscode (IndC)");
    headerLabel.setTextSize(14);
    headerLabel.setTypeface(headerLabel.getTypeface(), android.graphics.Typeface.BOLD);
    headerLabel.setTextColor(0xFF666666);
    LayoutParams hlp = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
    headerRow.addView(headerLabel, hlp);
    countLabel = new TextView(getContext());
    countLabel.setTextSize(12);
    countLabel.setTextColor(0xFF999999);
    headerRow.addView(countLabel,
      new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    addView(headerRow);

    productLabel = new TextView(getContext());
    productLabel.setTextSize(14);
    productLabel.setTextColor(0xFF222222);
    productLabel.setPadding(0, dp(4), 0, 0);
    addView(productLabel);

    radiosInsertIndex = getChildCount(); // radios go here

    limContainer = new LinearLayout(getContext());
    limContainer.setOrientation(VERTICAL);
    GradientDrawable limBg = new GradientDrawable();
    limBg.setColor(0xFFF5F5F5);
    limBg.setCornerRadius(dp(10));
    limBg.setStroke(1, 0xFFDDDDDD);
    limContainer.setBackground(limBg);
    int limPad = dp(12);
    limContainer.setPadding(limPad, dp(10), limPad, dp(10));
    LayoutParams limLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    limLp.topMargin = dp(6);
    limTextLabel = new TextView(getContext());
    limTextLabel.setTextSize(13);
    limTextLabel.setTextColor(0xFF222222);
    limContainer.addView(limTextLabel,
      new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    addView(limContainer, limLp);

    warningContainer = new LinearLayout(getContext());
    warningContainer.setOrientation(VERTICAL);
    GradientDrawable warnBg = new GradientDrawable();
    warnBg.setColor(0x29FF9500); // systemOrange @ ~16%
    warnBg.setCornerRadius(dp(10));
    warnBg.setStroke(1, 0x66FF9500);
    warningContainer.setBackground(warnBg);
    warningContainer.setPadding(limPad, dp(10), limPad, dp(10));
    TextView warnTitle = new TextView(getContext());
    warnTitle.setText("⚠︎ Kostengutsprache erforderlich");
    warnTitle.setTextSize(13);
    warnTitle.setTypeface(warnTitle.getTypeface(), android.graphics.Typeface.BOLD);
    warnTitle.setTextColor(0xFF8C4500);
    warningContainer.addView(warnTitle,
      new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    TextView warnSub = new TextView(getContext());
    warnSub.setText("Vorgängige Konsultation des Vertrauensarztes nötig.");
    warnSub.setTextSize(12);
    warnSub.setTextColor(0xFF666666);
    warningContainer.addView(warnSub,
      new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
    LayoutParams warnLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    warnLp.topMargin = dp(6);
    addView(warningContainer, warnLp);

    offLabelHint = new TextView(getContext());
    offLabelHint.setText("Kein BAG-Indikationscode vorhanden. Falls Off-Label nach KVV Art. 71b/c, Begründung erfassen:");
    offLabelHint.setTextSize(13);
    offLabelHint.setTextColor(0xFF666666);
    LayoutParams hintLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    hintLp.topMargin = dp(4);
    addView(offLabelHint, hintLp);

    offLabelEdit = new EditText(getContext());
    offLabelEdit.setTextSize(14);
    offLabelEdit.setGravity(Gravity.TOP | Gravity.START);
    offLabelEdit.setInputType(android.text.InputType.TYPE_CLASS_TEXT
      | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    GradientDrawable edBg = new GradientDrawable();
    edBg.setColor(Color.TRANSPARENT);
    edBg.setCornerRadius(dp(8));
    edBg.setStroke(1, 0xFFCCCCCC);
    offLabelEdit.setBackground(edBg);
    offLabelEdit.setPadding(dp(8), dp(8), dp(8), dp(8));
    LayoutParams edLp = new LayoutParams(LayoutParams.MATCH_PARENT, dp(80));
    edLp.topMargin = dp(4);
    addView(offLabelEdit, edLp);
    offLabelEdit.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
      @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
      @Override public void afterTextChanged(Editable s) {
        if (listener != null) listener.onOffLabelTextChanged(s.toString());
      }
    });

    updateVisibility();
  }

  /**
   * Configure the section with parsed IndC entries. Pass empty/null
   * code+text strings for products without an IndC (off-label fallback shown).
   */
  public void configure(String productName, String gtin,
                        String codeString, String textString) {
    this.productName = productName != null ? productName : "";
    this.gtin = gtin != null ? gtin : "";
    entries.clear();
    entries.addAll(parse(codeString, textString));
    rebuildRadioRows();
    updateVisibility();

    if (entries.size() == 1) {
      selectEntry(0);
    } else {
      selectedEntry = null;
      limContainer.setVisibility(GONE);
      warningContainer.setVisibility(GONE);
    }
    if (listener != null) listener.onIndCEntrySelected(selectedEntry);
  }

  /** Pre-set the off-label justification (round-tripping a saved receipt). */
  public void setOffLabelText(String text) {
    offLabelEdit.setText(text != null ? text : "");
  }

  /** Re-select an entry by code (round-tripping a saved receipt). */
  public void selectEntryByCode(String code) {
    if (code == null) return;
    for (int i = 0; i < entries.size(); i++) {
      if (code.equals(entries.get(i).code)) {
        selectEntry(i);
        return;
      }
    }
  }

  private void rebuildRadioRows() {
    for (RadioRow r : radioRows) removeView(r);
    radioRows.clear();
    for (int i = 0; i < entries.size(); i++) {
      final int idx = i;
      RadioRow row = new RadioRow(getContext(), entries.get(i));
      row.setOnClickListener(new OnClickListener() {
        @Override public void onClick(View v) { selectEntry(idx); }
      });
      LayoutParams rlp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
      rlp.topMargin = dp(6);
      addView(row, radiosInsertIndex + i, rlp);
      radioRows.add(row);
    }
  }

  private void selectEntry(int idx) {
    if (idx < 0 || idx >= entries.size()) return;
    selectedEntry = entries.get(idx);
    for (int i = 0; i < radioRows.size(); i++) {
      radioRows.get(i).setChecked(i == idx);
    }
    if (selectedEntry != null) {
      limTextLabel.setText(selectedEntry.text);
      limContainer.setVisibility(
        selectedEntry.text == null || selectedEntry.text.isEmpty() ? GONE : VISIBLE);
      warningContainer.setVisibility(selectedEntry.requiresKoGu() ? VISIBLE : GONE);
    }
    if (listener != null) listener.onIndCEntrySelected(selectedEntry);
  }

  private void updateVisibility() {
    boolean hasEntries = !entries.isEmpty();
    productLabel.setVisibility(hasEntries ? VISIBLE : GONE);
    for (RadioRow r : radioRows) r.setVisibility(hasEntries ? VISIBLE : GONE);

    offLabelHint.setVisibility(hasEntries ? GONE : VISIBLE);
    offLabelEdit.setVisibility(hasEntries ? GONE : VISIBLE);

    if (!hasEntries) {
      limContainer.setVisibility(GONE);
      warningContainer.setVisibility(GONE);
      countLabel.setText(productName.isEmpty() ? "" : "0 Indikationen · KVV 71b/c");
      productLabel.setText("");
    } else {
      countLabel.setText(entries.size() == 1
        ? "1 Indikation · auto"
        : entries.size() + " Indikationen");
      String txt = productName;
      if (gtin != null && !gtin.isEmpty()) {
        txt = txt + "  " + gtin;
      }
      productLabel.setText(txt);
    }
  }

  // -- Radio row

  private class RadioRow extends LinearLayout {
    private final IndCEntry entry;
    private final View dot;
    private final View dotInner;
    private boolean checked;

    RadioRow(Context ctx, IndCEntry e) {
      super(ctx);
      this.entry = e;
      setOrientation(VERTICAL);
      setPadding(dp(12), dp(12), dp(12), dp(12));

      LinearLayout topRow = new LinearLayout(ctx);
      topRow.setOrientation(HORIZONTAL);
      topRow.setGravity(Gravity.CENTER_VERTICAL);

      // Radio dot
      LinearLayout dotWrap = new LinearLayout(ctx);
      dotWrap.setGravity(Gravity.CENTER);
      LayoutParams dwLp = new LayoutParams(dp(22), dp(22));
      dotWrap.setLayoutParams(dwLp);
      dot = new View(ctx);
      LayoutParams dotLp = new LayoutParams(dp(22), dp(22));
      dotWrap.addView(dot, dotLp);
      dotInner = new View(ctx);
      dotInner.setVisibility(INVISIBLE);
      LayoutParams diLp = new LayoutParams(dp(8), dp(8));
      dotWrap.addView(dotInner, diLp);
      topRow.addView(dotWrap);

      // Code pill
      TextView codePill = new TextView(ctx);
      codePill.setText(entry.code);
      codePill.setTextSize(12);
      codePill.setTypeface(codePill.getTypeface(), android.graphics.Typeface.BOLD);
      codePill.setTextColor(0xFF222222);
      GradientDrawable codeBg = new GradientDrawable();
      codeBg.setColor(0x1F999999);
      codeBg.setCornerRadius(dp(4));
      codePill.setBackground(codeBg);
      codePill.setPadding(dp(6), dp(2), dp(6), dp(2));
      LayoutParams cpLp = new LayoutParams(
        LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
      cpLp.leftMargin = dp(10);
      topRow.addView(codePill, cpLp);

      // KoGu pill
      if (entry.requiresKoGu()) {
        TextView kogu = new TextView(ctx);
        kogu.setText("KoGu");
        kogu.setTextSize(9);
        kogu.setTypeface(kogu.getTypeface(), android.graphics.Typeface.BOLD);
        kogu.setTextColor(0xFF8C4500);
        GradientDrawable koguBg = new GradientDrawable();
        koguBg.setColor(0x29FF9500);
        koguBg.setCornerRadius(dp(4));
        kogu.setBackground(koguBg);
        kogu.setPadding(dp(6), dp(2), dp(6), dp(2));
        LayoutParams klp = new LayoutParams(
          LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        klp.leftMargin = dp(6);
        topRow.addView(kogu, klp);
      }

      addView(topRow);

      TextView title = new TextView(ctx);
      title.setText(entry.title);
      title.setTextSize(14);
      title.setTextColor(0xFF222222);
      LayoutParams tlp = new LayoutParams(
        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
      tlp.topMargin = dp(4);
      tlp.leftMargin = dp(32); // align with code pill
      addView(title, tlp);

      setChecked(false);
    }

    void setChecked(boolean on) {
      this.checked = on;
      GradientDrawable bg = new GradientDrawable();
      bg.setCornerRadius(dp(8));
      bg.setStroke(1, on ? 0xFF007AFF : 0xFFCCCCCC);
      bg.setColor(on ? 0x14007AFF : Color.TRANSPARENT);
      setBackground(bg);

      GradientDrawable dotBg = new GradientDrawable();
      dotBg.setShape(GradientDrawable.OVAL);
      dotBg.setStroke(dp(2), on ? 0xFF007AFF : 0xFF999999);
      dotBg.setColor(on ? 0xFF007AFF : Color.TRANSPARENT);
      dot.setBackground(dotBg);

      if (on) {
        GradientDrawable inner = new GradientDrawable();
        inner.setShape(GradientDrawable.OVAL);
        inner.setColor(Color.WHITE);
        dotInner.setBackground(inner);
        dotInner.setVisibility(VISIBLE);
      } else {
        dotInner.setVisibility(INVISIBLE);
      }
    }
  }
}
