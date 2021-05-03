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
import android.test.AndroidTestCase;

/**
 * Tests the {@link SendRequest}.
 * 
 * @author Youtao Liu
 */
public class SendRequestTest extends AndroidTestCase {

  private SendRequest sendRequest;

  private final static String ACCOUNTNAME = "testAccount1";
  private final static String ACCOUNTYPE = "testType1";
  private final static String DRIVE_SHARE_EMAILS = "foo@foo.com";
  private final static String SHARE_URL = "url@url.com";

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    sendRequest = new SendRequest(1);
  }

  /**
   * Tests the method {@link SendRequest#getTrackId()}. The value should be set
   * to 1 when it is initialed in setup method.
   */
  public void testGetTrackId() {
    assertEquals(1, sendRequest.getTrackId());
  }

  /**
   * Tests the method {@link SendRequest#getAccount()}. The value should be set
   * to null which is its default value when it is initialed in setup method.
   */
  public void testGetAccount() {
    assertEquals(null, sendRequest.getAccount());
    Account account = new Account(ACCOUNTNAME, ACCOUNTYPE);
    sendRequest.setAccount(account);
    assertEquals(account, sendRequest.getAccount());
  }

  /**
   * Tests SendRequest.CREATOR.createFromParcel when all values are true.
   */
  public void testCreateFromParcel_true() {
    Parcel parcel = Parcel.obtain();
    parcel.setDataPosition(0);
    parcel.writeLong(2);
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    parcel.writeByte((byte) 1);
    parcel.writeString(DRIVE_SHARE_EMAILS);
    Account account = new Account(ACCOUNTNAME, ACCOUNTYPE);
    parcel.writeParcelable(account, 0);
    parcel.writeByte((byte) 1);
    parcel.writeString(SHARE_URL);
    parcel.setDataPosition(0);
    sendRequest = SendRequest.CREATOR.createFromParcel(parcel);
    assertEquals(2, sendRequest.getTrackId());
    assertTrue(sendRequest.isSendDrive());
    assertTrue(sendRequest.isDriveSync());
    assertTrue(sendRequest.isDriveSharePublic());
    assertEquals(DRIVE_SHARE_EMAILS, sendRequest.getDriveShareEmails());
    assertEquals(account, sendRequest.getAccount());
    assertTrue(sendRequest.isDriveSuccess());
    assertEquals(SHARE_URL, sendRequest.getShareUrl());
  }

  /**
   * Tests SendRequest.CREATOR.createFromParcel when all values are false.
   */
  public void testCreateFromParcel_false() {
    Parcel parcel = Parcel.obtain();
    parcel.setDataPosition(0);
    parcel.writeLong(4);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    parcel.writeByte((byte) 0);
    parcel.writeString(null);
    Account account = new Account(ACCOUNTNAME, ACCOUNTYPE);
    parcel.writeParcelable(account, 0);
    parcel.writeByte((byte) 0);
    parcel.writeString(null);
    parcel.setDataPosition(0);
    sendRequest = SendRequest.CREATOR.createFromParcel(parcel);
    assertEquals(4, sendRequest.getTrackId());
    assertFalse(sendRequest.isSendDrive());
    assertFalse(sendRequest.isDriveSync());
      assertFalse(sendRequest.isDriveSharePublic());
    assertNull(sendRequest.getDriveShareEmails());
    assertEquals(account, sendRequest.getAccount());
    assertFalse(sendRequest.isDriveSuccess());
    assertNull(sendRequest.getShareUrl());
  }

  /**
   * Tests {@link SendRequest#writeToParcel(Parcel, int)} with default values.
   */
  public void testWriteToParcel_default() {
    sendRequest = new SendRequest(1);
    Parcel parcel = Parcel.obtain();
    parcel.setDataPosition(0);
    sendRequest.writeToParcel(parcel, 1);
    parcel.setDataPosition(0);
    long trackId = parcel.readLong();
    boolean sendDrive = parcel.readByte() == 1;
    boolean driveSync = parcel.readByte() == 1;
    boolean driveSharePublic = parcel.readByte() == 1;
    String dirveShareEmails = parcel.readString();
    Parcelable account = parcel.readParcelable(null);
    boolean driveSuccess = parcel.readByte() == 1;
    String shareUrl = parcel.readString();
    assertEquals(1, trackId);
    assertFalse(sendDrive);
    assertFalse(driveSync);
    assertFalse(driveSharePublic);
    assertNull(dirveShareEmails);
    assertNull(account);
    assertFalse(driveSuccess);
    assertNull(shareUrl);
  }

  /**
   * Tests {@link SendRequest#writeToParcel(Parcel, int)}.
   */
  public void testWriteToParcel() {
    sendRequest = new SendRequest(4);
    sendRequest.setSendDrive(true);
    sendRequest.setDriveSync(true);
    sendRequest.setDriveSharePublic(true);
    sendRequest.setDriveShareEmails(DRIVE_SHARE_EMAILS);
    Account accountNew = new Account(ACCOUNTNAME + "2", ACCOUNTYPE + "2");
    sendRequest.setAccount(accountNew);
    sendRequest.setDriveSuccess(true);
    sendRequest.setShareUrl(SHARE_URL);
    Parcel parcel = Parcel.obtain();
    parcel.setDataPosition(0);
    sendRequest.writeToParcel(parcel, 1);
    parcel.setDataPosition(0);
    long trackId = parcel.readLong();
    boolean sendDrive = parcel.readByte() == 1;
    boolean driveSync = parcel.readByte() == 1;
    boolean driveSharePublic = parcel.readByte() == 1;
    String driveShareEmails = parcel.readString();
    Parcelable account = parcel.readParcelable(null);
    boolean driveSuccess = parcel.readByte() == 1;
    String shareUrl = parcel.readString();
    assertEquals(4, trackId);
    assertTrue(sendDrive);
    assertTrue(driveSync);
    assertTrue(driveSharePublic);
    assertEquals(DRIVE_SHARE_EMAILS, driveShareEmails);
    assertEquals(accountNew, account);
    assertTrue(driveSuccess);
    assertEquals(SHARE_URL, shareUrl);
  }
}
