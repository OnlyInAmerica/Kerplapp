package net.binaryparadox.kerplapp;

import net.binaryparadox.kerplapp.Utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.util.Log;
import android.widget.Toast;

/**
 * The PackageReceiver class extends {@link android.content.BroadcastReceiver} in order to catch Package install broadcasts.
 * Upon receiving a <b>PACKAGE_ADDED</b> or <b>PACKAGE_REPLACED</b> event the PackageReceiver uses
 * {@link Utils} to calculate the UAppID for the package name bundled in the broadcast intent.
 *
 * @see BroadcastReceiver
 * @see Utils
 */
public class PackageReceiver extends BroadcastReceiver
{
  private static final String TAG                = PackageReceiver.class.getCanonicalName();
  private static final String INTENT_DATA_PREFIX = "package:";
  private static final int    TOAST_DURATION     = 10;

  /* (non-Javadoc)
   * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
   */
  @Override
  public void onReceive(Context context, Intent intent)
  {
    assert(intent != null);

    String pkgName = intent.getDataString();

    //Intent data is formatted with a prefix that must be stripped.
    if(pkgName != null && pkgName.startsWith(INTENT_DATA_PREFIX))
      pkgName = pkgName.substring(INTENT_DATA_PREFIX.length());

    Log.v(TAG, "Received a package intent for package " + pkgName);

    CharSequence text = "New package: " + pkgName;
    Toast toast = Toast.makeText(context.getApplicationContext(), text, TOAST_DURATION);
    toast.show();
  }
}
