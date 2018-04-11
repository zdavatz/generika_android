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
package org.oddb.generika.util;

import android.util.Log;

import java.lang.StringBuffer;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

/**
 *
 * ```
 * # with maxReadLength
 * StreamReader reader = new StreamReader();
 * reader.setMaxReadLength(500);
 * reader.setStream(inputStream);
 * String content = reader.read();
 *
 * # without maxReadLength
 * StreamReader reader = new StreamReader();
 * reader.setStream(inputStream);
 * String content = reader.read();
 * ```
 */
public class StreamReader {
  private static final String TAG = "StreamReader";

  private int maxReadLength = -1;
  private InputStream inputStream;

  public void setMaxReadLength(int maxLength) {
    this.maxReadLength = maxLength;
  }

  public void setStream(InputStream stream) {
    this.inputStream = stream;
  }

  public String read() {
    String content = null;
    try {
      if (inputStream != null) {
        if (maxReadLength != -1) {
          content = readStream(inputStream, maxReadLength);
        } else {
          content = readStream(inputStream);
        }
      }
    } catch (IOException e) {
      Log.d(TAG, "(read) exception: " + e.getMessage());
      e.printStackTrace();
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
          Log.d(TAG, "(read/close) exception: " + e.getMessage());
        } finally {
          this.inputStream = null;
        }
      }
    }
    return content;
  }

  // read with maxReadLength
  private String readStream(InputStream stream, final int maxLength)
    throws IOException, UnsupportedEncodingException {
    Reader reader = new InputStreamReader(stream, "UTF-8");
    char[] rawBuffer = new char[maxLength];
    int length;
    StringBuffer buffer = new StringBuffer();
    while (true) {
      length = reader.read(rawBuffer, 0, maxLength);
      if (length == -1) {
        break;
      }
      buffer.append(rawBuffer, 0, length);
    }
    return buffer.toString();
  }

  // read without maxReadLength
  private String readStream(InputStream stream)
    throws IOException, UnsupportedEncodingException {
    BufferedReader reader = new BufferedReader(
      new InputStreamReader(stream, "UTF-8"));
    StringBuffer buffer = new StringBuffer();
    String line;
    while ((line = reader.readLine()) != null) {
      buffer.append(line);
    }
    return buffer.toString();
  }
}
