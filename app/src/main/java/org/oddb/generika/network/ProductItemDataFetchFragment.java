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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.lang.StringBuffer;
import javax.net.ssl.HttpsURLConnection;

import org.oddb.generika.model.ProductItem;


public class ProductItemDataFetchFragment extends Fragment {

  public static final String TAG = "ProductItemDataFetchFragment";

  private static final String kAPI_KEY = "ApiKey";
  private static final String kBASE_URL = "BaseUrl";

  private FetchCallback fetchCallback;
  private FetchTask fetchTask;
  private String baseUrl;
  private String eanCode;


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
    public ProductItem item;
    public String errorMessage;

    public FetchResult(FetchTask.Result result) {
      if (result != null) {
        if (result.productItem != null) {
          this.item = result.productItem;
        }
        if (result.exception != null) {
          this.errorMessage = result.exception.getMessage();
        }
      }
    }
  }

  private class FetchTask extends
    AsyncTask<String, Integer, FetchTask.Result> {

    private FetchCallback fetchCallback;

    FetchTask(FetchCallback<FetchResult> callback) {
      setCallback(callback);
    }

    void setCallback(FetchCallback<FetchResult> callback) {
      this.fetchCallback = callback;
    }

    // inner result object
    private class Result {
      public ProductItem productItem;
      public Exception exception;

      public Result(ProductItem productItem) {
        this.productItem = productItem;
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
          Result result = new Result(
            new Exception("Keine Verbindung zum Internet!"));
          fetchCallback.updateFromFetch(new FetchResult(result));
          cancel(true);
        }
      }
    }

    @Override
    protected FetchTask.Result doInBackground(String... urls) {
      Result result = null;

      if (!isCancelled() && urls != null && urls.length > 0) {
        Log.d(TAG, "(doInBackground) urls[0]: " + urls[0]);
        try {
          URL url = new URL(urls[0]);
          String resultString = fetchUrl(url);
          Log.d(TAG, "(doInBackground) resultString: " + resultString);
          if (resultString != null) {
            ProductItem productItem = new ProductItem();
            // parse JSON
            JSONObject jsonObj = new JSONObject(resultString);
            productItem.setName(jsonObj.getString("name"));
            productItem.setSize(jsonObj.getString("size"));
            productItem.setDeduction(jsonObj.getString("deduction"));
            productItem.setPrice(jsonObj.getString("price"));
            productItem.setCategory(jsonObj.getString("category"));
            productItem.setEan(eanCode);
            // TODO: parse JSON
            // resultString
            result = new Result(productItem);
          } else {
            throw new IOException("No response received");
          }
        } catch(Exception e) {
          // IOException, JSONException
          Log.d(TAG, "(doInBackground) exception: " + e.getMessage());
          // replace as not found
          String message = String.format(
            "%s\n\"%s\"",
            "Kein Medikament gefunden auf Generika.cc mit dem folgenden EAN-Code:",
            eanCode);
          result = new Result(new Exception(message));
        }
      }
      return result;
    }

    @Override
    protected void onPostExecute(Result result) {
      if (fetchCallback != null) {
        if (result != null) {
          if (result.productItem != null || result.exception != null) {
            fetchCallback.updateFromFetch(new FetchResult(result));
          }
        }
        fetchCallback.finishFetching();
      }
    }

    @Override
    protected void onCancelled(Result result) {
      // TODO
    }

    private String fetchUrl(URL url) throws IOException {
      InputStream stream = null;
      HttpsURLConnection conn = null;
      String response = null;

      try {
        conn = (HttpsURLConnection)url.openConnection();
        conn.setReadTimeout(3000);
        conn.setConnectTimeout(3000);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();
        publishProgress(FetchCallback.Progress.CONNECT_SUCCESS);
        int responseCode = conn.getResponseCode();
        if (responseCode != HttpsURLConnection.HTTP_OK) {
          throw new IOException("HTTP error code: " + responseCode);
        }
        stream = conn.getInputStream();
        publishProgress(FetchCallback.Progress.GET_INPUT_STREAM_SUCCESS, 0);
        if (stream != null) {
          response = readStream(stream, 500);
        }
      } finally {
        if (stream != null) {
          stream.close();
        }
        if (conn != null) {
          conn.disconnect();
        }
      }
      return response;
    }

    private String readStream(InputStream stream, int maxReadLength)
      throws IOException, UnsupportedEncodingException {
      Reader reader = null;
      reader = new InputStreamReader(stream, "UTF-8");
      char[] rawBuffer = new char[maxReadLength];
      int readLength;

      StringBuffer buffer = new StringBuffer();
      while (((readLength = reader.read(rawBuffer)) != -1) &&
             maxReadLength > 0) {
        if (readLength > maxReadLength) {
          readLength = maxReadLength;
        }
        buffer.append(rawBuffer, 0, readLength);
        maxReadLength -= readLength;
      }
      return buffer.toString();
    }
  }


  // -- fragment methods

  public static ProductItemDataFetchFragment getInstance(
    FragmentManager fragmentManager, String baseUrl) {

    ProductItemDataFetchFragment fragment = (ProductItemDataFetchFragment)
      fragmentManager.findFragmentByTag(ProductItemDataFetchFragment.TAG);
    if (fragment == null) {
      fragment = new ProductItemDataFetchFragment();
    } else if (fragment.getArguments() != null) {
      fragment.getArguments().clear();
    }

    Bundle args = new Bundle();

    // TODO: use constant utility
    args.putString(kAPI_KEY, "");
    args.putString(kBASE_URL, baseUrl);

    fragment.setArguments(args);
    fragmentManager.beginTransaction().add(
      fragment, TAG).commitAllowingStateLoss();

    return fragment;
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    // pass
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    this.baseUrl = getArguments().getString(kBASE_URL);
    Log.d(TAG, "(onCreate) baseUrl: " + baseUrl);

    // retain fragment,even if situation changes
    setRetainInstance(true);
  }

  @Override
  public void onAttach(Context context) {
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

  public void invokeFetch(String ean) {
    cancelFetch();
    this.fetchTask = new FetchTask(this.fetchCallback);
    this.eanCode = ean;

    String urlString = baseUrl;
    urlString += this.eanCode;

    Log.d(TAG, "(invokeFetch) urlString: " + urlString);
    fetchTask.execute(urlString);
  }

  public void cancelFetch() {
    // TODO
  }
}
