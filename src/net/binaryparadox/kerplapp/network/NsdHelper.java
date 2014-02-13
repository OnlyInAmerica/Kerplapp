/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.binaryparadox.kerplapp.network;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NsdHelper {

    // these match FDroid's custom URI schemes
    public static final String HTTP_SERVICE_TYPE =  "_fdroidrepo._tcp.";
    public static final String HTTPS_SERVICE_TYPE = "_fdroidrepos._tcp.";

    public static final String TAG = "NsdHelper";

    private final SharedPreferences prefs;
    private final Context mContext;
    private String mServiceName;

    private NsdManager mNsdManager;
    private NsdManager.RegistrationListener mRegistrationListener;

    public NsdHelper(Context context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
    }

    public void initializeNsd() {
        initializeRegistrationListener();
    }

    public void initializeRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo serviceInfo) {
                mServiceName = serviceInfo.getServiceName();
                Log.i(TAG, "Service registered with name "+ mServiceName);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
              Log.e(TAG, "Error registering service. Error code: "+ errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo serviceInfo) {
              Log.i(TAG, "Successfully unregistered service");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
              Log.e(TAG, "Unable to unregister service. Error code: "+ errorCode);
            }
        };
    }

    public void registerService(int port) {
        String desiredServiceName = prefs.getString("repo_name", "Kerplapp");
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();

        boolean httpsEnabled = prefs.getBoolean("use_https", false);

        serviceInfo.setServiceType(httpsEnabled ? HTTPS_SERVICE_TYPE : HTTP_SERVICE_TYPE);
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(desiredServiceName);


        mNsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
    }

    public void tearDown() {
      try{
        mNsdManager.unregisterService(mRegistrationListener);
      } catch (IllegalArgumentException e) {
        //NOP - Service wasn't registered so we don't have to tear it down.
      }
    }
}
