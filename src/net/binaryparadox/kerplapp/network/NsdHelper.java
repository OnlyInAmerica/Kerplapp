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
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NsdHelper {

    Context mContext;

    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;
    NsdManager.RegistrationListener mRegistrationListener;

    public static final String SERVICE_TYPE = "_http._tcp.";

    public static final String TAG = "NsdHelper";
    public String mServiceName = "Kerplapp";

    public NsdHelper(Context context) {
        mContext = context;
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
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setPort(port);
        serviceInfo.setServiceName(mServiceName);
        serviceInfo.setServiceType(SERVICE_TYPE);
        
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
