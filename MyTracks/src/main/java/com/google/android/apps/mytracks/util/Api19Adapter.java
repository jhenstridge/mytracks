/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.apps.mytracks.util;

import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TabWidget;

import com.google.android.apps.mytracks.ContextualActionModeCallback;
import com.google.android.apps.mytracks.TrackController;
import com.google.android.apps.mytracks.services.sensors.BluetoothConnectionManager;
import com.google.android.apps.mytracks.widgets.TrackWidgetProvider;
import com.google.android.maps.mytracks.R;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * API level 19 specific implementation of the {@link ApiAdapter}.
 * 
 * @author Jimmy Shih
 */
@TargetApi(19)
public class Api19Adapter implements ApiAdapter {

  private static final String TAG = Api19Adapter.class.getSimpleName();

  @Override
  public void applyPreferenceChanges(SharedPreferences.Editor editor) {
    // Apply asynchronously
    editor.apply();
  }

  @Override
  public void enableStrictMode() {
    Log.d(TAG, "Enabling strict mode");
    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            //.detectAll()
            .detectCustomSlowCalls()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()
            .build());
    StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build());
  }

  @Override
  public byte[] copyByteArray(byte[] input, int start, int end) {
    return Arrays.copyOfRange(input, start, end);
  }

  @Override
  public HttpTransport getHttpTransport() {
    return new NetHttpTransport();
  }

  @Override
  public boolean isGeoCoderPresent() {
    return Geocoder.isPresent();
  }

  @Override
  public boolean revertMenuIconColor() {
    return false;
  }

  @Override
  public BluetoothSocket getBluetoothSocket(BluetoothDevice bluetoothDevice) throws IOException {
    try {
      return bluetoothDevice.createInsecureRfcommSocketToServiceRecord(
              BluetoothConnectionManager.MY_TRACKS_UUID);
    } catch (IOException e) {
      Log.d(TAG, "Unable to create insecure connection", e);
    }
    return bluetoothDevice.createRfcommSocketToServiceRecord(BluetoothConnectionManager.MY_TRACKS_UUID);
  }

  @Override
  public void hideTitle(Activity activity) {
    // Do nothing
  }

  @Override
  public void configureListViewContextualMenu(final Activity activity, final ListView listView,
                                              final ContextualActionModeCallback contextualActionModeCallback) {
    listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
    listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

      @Override
      public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.list_context_menu, menu);
        return true;
      }

      @Override
      public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        contextualActionModeCallback.onPrepare(
                menu, getCheckedPositions(listView), listView.getCheckedItemIds(), true);
        return true;
      }

      @Override
      public void onDestroyActionMode(ActionMode mode) {
        // Do nothing
      }

      @Override
      public void onItemCheckedStateChanged(
              ActionMode mode, int position, long id, boolean checked) {
        mode.invalidate();
      }

      @Override
      public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (contextualActionModeCallback.onClick(
                item.getItemId(), getCheckedPositions(listView), listView.getCheckedItemIds())) {
          mode.finish();
        }
        return true;
      }

      /**
       * Gets the checked positions in a list view.
       *
       * @param list the list view
       */
      private int[] getCheckedPositions(ListView list) {
        SparseBooleanArray positions  = list.getCheckedItemPositions();
        ArrayList<Integer> arrayList = new ArrayList<Integer>();
        for (int i = 0; i < positions.size(); i++) {
          int key = positions.keyAt(i);
          if (positions.valueAt(i)) {
            arrayList.add(key);
          }
        }
        int[] result = new int[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
          result[i] = arrayList.get(i);
        }
        return result;
      }
    });
  }

  @Override
  public boolean handleSearchMenuSelection(Activity activity) {
    // Returns false to allow the platform to expand the search widget.
    return false;
  }

  @Override
  public <T> void addAllToArrayAdapter(ArrayAdapter<T> arrayAdapter, List<T> items) {
    arrayAdapter.addAll(items);
  }

  @Override
  public void invalidMenu(Activity activity) {
    activity.invalidateOptionsMenu();
  }

  @Override
  public void setTabBackground(TabWidget tabWidget) {
    for (int i = 0; i < tabWidget.getChildCount(); i++) {
      tabWidget.getChildAt(i).setBackgroundResource(R.drawable.tab_indicator_mytracks);
    }
  }

  @Override
  public boolean hasDialogTitleDivider() {
    return true;
  }

  @Override
  public void setTitleAndSubtitle(Activity activity, String title, String subtitle) {
    ActionBar actionBar = activity.getActionBar();
    actionBar.setTitle(title);
    actionBar.setSubtitle(subtitle);
  }

  @Override
  public void configureActionBarHomeAsUp(Activity activity) {
    ActionBar actionBar = activity.getActionBar();
    if (actionBar != null) {
      actionBar.setHomeButtonEnabled(true);
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  @Override
  public void configureSearchWidget(
          Activity activity, final MenuItem menuItem, final TrackController trackController) {
    SearchManager searchManager = (SearchManager) activity.getSystemService(Context.SEARCH_SERVICE);
    SearchView searchView = (SearchView) menuItem.getActionView();
    searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
    searchView.setQueryRefinementEnabled(true);
    searchView.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {

      @Override
      public void onFocusChange(View v, boolean hasFocus) {
        // Hide and show trackController when search widget has focus/no focus
        if (trackController != null) {
          if (hasFocus) {
            trackController.hide();
          } else {
            trackController.show();
          }
        }
      }
    });
    searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        menuItem.collapseActionView();
        return false;
      }

      @Override
      public boolean onQueryTextChange(String newText) {
        return false;
      }
    });
    searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
      @Override
      public boolean onSuggestionSelect(int position) {
        return false;
      }

      @Override
      public boolean onSuggestionClick(int position) {
        menuItem.collapseActionView();
        return false;
      }
    });
  }

  @Override
  public boolean handleSearchKey(MenuItem menuItem) {
    menuItem.expandActionView();
    return true;
  }

  @Override
  public boolean isGoogleFeedbackAvailable() {
    return true;
  }

  private static final String APP_WIDGET_SIZE_KEY = "app_widget_size_key";

  @Override
  public int getAppWidgetSize(AppWidgetManager appWidgetManager, int appWidgetId) {
    Bundle bundle = appWidgetManager.getAppWidgetOptions(appWidgetId);
    return bundle.getInt(APP_WIDGET_SIZE_KEY, getAppWidgetSizeDefault(bundle));
  }

  @Override
  public void setAppWidgetSize(AppWidgetManager appWidgetManager, int appWidgetId, int size) {
    Bundle bundle = new Bundle();
    bundle.putInt(APP_WIDGET_SIZE_KEY, size);
    appWidgetManager.updateAppWidgetOptions(appWidgetId, bundle);
  }

  @Override
  public void removeGlobalLayoutListener(
          ViewTreeObserver observer, ViewTreeObserver.OnGlobalLayoutListener listener) {
    observer.removeOnGlobalLayoutListener(listener);
  }

  protected int getAppWidgetSizeDefault(Bundle bundle) {
    boolean isKeyguard = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1)
            == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
    return isKeyguard ? TrackWidgetProvider.KEYGUARD_DEFAULT_SIZE
            : TrackWidgetProvider.HOME_SCREEN_DEFAULT_SIZE;
  }

  @Override
  public boolean hasLocationMode() {
    return true;
  }
}
