
package net.binaryparadox.kerplapp.repo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class App implements Comparable<App> {
    public App() {
        name = "Unknown";
        summary = "Unknown application";
        icon = "noicon.png";
        id = "unknown";
        // antiFeatures = null;
        // requirements = null;
        added = null;
        lastUpdated = null;
        apks = new ArrayList<Apk>();
    }

    public boolean includeInRepo = false;

    public String id;
    public String name;
    public String summary;
    public String icon;

    // Null when !detail_Populated
    public String detail_description;

    public Date added;
    public Date lastUpdated;
    public List<Apk> apks;

    @Override
    public int compareTo(App arg0) {
        return name.compareToIgnoreCase(arg0.name);
    }

}
