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

package com.google.android.apps.mytracks.io.sendtogoogle;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Send request states for sending a track to Google Drive, Google Maps, and Google
 * Spreadsheets.
 * 
 * @author Jimmy Shih
 */
public class SendRequest implements Parcelable {

  public static final String SEND_REQUEST_KEY = "sendRequest";

  private long trackId = -1L;
  private boolean sendDrive = false;

  private boolean driveSync = false; // to enable Drive sync 
  private boolean driveSharePublic = false; // for driveShare, share as public
  private String driveShareEmails = null; // for driveShare, emails to share

  private Account account = null;

  private boolean driveSuccess = false;
  private String shareUrl = null;

  /**
   * Creates a new send request.
   * 
   * @param trackId the track id
   */
  public SendRequest(long trackId) {
    this.trackId = trackId;
  }

  public long getTrackId() {
    return trackId;
  }

  public boolean isSendDrive() {
    return sendDrive;
  }

  public void setSendDrive(boolean sendDrive) {
    this.sendDrive = sendDrive;
  }

  public boolean isDriveSync() {
    return driveSync;
  }

  public void setDriveSync(boolean driveSync) {
    this.driveSync = driveSync;
  }

  public boolean isDriveSharePublic() {
    return driveSharePublic;
  }

  public void setDriveSharePublic(boolean driveSharePublic) {
    this.driveSharePublic = driveSharePublic;
  }
  
  public String getDriveShareEmails() {
    return driveShareEmails;
  }

  public void setDriveShareEmails(String driveShareEmails) {
    this.driveShareEmails = driveShareEmails;
  }

  public Account getAccount() {
    return account;
  }

  public void setAccount(Account account) {
    this.account = account;
  }

  public boolean isDriveSuccess() {
    return driveSuccess;
  }

  public void setDriveSuccess(boolean driveSuccess) {
    this.driveSuccess = driveSuccess;
  }

  public String getShareUrl() {
    return shareUrl;
  }

  public void setShareUrl(String shareUrl) {
    this.shareUrl = shareUrl;
  }

  private SendRequest(Parcel in) {
    trackId = in.readLong();
    sendDrive = in.readByte() == 1;
    driveSync = in.readByte() == 1;
    driveSharePublic = in.readByte() == 1;
    driveShareEmails = in.readString();
    account = in.readParcelable(null);
    driveSuccess = in.readByte() == 1;
    shareUrl = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    out.writeLong(trackId);
    out.writeByte((byte) (sendDrive ? 1 : 0));
    out.writeByte((byte) (driveSync ? 1 : 0));
    out.writeByte((byte) (driveSharePublic ? 1 : 0));
    out.writeString(driveShareEmails);
    out.writeParcelable(account, 0);
    out.writeByte((byte) (driveSuccess ? 1 : 0));
    out.writeString(shareUrl);
  }

  public static final Parcelable.Creator<SendRequest>
      CREATOR = new Parcelable.Creator<SendRequest>() {
        public SendRequest createFromParcel(Parcel in) {
          return new SendRequest(in);
        }

        public SendRequest[] newArray(int size) {
          return new SendRequest[size];
        }
      };
}
