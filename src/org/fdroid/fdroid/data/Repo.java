
package org.fdroid.fdroid.data;

import android.content.ContentValues;
import android.database.Cursor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

/**
 * This is a copy of org.fdroid.fdroid.data.Repo so that we can use the same
 * data structure that is used in F-Droid itself, aiding future integration.
 * Parts that are unused in Kerplapp have been unceremoniously ripped out, so
 * don't rely on this copy to be a reference for how F-Droid does it.
 */

public class Repo {

    private long id;

    public String address;
    public String name;
    public String description;
    public int version; // index version, i.e. what fdroidserver built it - 0 if
                        // not specified
    public boolean inuse;
    public int priority;
    public String pubkey; // null for an unsigned repo
    public String fingerprint; // always null for an unsigned repo
    public int maxage; // maximum age of index that will be accepted - 0 for any
    public String lastetag; // last etag we updated from, null forces update
    public Date lastUpdated;

    public Repo() {

    }

    public Repo(Cursor cursor) {
        // not used in Kerplapp, the reads data from the ContentProvider
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return address;
    }

    public int getNumberOfApps() {
        // not used in Kerplapp
        return -1;
    }

    public boolean isSigned() {
        return this.pubkey != null && this.pubkey.length() > 0;
    }

    public boolean hasBeenUpdated() {
        return this.lastetag != null;
    }

    /**
     * If we haven't run an update for this repo yet, then the name will be
     * unknown, in which case we will just take a guess at an appropriate name
     * based on the url (e.g. "fdroid.org/archive")
     */
    public static String addressToName(String address) {
        String tempName;
        try {
            URL url = new URL(address);
            tempName = url.getHost() + url.getPath();
        } catch (MalformedURLException e) {
            tempName = address;
        }
        return tempName;
    }

    private static int toInt(Integer value) {
        if (value == null) {
            return 0;
        } else {
            return value;
        }
    }

    public void setValues(ContentValues values) {
        // not used in Kerplapp
    }
}
