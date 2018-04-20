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

import android.os.Bundle;
import android.content.Context;
import android.net.NetworkInfo;
import android.support.v4.app.Fragment;
import android.util.Log;


public class BaseFetcher extends Fragment {
  public static final String TAG = "BaseFetcher";

  protected Context context;

  public interface FetchTaskCallback<T> {
    interface Progress {
      int ERROR = -1;
      int CONNECT_SUCCESS = 0;
      int GET_INPUT_STREAM_SUCCESS = 1;
      int PROCESS_INPUT_STREAM_IN_PROGRESS = 2;
      int PROCESS_INPUT_STREAM_SUCCESS = 3;
    }

    // activity
    NetworkInfo getActiveNetworkInfo();

    void onProgressUpdate(Integer ...progress);

    void updateFromFetch(T result);

    void finishFetching();
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    // pass
  }

  @Override
  public void onAttach(Context context) {
    Log.d(TAG, "(onAttach) context: " + context);
    if (this.context == null) {
      this.context = context;
    }

    super.onAttach(context);
  }

  @Override
  public void onDetach() {
    this.context = null;

    super.onDetach();
  }

  public void cancelFetch() {
    // TODO
  }
}
