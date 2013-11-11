
package net.binaryparadox.kerplapp;

import android.os.Parcel;
import android.os.Parcelable;

public class AppListEntry implements Parcelable
{
    private String appName;
    private boolean checked;
    private String pkgName;

    public AppListEntry(String pkgName, String appName, boolean checked)
    {
        this.pkgName = pkgName;
        this.appName = appName;
        this.checked = checked;
    }

    public AppListEntry(Parcel p)
    {
        this.pkgName = p.readString();
        this.appName = p.readString();

        boolean[] arr = new boolean[1];
        p.readBooleanArray(arr);
        this.checked = arr[0];
    }

    /**
     * @return the appName
     */
    public String getAppName()
    {
        return appName;
    }

    /**
     * @return the pkgName
     */
    public String getPkgName()
    {
        return pkgName;
    }

    /**
     * @return the checked
     */
    public boolean isChecked()
    {
        return checked;
    }

    /**
     * @param appName the appName to set
     */
    public void setAppName(String appName)
    {
        this.appName = appName;
    }

    /**
     * @param checked the checked to set
     */
    public void setChecked(boolean checked)
    {
        this.checked = checked;
    }

    /**
     * @param pkgName the pkgName to set
     */
    public void setPkgName(String pkgName)
    {
        this.pkgName = pkgName;
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(pkgName);
        dest.writeString(appName);
        dest.writeBooleanArray(new boolean[] {
            checked
        });
    }

    public static Creator<AppListEntry> CREATOR = new Creator<AppListEntry>() {
        public AppListEntry createFromParcel(Parcel parcel) {
            return new AppListEntry(parcel);
        }

        public AppListEntry[] newArray(int size) {
            return new AppListEntry[size];
        }
    };
}
