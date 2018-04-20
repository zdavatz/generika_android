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
import android.os.AsyncTask;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.io.IOException;

import org.oddb.generika.util.ConnectionStream;
import org.oddb.generika.util.StreamReader;


public class FetchTask<T> extends AsyncTask<String, Integer,
  FetchTask<T>.Result> {
  public static final String TAG = "FetchTask";

  private Class<T> type; // fetch result type

  private Context context;
  private BaseFetcher.FetchTaskCallback<T> fetchTaskCallback;
  private Finalizer<T> finalizer;
  private String notFoundMessage = "";

  FetchTask(Class<T> type, BaseFetcher.FetchTaskCallback<T> callback) {
    this.type = type;
    this.fetchTaskCallback = callback;
  }

  // result object
  public class Result {
    public String object;
    public Exception exception;

    public Result(String object_) {
      this.object = object_;
    }

    public Result(Exception exception) {
      this.exception = exception;
    }
  }

  // result finalizer
  public interface Finalizer<T> {
    abstract public T finalize(T result);
  }

  @Override
  protected void onPreExecute() {
    // cancel if app has not network connectivity
    if (fetchTaskCallback != null) {
      NetworkInfo networkinfo = fetchTaskCallback.getActiveNetworkInfo();
      if (networkinfo == null ||
          !networkinfo.isConnected() ||
          (networkinfo.getType() != ConnectivityManager.TYPE_WIFI &&
           networkinfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
        try {
          int stringId = context.getResources().getIdentifier(
            "no_internet_connection", "string", context.getPackageName()
          );
          String errorMessage = context.getString(stringId);
          Result innerResult = new Result(new Exception(errorMessage));
          T result = newResultInstance(innerResult);
          if (this.finalizer != null) {
            result = finalizer.finalize(result);
          }
          fetchTaskCallback.updateFromFetch(result);
        } catch (Exception e) {  // (unexpected) JSONException
          // don't care
        } finally {
          cancel(true);
        }
      }
    }
  }

  // create FetchResult instance
  private T newResultInstance(FetchTask.Result innerResult)
    throws IOException {
    try {
      Log.d(TAG, "(newResultInstance) innerResult: " + innerResult);
      Log.d(TAG, "(newResultInstance) type: " + type);
      T result;
      // These lines are little bit ugly, but we need to instantiate inner
      // class (FetchResult)
      String outerName = type.getCanonicalName().subSequence(
        0,
        type.getCanonicalName().length() - type.getSimpleName().length() - 1
      ).toString();
      Class outer = Class.forName(outerName);
      Log.d(TAG, "(newResultInstance) outer: " + outer); // fetcher
      try {
        result = type.getConstructor(outer, Result.class).newInstance(
          outer.newInstance(), innerResult);
      } catch (Exception e) {
        // not found
        FetchTask.Result errorResult = new FetchTask.Result(
          new Exception(notFoundMessage));
        result = type.getConstructor(outer, Result.class).newInstance(
          outer.newInstance(), errorResult);
        return result;
      }
      return result;
    } catch(ClassNotFoundException | NoSuchMethodException |
            InstantiationException | IllegalAccessException |
            InvocationTargetException e) {
      // somthing wrong
      Log.d(TAG, "(newResultInstance) " + Log.getStackTraceString(e));
      return null;
    }
  }

  public void setFinalizer(Finalizer<T> finalizer_) {
    this.finalizer = finalizer_;
  }

  public void setContext(Context context_) {
    this.context = context_;
  }

  public void setNotFoundMessage(String message) {
    this.notFoundMessage = message;
  }

  @Override
  protected FetchTask.Result doInBackground(String... urls) {
    Result result = null;

    if (!isCancelled() && urls != null && urls.length > 0) {
      String urlString = urls[0];
      Log.d(TAG, "(doInBackground) urlString: " + urlString);
      try {
        String response = fetch(urlString);
        if (response != null && !response.equals("[]")) {
          // api returns array (not expected)
          Log.d(TAG, "(doInBackground) response.length: " + response.length());
          result = new Result(response);  // inner result
        } else {
          throw new IOException("No response received");
        }
      } catch (Exception e) {  // IOException, JSONException
        Log.d(TAG, "(doInBackground) exception: " + e.getMessage());
        // replace as not found
        result = new Result(new Exception(notFoundMessage));
      }
    }
    return result;
  }

  @Override
  public void onPostExecute(Result innerResult) {
    // main ui thread
    if (fetchTaskCallback != null) {
      try {
        if (innerResult != null &&
            (innerResult.object != null || innerResult.exception != null)) {
          T result = newResultInstance(innerResult);
          Log.d(TAG, "(onPostExecute) finalizer: " + finalizer);
          if (finalizer != null) {
            result = finalizer.finalize(result);
          }
          fetchTaskCallback.updateFromFetch(result);
        }
      } catch (Exception e) { // parse error
        Log.d(TAG, "(onPostExecute) e: " + e.getMessage());
      } finally {
        fetchTaskCallback.finishFetching();
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
      int length = stream.getContentLength();
      if (length > 0) {
        reader.setMaxReadLength(length);
      }
      reader.setStream(stream.derive());
      publishProgress(
        (Integer)BaseFetcher.FetchTaskCallback.Progress.CONNECT_SUCCESS);

      response = reader.read();
      publishProgress(
        (Integer)BaseFetcher.FetchTaskCallback.Progress.GET_INPUT_STREAM_SUCCESS);
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
