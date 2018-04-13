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

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import java.io.IOException;


public class ReceiptFileFetcher extends BaseFetcher implements
  FetchTask.Finalizer<ReceiptFileFetcher.FetchResult> {
  public static final String TAG = "ReceiptFileFetcher";

  private FetchTaskCallback<FetchResult> fetchTaskCallback;
  private FetchTask<FetchResult> fetchTask;

  // target
  private Uri uri;

  public class FetchResult {
    public String url = null;

    public String content;
    public String errorMessage;

    public FetchResult(FetchTask<FetchResult>.Result result)
      throws IOException {
      if (result != null) {
        if (result.object != null) {
          this.content = result.object;
        }
        if (result.exception != null) {
          this.errorMessage = result.exception.getMessage();
        }
      }
    }
  }

  @Override
  public FetchResult finalize(FetchResult result) {
    result.url = uri.toString();
    return result;
  }

  public static ReceiptFileFetcher getInstance(
    FragmentManager fragmentManager, Activity activity) {

    ReceiptFileFetcher fetcher = (ReceiptFileFetcher)
      fragmentManager.findFragmentByTag(ReceiptFileFetcher.TAG);
    if (fetcher == null) {
      fetcher = new ReceiptFileFetcher();
    } else if (fetcher.getArguments() != null) {
      fetcher.getArguments().clear();
    }

    fetcher.fetchTaskCallback = (FetchTaskCallback)activity;
    fetcher.context = (Context)activity;

    fragmentManager.beginTransaction().add(
      fetcher, TAG).commitAllowingStateLoss();

    return fetcher;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // retain fragment, even if situation changes
    setRetainInstance(true);
  }

  @Override
  public void onDestroy() {
    cancelFetch();
    this.fetchTaskCallback = null;

    super.onDestroy();
  }

  public void invokeFetch(Uri uri_) {
    cancelFetch();
    this.fetchTask = new FetchTask(FetchResult.class, this.fetchTaskCallback);
    fetchTask.setFinalizer(this);
    fetchTask.setContext(context);

    // not found
    int stringId = context.getResources().getIdentifier(
      "receipt_not_found", "string", context.getPackageName()
    );
    String errorMessage = context.getString(stringId);
    fetchTask.setNotFoundMessage(errorMessage);

    this.uri = uri_;

    String urlString = uri.toString();
    Log.d(TAG, "(invokeFetch) urlString: " + urlString);
    fetchTask.execute(urlString);
  }
}
