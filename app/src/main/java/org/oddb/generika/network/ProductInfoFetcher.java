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
package org.oddb.generika.network;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;

import org.oddb.generika.model.Product;
import org.oddb.generika.util.Constant;
import org.oddb.generika.util.ConnectionStream;
import org.oddb.generika.util.StreamReader;


public class ProductInfoFetcher extends Fragment {
  public static final String TAG = "ProductInfoFetcher";

  private Context context;
  private FetchCallback<FetchResult> fetchCallback;
  private FetchTask fetchTask;
  private String baseUrl;

  // Product
  private String id;
  private String ean;

  public interface FetchCallback<T> {
    interface Progress {
      int ERROR = -1;
      int CONNECT_SUCCESS = 0;
      int GET_INPUT_STREAM_SUCCESS = 1;
      int PROCESS_INPUT_STREAM_IN_PROGRESS = 2;
      int PROCESS_INPUT_STREAM_SUCCESS = 3;
    }

    NetworkInfo getActiveNetworkInfo();

    void updateFromFetch(T result);

    void onProgressUpdate(int progressCode, int percentComplete);

    void finishFetching();
  }

  public class FetchResult {
    public String id;
    public HashMap<String, String> map;
    public String errorMessage;

    public FetchResult(FetchTask.Result result) throws JSONException {
      if (result != null) {
        this.id = result.id;

        if (result.object != null) {
          JSONObject object = result.object;;
          // just map all values as string, here
          HashMap<String, String> map = new HashMap<String, String>();
          map.put("seq", object.getString("seq"));
          map.put("name", object.getString("name"));
          // not used (extracted from ean)
          // map.put("pack", object.getString("pack"));
          map.put("size", object.getString("size"));
          map.put("deduction", object.getString("deduction"));
          map.put("price", object.getString("price"));
          map.put("category", object.getString("category"));
          this.map = map;
        }
        if (result.exception != null) {
          this.errorMessage = result.exception.getMessage();
        }
      }
    }
  }

  private class FetchTask extends
    AsyncTask<String, Integer, FetchTask.Result> {

    private FetchCallback<FetchResult> fetchCallback;

    FetchTask(FetchCallback<FetchResult> callback) {
      setCallback(callback);
    }

    void setCallback(FetchCallback<FetchResult> callback) {
      this.fetchCallback = callback;
    }

    // inner result object
    private class Result {
      public String id;
      public JSONObject object;
      public Exception exception;

      public Result(JSONObject object) {
        this.object = object;
      }

      public Result(Exception exception) {
        this.exception = exception;
      }
    }

    @Override
    protected void onPreExecute() {
      // cancel if app has not network connectivity
      if (fetchCallback != null) {
        NetworkInfo networkinfo = fetchCallback.getActiveNetworkInfo();
        if (networkinfo == null ||
            !networkinfo.isConnected() ||
            (networkinfo.getType() != ConnectivityManager.TYPE_WIFI &&
             networkinfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
          try {
            // TODO: replace with translated string
            Result result = new Result(
              new Exception("Keine Verbindung zum Internet!"));
            result.id = id;
            fetchCallback.updateFromFetch(new FetchResult(result));
          } catch (Exception e) {  // (unexpected) JSONException
            // don't care
          } finally {
            cancel(true);
          }
        }
      }
    }

    @Override
    protected FetchTask.Result doInBackground(String... urls) {
      Result result = null;

      if (!isCancelled() && urls != null && urls.length > 0) {
        String urlString = urls[0];
        Log.d(TAG, "(doInBackground) urlString: " + urlString);
        try {
          String response = fetch(urlString);
          Log.d(TAG, "(doInBackground) response: " + response);

          if (response != null) {
            JSONObject json = new JSONObject(response);
            result = new Result(json);  // inner result
          } else {
            throw new IOException("No response received");
          }
        } catch (Exception e) {  // IOException, JSONException
          Log.d(TAG, "(doInBackground) exception: " + e.getMessage());
          // replace as not found
          // TODO: replace with translated string
          String message = String.format(
            "%s\n\"%s\"",
            "Kein Medikament gefunden auf Generika.cc mit dem folgenden EAN-Code:",
            ean);
          result = new Result(new Exception(message));
        }
      }
      return result;
    }

    @Override
    protected void onPostExecute(Result result) {
      // main ui thread
      if (fetchCallback != null) {
        try {
          if (result != null &&
              (result.object != null || result.exception != null)) {
            // inner result to final result (FetchResult)
            FetchResult fetchResult = new FetchResult(result);
            fetchResult.id = id;
            fetchCallback.updateFromFetch(fetchResult);
          }
        } catch (JSONException e) {
          // TODO: parse error
          Log.d(TAG, "(onPostExecute) e: " + e.getMessage());
        } finally {
          fetchCallback.finishFetching();
        }
      }
    }

    @Override
    protected void onCancelled(Result result) {
      // TODO
    }

    private String fetch(String urlString) throws IOException {
      String response = null;
      ConnectionStream stream = null;
      try {
        stream = new ConnectionStream(context);
        stream.setSource(urlString);

        StreamReader reader = new StreamReader();
        reader.setMaxReadLength(500);
        reader.setStream(stream.derive());
        publishProgress(FetchCallback.Progress.CONNECT_SUCCESS);

        response = reader.read();
        publishProgress(FetchCallback.Progress.GET_INPUT_STREAM_SUCCESS, 0);
      } catch (IOException e) {
        Log.d(TAG, "(fetch) e: " + e.getMessage());
        e.printStackTrace();
      } finally {
        if (stream != null) {
          stream.close();
        }
      }
      return response;
    }
  }

  // -- fragment methods

  public static ProductInfoFetcher getInstance(
    FragmentManager fragmentManager, String baseUrl) {

    ProductInfoFetcher fetcher = (ProductInfoFetcher)
      fragmentManager.findFragmentByTag(ProductInfoFetcher.TAG);
    if (fetcher == null) {
      fetcher = new ProductInfoFetcher();
    } else if (fetcher.getArguments() != null) {
      fetcher.getArguments().clear();
    }

    Bundle args = new Bundle();

    // TODO: use constant utility
    args.putString(Constant.kApiKey, "");
    args.putString(Constant.kBaseUrl, baseUrl);

    fetcher.setArguments(args);
    fragmentManager.beginTransaction().add(
      fetcher, TAG).commitAllowingStateLoss();

    return fetcher;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    // pass
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.baseUrl = getArguments().getString(Constant.kBaseUrl);
    Log.d(TAG, "(onCreate) baseUrl: " + baseUrl);

    // retain fragment, even if situation changes
    setRetainInstance(true);
  }

  @Override
  public void onAttach(Context context) {
    this.context = context;
    super.onAttach(context);

    this.fetchCallback = (FetchCallback)context;
  }

  @Override
  public void onDetach() {
    this.fetchCallback = null;

    super.onDetach();
  }

  @Override
  public void onDestroy() {
    cancelFetch();

    super.onDestroy();
  }

  public void invokeFetch(Product product) {
    cancelFetch();
    this.fetchTask = new FetchTask(this.fetchCallback);

    this.id = product.getId();
    this.ean = product.getEan();

    String urlString = baseUrl;
    urlString += ean;

    Log.d(TAG, "(invokeFetch) urlString: " + urlString);
    fetchTask.execute(urlString);
  }

  public void cancelFetch() {
    // TODO
  }
}
