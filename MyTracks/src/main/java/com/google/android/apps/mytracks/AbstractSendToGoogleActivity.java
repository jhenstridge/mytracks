/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.mytracks;

import com.google.android.apps.mytracks.fragments.ConfirmDeleteDialogFragment;
import com.google.android.apps.mytracks.fragments.ConfirmDeleteDialogFragment.ConfirmDeleteCaller;
import com.google.android.apps.mytracks.fragments.ExportDialogFragment.ExportType;
import com.google.android.apps.mytracks.fragments.InstallEarthDialogFragment;
import com.google.android.apps.mytracks.fragments.ShareTrackDialogFragment;
import com.google.android.apps.mytracks.fragments.ShareTrackDialogFragment.ShareTrackCaller;
import com.google.android.apps.mytracks.io.drive.SendDriveActivity;
import com.google.android.apps.mytracks.io.file.TrackFileFormat;
import com.google.android.apps.mytracks.io.file.exporter.SaveActivity;
import com.google.android.apps.mytracks.io.sendtogoogle.SendRequest;
import com.google.android.apps.mytracks.io.sendtogoogle.SendToGoogleUtils;
import com.google.android.apps.mytracks.io.sendtogoogle.UploadResultActivity;
import com.google.android.apps.mytracks.io.sync.SyncUtils;
import com.google.android.apps.mytracks.services.TrackRecordingServiceConnection;
import com.google.android.apps.mytracks.services.tasks.CheckPermissionAsyncTask;
import com.google.android.apps.mytracks.services.tasks.CheckPermissionAsyncTask.CheckPermissionCaller;
import com.google.android.apps.mytracks.util.AnalyticsUtils;
import com.google.android.apps.mytracks.util.GoogleEarthUtils;
import com.google.android.apps.mytracks.util.IntentUtils;
import com.google.android.apps.mytracks.util.PreferencesUtils;
import com.google.android.apps.mytracks.util.TrackRecordingServiceConnectionUtils;
import com.google.android.maps.mytracks.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

/**
 * An abstract class for the following common tasks across
 * {@link TrackListActivity}, {@link TrackDetailActivity}, and
 * {@link SearchListActivity}:
 * <p>
 * - share track <br>
 * - export track to Google services<br>
 * - enable sync to Google Drive <br>
 * - delete tracks <br>
 * - play tracks
 * 
 * @author Jimmy Shih
 */
public abstract class AbstractSendToGoogleActivity extends AbstractMyTracksActivity
    implements CheckPermissionCaller, ShareTrackCaller, ConfirmDeleteCaller {

  private static final String TAG = AbstractMyTracksActivity.class.getSimpleName();
  private static final String SEND_REQUEST_KEY = "send_request_key";
  private static final int DRIVE_REQUEST_CODE = 0;
  private static final int DELETE_REQUEST_CODE = 3;
  protected static final int GOOGLE_PLAY_SERVICES_REQUEST_CODE = 4;
  protected static final int CAMERA_REQUEST_CODE = 5;

  private SendRequest sendRequest;
  private CheckPermissionAsyncTask asyncTask;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      sendRequest = savedInstanceState.getParcelable(SEND_REQUEST_KEY);
    }
    Object retained = getLastCustomNonConfigurationInstance();
    if (retained instanceof CheckPermissionAsyncTask) {
      asyncTask = (CheckPermissionAsyncTask) retained;
      asyncTask.setActivity(this);
    }
  }

  @Override
  public Object onRetainCustomNonConfigurationInstance() {
    if (asyncTask != null) {
      asyncTask.setActivity(null);
    }
    return asyncTask;
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(SEND_REQUEST_KEY, sendRequest);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case DRIVE_REQUEST_CODE:
        SendToGoogleUtils.cancelNotification(this, SendToGoogleUtils.DRIVE_NOTIFICATION_ID);
        if (resultCode == Activity.RESULT_OK) {
          onDrivePermissionSuccess();
        } else {
          onPermissionFailure();
        }
        break;
      case DELETE_REQUEST_CODE:
        onDeleted();
        break;
      default:
        super.onActivityResult(requestCode, resultCode, data);
    }
  }

  /**
   * Shares a track.
   * 
   * @param trackId the track id
   */
  protected void shareTrack(long trackId) {
    AnalyticsUtils.sendPageViews(this, AnalyticsUtils.ACTION_SHARE_DRIVE);
    ShareTrackDialogFragment.newInstance(trackId)
        .show(getSupportFragmentManager(), ShareTrackDialogFragment.SHARE_TRACK_DIALOG_TAG);
  }

  @Override
  public void onShareTrackDone(long trackId, boolean makePublic, String emails, Account account) {
    sendRequest = new SendRequest(trackId);
    sendRequest.setSendDrive(true);
    sendRequest.setDriveSharePublic(makePublic);
    sendRequest.setDriveShareEmails(emails);
    sendRequest.setAccount(account);
    checkPermissions();
  }

  protected void exportTrackToGoogle(long trackId, ExportType exportType, Account account) {
    sendRequest = new SendRequest(trackId);
    String pageView = AnalyticsUtils.ACTION_EXPORT_DRIVE;
    sendRequest.setSendDrive(true);
    AnalyticsUtils.sendPageViews(this, pageView);
    sendRequest.setAccount(account);
    checkPermissions();
  }
 
  /**
   * Enables Google Drive sync.
   */
  protected void enableSync(Account account) {
    sendRequest = new SendRequest(-1L);
    sendRequest.setSendDrive(true);
    sendRequest.setDriveSync(true);
    sendRequest.setAccount(account);
    checkPermissions();
  }
  
  /**
   * Checks permissions to needed Google services.
   */
  private void checkPermissions() {
    // Check Drive permission
    boolean needDrivePermission = sendRequest.isSendDrive();

    if (needDrivePermission) {
      startCheckPermission(SendToGoogleUtils.DRIVE_SCOPE);
    } else {
      onDrivePermissionSuccess();
    }
  }

  /**
   * Starts checking permission for a Google service.
   * 
   * @param scope the service scope
   */
  private void startCheckPermission(String scope) {
    asyncTask = new CheckPermissionAsyncTask(this, sendRequest.getAccount().name, scope);
    asyncTask.execute();
  }
  
  @Override
  public void onCheckPermissionDone(String scope, boolean success, Intent userRecoverableIntent) {
    asyncTask = null;
    if (success) {
        onDrivePermissionSuccess();
    } else {
      if (userRecoverableIntent != null) {
        startActivityForResult(userRecoverableIntent, DRIVE_REQUEST_CODE);
      } else {
        onPermissionFailure();
      }
    }
  }

  /**
   * On drive permission success. If
   * <p>
   * isSendDrive and isDriveEnableSync -> enable sync
   * <p>
   * isSendDrive -> start {@link SendDriveActivity}
   * <p>
   * else -> start {@link UploadResultActivity}
   */
  private void onDrivePermissionSuccess() {
    Class<?> next;
    if (sendRequest.isSendDrive()) {
      if (sendRequest.isDriveSync()) {
        SyncUtils.enableSync(this);       
        return;
      } else {
        next = SendDriveActivity.class;
      }
    } else {
      next = UploadResultActivity.class;
    }
    Intent intent = IntentUtils.newIntent(this, next)
        .putExtra(SendRequest.SEND_REQUEST_KEY, sendRequest);
    startActivity(intent);
  }

  /**
   * Call when not able to get permission for a google service.
   */
  private void onPermissionFailure() {
    Toast.makeText(this, R.string.send_google_no_account_permission, Toast.LENGTH_LONG).show();
  }
  
  /**
   * Delete tracks.
   * 
   * @param trackIds the track ids
   */
  protected void deleteTracks(long[] trackIds) {
    ConfirmDeleteDialogFragment.newInstance(trackIds)
        .show(getSupportFragmentManager(), ConfirmDeleteDialogFragment.CONFIRM_DELETE_DIALOG_TAG);
  }
  
  @Override
  public void onConfirmDeleteDone(long[] trackIds) {
    boolean stopRecording = false;
    if (trackIds.length == 1 && trackIds[0] == -1L) {
      stopRecording = true;
    } else {
      long recordingTrackId = PreferencesUtils.getLong(this, R.string.recording_track_id_key);
      for (long trackId : trackIds) {
        if (trackId == recordingTrackId) {
          stopRecording = true;
          break;
        }
      }
    }
    if (stopRecording) {
      TrackRecordingServiceConnectionUtils.stopRecording(
          this, getTrackRecordingServiceConnection(), false);
    }
    Intent intent = IntentUtils.newIntent(this, DeleteActivity.class);
    intent.putExtra(DeleteActivity.EXTRA_TRACK_IDS, trackIds);
    startActivityForResult(intent, DELETE_REQUEST_CODE);
  }

  /**
   * Gets the track recording service connection. For stopping the current
   * recording if need to delete the current recording track.
   */
  abstract protected TrackRecordingServiceConnection getTrackRecordingServiceConnection();

  /**
   * Called after {@link DeleteActivity} returns its result.
   */
  abstract protected void onDeleted();

  /**
   * Play tracks in Google Earth.
   * 
   * @param trackIds the track ids
   */
  protected void playTracks(long[] trackIds) {
    AnalyticsUtils.sendPageViews(this, AnalyticsUtils.ACTION_PLAY);
    if (GoogleEarthUtils.isEarthInstalled(this)) {
      Intent intent = IntentUtils.newIntent(this, SaveActivity.class)
          .putExtra(SaveActivity.EXTRA_TRACK_IDS, trackIds)
          .putExtra(SaveActivity.EXTRA_TRACK_FILE_FORMAT, (Parcelable) TrackFileFormat.KML)
          .putExtra(SaveActivity.EXTRA_PLAY_TRACK, true);
      startActivity(intent);
    } else {
      new InstallEarthDialogFragment().show(
          getSupportFragmentManager(), InstallEarthDialogFragment.INSTALL_EARTH_DIALOG_TAG);
    }
  }
}
