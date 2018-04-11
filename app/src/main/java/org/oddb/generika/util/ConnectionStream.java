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

import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.oddb.generika.util.Constant;


/**
 * A wrapper of URL Connection which handles its response as stream.
 *
 * ```
 * ConnectionStream stream = new ConnectionStream();
 * stream.setSource(urlString);
 * InputStream data = stream.derive();
 * // do something with data
 * stream.close();
 * ```
 */
public class ConnectionStream {
  private static final String TAG = "ConnectionStream";

  private URL sourceUrl;
  private HttpsURLConnection connection;

  private int contentLength = -1;

  public void setSource(String urlString) {
    try {
      this.sourceUrl = new URL(urlString);
    } catch (MalformedURLException e) {
      Log.d(TAG, "(setSource) e: " + e.getMessage());
      this.sourceUrl = null;
    }
  }

  public int getContentLength() {
    return contentLength;
  }

  public InputStream derive() throws IOException {
    if (sourceUrl == null) { return null; };

    // TODO: set user-agent
    this.connection = (HttpsURLConnection)sourceUrl.openConnection();
    connection.setReadTimeout(Constant.HUC_READ_TIMEOUT);
    connection.setConnectTimeout(Constant.HUC_CONNECT_TIMEOUT);
    connection.setRequestMethod("GET");
    connection.setDoInput(true);
    connection.connect();
    int responseCode = connection.getResponseCode();
    Log.d(TAG, "(derive) responseCode: " + responseCode);
    if (responseCode != HttpsURLConnection.HTTP_OK) {
      if (connection != null) {
        connection.disconnect();
      }
      throw new IOException("HTTP error code: " + responseCode);
    }
    int length = connection.getContentLength();
    if (length > 0) {
      this.contentLength = length;
    }
    Log.d(TAG, "(derive) content-length: " + contentLength);

    InputStream stream = connection.getInputStream();
    return stream;
  }

  public void close() {
    if (connection != null) {
      connection.disconnect();
    }
  }
}
