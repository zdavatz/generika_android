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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;

import org.oddb.generika.model.Product;
import org.oddb.generika.util.Constant;


public class ProductInfoFetcher extends BaseFetcher implements
  FetchTask.Finalizer<ProductInfoFetcher.FetchResult> {
  public static final String TAG = "ProductInfoFetcher";

  private FetchTaskCallback<FetchResult> fetchTaskCallback;
  private FetchTask<FetchResult> fetchTask;

  // target
  private String baseUrl;
  private Product product;

  public class FetchResult {
    public String id = null;

    public HashMap<String, String> map;
    public String errorMessage;

    public FetchResult(FetchTask<FetchResult>.Result result)
      throws IOException {
      Log.d(TAG, "(FetchResult) result: " + result);
      if (result != null) {
        if (result.exception != null) {
          this.errorMessage = result.exception.getMessage();
          return;
        }
        if (result.object != null) {
          HashMap<String, String> map = new HashMap<String, String>();
          try {
            JSONObject object = new JSONObject(result.object);
            // just map all values as string, here
            map.put("seq", object.getString("seq"));
            map.put("name", object.getString("name"));
            // (pack is extracted from ean)
            // map.put("pack", object.getString("pack"));
            map.put("size", object.getString("size"));
            map.put("deduction", object.getString("deduction"));
            map.put("price", object.getString("price"));
            map.put("category", object.getString("category"));
          } catch (JSONException e) {
            throw new IOException();
          }
          this.map = map;
        }
      }
    }
  }

  @Override
  public FetchResult finalize(FetchResult result) {
    result.id = product.getId();
    return result;
  }

  public static ProductInfoFetcher getInstance(
    FragmentManager fragmentManager, Activity activity, String baseUrl) {

    ProductInfoFetcher fetcher = (ProductInfoFetcher)
      fragmentManager.findFragmentByTag(ProductInfoFetcher.TAG);
    if (fetcher == null) {
      fetcher = new ProductInfoFetcher();
    } else if (fetcher.getArguments() != null) {
      fetcher.getArguments().clear();
    }

    Bundle args = new Bundle();
    args.putString(Constant.kApiKey, "");
    args.putString(Constant.kBaseUrl, baseUrl);

    fetcher.fetchTaskCallback = (FetchTaskCallback)activity;
    fetcher.context = (Context)activity;

    fetcher.setArguments(args);
    fragmentManager.beginTransaction().add(
      fetcher, TAG).commitAllowingStateLoss();

    return fetcher;
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
  public void onDestroy() {
    cancelFetch();
    this.fetchTaskCallback = null;

    super.onDestroy();
  }

  public void invokeFetch(Product product_) {
    cancelFetch();

    this.fetchTask = new FetchTask(FetchResult.class, this.fetchTaskCallback);
    fetchTask.setFinalizer(this);
    fetchTask.setContext(context);

    // not found
    int stringId = context.getResources().getIdentifier(
      "product_not_found", "string", context.getPackageName()
    );
    String errorMessage = String.format(
      context.getString(stringId), product_.getEan());
    fetchTask.setNotFoundMessage(errorMessage);

    this.product = product_;

    String urlString = baseUrl;
    urlString += product.getEan();

    Log.d(TAG, "(invokeFetch) urlString: " + urlString);
    fetchTask.execute(urlString);
  }
}
