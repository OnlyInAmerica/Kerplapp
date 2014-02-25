
package net.binaryparadox.kerplapp;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import net.binaryparadox.kerplapp.repo.LocalRepo;

import org.spongycastle.operator.OperatorCreationException;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class KerplappApplication extends Application {
    private static final String TAG = "KerplappApplication";
    private static final String keyStoreDirName = "keystore";
    private static final String keyStoreFileName = "kerplapp.bks";

    // the local repo on this device
    static int ipAddress = 0;
    static int port = 8888;
    static String ipAddressString = null;

    private LocalRepo localRepo = null;
    private KerplappKeyStore keystore = null;

    @Override
    public void onCreate() {
        super.onCreate();

        //Apply the Google PRNG fixes to properly seed SecureRandom
        PRNGFixes.apply();

        File appKeyStoreDir = getDir(keyStoreDirName, Context.MODE_PRIVATE);
        File keyStoreFile = new File(appKeyStoreDir, keyStoreFileName);

        if (localRepo == null) {
            localRepo = new LocalRepo(getApplicationContext());

            try {
                localRepo.init();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

        if (keystore == null) {
            try {
                keystore = new KerplappKeyStore(keyStoreFile);
            } catch (UnrecoverableKeyException e) {
                Log.e(TAG, e.getMessage());
            } catch (KeyStoreException e) {
                Log.e(TAG, e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, e.getMessage());
            } catch (CertificateException e) {
                Log.e(TAG, e.getMessage());
            } catch (OperatorCreationException e) {
                Log.e(TAG, e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    public LocalRepo getLocalRepo() {
        return localRepo;
    }

    public KerplappKeyStore getKeyStore() {
        return keystore;
    }
}
