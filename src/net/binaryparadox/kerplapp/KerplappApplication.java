
package net.binaryparadox.kerplapp;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import net.binaryparadox.kerplapp.repo.KerplappRepo;

import org.spongycastle.operator.OperatorCreationException;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

public class KerplappApplication extends Application {
    private static final String TAG = "KerplappApplication";
    private static final String keyStoreDirName  = "keystore";
    private static final String keyStoreFileName = "kerplapp.bks";

    private KerplappRepo     repo     = null;
    private KerplappKeyStore keystore = null;

    @Override
    public void onCreate() {
        super.onCreate();

        File appKeyStoreDir = getDir(keyStoreDirName, Context.MODE_PRIVATE);
        File keyStoreFile = new File(appKeyStoreDir, keyStoreFileName);

        if (repo == null) {
            repo = new KerplappRepo(getApplicationContext());

            try {
                repo.init();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

        if (keystore == null) {
            try {
                keystore = new KerplappKeyStore(keyStoreFile);
            } catch (UnrecoverableKeyException e) {
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (CertificateException e) {
                e.printStackTrace();
            } catch (OperatorCreationException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public KerplappRepo getKerplappRepo() {
        return repo;
    }

    public KerplappKeyStore getKeyStore() {
        return keystore;
    }
}
