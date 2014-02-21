
package net.binaryparadox.kerplapp;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;
import android.util.Log;
import android.view.Display;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.encode.Contents;
import com.google.zxing.encode.QRCodeEncoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * The UAppIDUtils class provides static utility methods for calculating
 * application UAppID's. Provided a binary file or a package name this class is
 * capable of creating a UAppID returned as a string literal.
 */
public class Utils {
    private static final String TAG = "Utils";

    public static String hashBytes(byte[] input, String algo) {
        try {
            MessageDigest md = MessageDigest.getInstance(algo);
            byte[] hashBytes = md.digest(input);
            String hash = toHexString(hashBytes);

            md.reset();
            return hash;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Device does not support SHA1 MessageDisgest algorithm");
            return null;
        }
    }

    /**
     * Computes the SHA1 hash of a binary file.
     *
     * @param apkPath the path to a binary file to hash.
     * @return the SHA1 hash of the file provided as an argument.
     */
    public static String getBinaryHash(File apk) {
        return getBinaryHash(apk, "SHA1");
    }

    public static String getBinaryHash(File apk, String algo) {
        FileInputStream fis = null;
        try {
            MessageDigest md = MessageDigest.getInstance(algo);
            fis = new FileInputStream(apk);

            byte[] dataBytes = new byte[1024];
            int nread = 0;

            while ((nread = fis.read(dataBytes)) != -1)
                md.update(dataBytes, 0, nread);

            byte[] mdbytes = md.digest();
            return toHexString(mdbytes);
        } catch (IOException e) {
            Log.e(TAG, "Error reading \"" + apk.getAbsolutePath() + "\" to compute SHA1 hash.");
            return null;
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Device does not support SHA1 MessageDisgest algorithm");
            return null;
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (IOException e) {
                    return null;
                }
        }
    }

    /**
     * Computes the base 16 representation of the byte array argument.
     *
     * @param bytes an array of bytes.
     * @return the bytes represented as a string of hexadecimal digits.
     */
    public static String toHexString(byte[] bytes) {
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "X", bi);
    }


    @SuppressWarnings("deprecation")
    @TargetApi(13)
    public static Bitmap generateQrCode(Activity activity, String qrData) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        Point outSize = new Point();
        int x, y, qrCodeDimension;
        /* lame, got to use both the new and old APIs here */
        if (Build.VERSION.SDK_INT > 12) {
            display.getSize(outSize);
            x = outSize.x;
            y = outSize.y;
        } else {
            x = display.getWidth();
            y = display.getHeight();
        }
        if (outSize.x < outSize.y)
            qrCodeDimension = x;
        else
            qrCodeDimension = y;
        Log.i(TAG, "generating QRCode Bitmap of " + qrCodeDimension + "x" + qrCodeDimension);
        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(qrData, null,
                Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), qrCodeDimension);

        try {
            return qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }
}
