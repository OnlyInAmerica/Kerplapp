package net.binaryparadox.kerplapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import android.text.TextUtils.SimpleStringSplitter;
import android.util.Log;

/**
 * The UAppIDUtils class provides static utility methods for calculating application UAppID's.
 * Provided a binary file or a package name this class is capable of creating a UAppID returned
 * as a string literal.
 */
public class Utils
{
  private static final String TAG                   = Utils.class.getCanonicalName();
  private static final String HASH_ALGO             = "SHA1";

  
  /**
   * Computes a hash of the provided byte array, returning it in hex string format.
   * @param input an array of bytes to hash.
   * @return a base 16 string of hexadecimal digits.
   */
  public static String hashBytes(byte[] input)
  {    
    try
    {
      MessageDigest md        = MessageDigest.getInstance(HASH_ALGO);
      byte[]        hashBytes = md.digest(input);     
      String        hash      = toHexString(hashBytes);
      
      md.reset();
      return hash;
    } catch (NoSuchAlgorithmException e) {
      Log.e(TAG, "Device does not support SHA1 MessageDisgest algorithm");
      return null;
    }
  }

  /**
   * Computes the SHA1 hash of a binary file.
   * @param apkPath the path to a binary file to hash.
   * @return the SHA1 hash of the file provided as an argument.
   */ 
  public static String getBinaryHash(File apk)
  {
    try
    {
      //Note: didn't use sha1Hash() here as we're blocking 1024 bytes at a time
      //into the MessageDigest rather than feeding it a stable byte array
      MessageDigest md = MessageDigest.getInstance("SHA1");
      FileInputStream fis = new FileInputStream(apk);
      
      byte[] dataBytes = new byte[1024];
      int nread = 0;
      
      while ((nread = fis.read(dataBytes)) != -1)
        md.update(dataBytes, 0, nread);

      byte[] mdbytes = md.digest();
      return toHexString(mdbytes);
    } catch(IOException e) {
      Log.e(TAG, "Error reading \""+ apk.getAbsolutePath() +"\" to compute SHA1 hash.");
      return null;
    } catch (NoSuchAlgorithmException e) {
      Log.e(TAG, "Device does not support SHA1 MessageDisgest algorithm");
      return null;
    }
  }
  
  /**
   * Computes the base 16 representation of the byte array argument.
   * @param bytes an array of bytes.
   * @return the bytes represented as a string of hexadecimal digits.
   */
  public static String toHexString(byte[] bytes)
  {
    BigInteger bi = new BigInteger(1, bytes);
    return String.format("%0" + (bytes.length << 1) + "X", bi);
  }
  
  public static class CommaSeparatedList implements Iterable<String> {
    private String value;

    private CommaSeparatedList(String list) {
        value = list;
    }

    public static CommaSeparatedList make(String list) {
        if (list == null || list.length() == 0)
            return null;
        else
            return new CommaSeparatedList(list);
    }

    public static String str(CommaSeparatedList instance) {
        return (instance == null ? null : instance.toString());
    }

    public String toString() {
        return value;
    }

    public Iterator<String> iterator() {
        SimpleStringSplitter splitter = new SimpleStringSplitter(',');
        splitter.setString(value);
        return splitter.iterator();
    }
  }
}
