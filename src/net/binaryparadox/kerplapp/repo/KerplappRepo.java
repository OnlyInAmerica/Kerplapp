
package net.binaryparadox.kerplapp.repo;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import net.binaryparadox.kerplapp.KerplappApplication;
import net.binaryparadox.kerplapp.KerplappKeyStore;
import net.binaryparadox.kerplapp.Utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class KerplappRepo {
    private static final String TAG = "KerplappRepo";

    // For ref, official F-droid repo presently uses a maxage of 14 days
    private static final String DEFAULT_REPO_MAX_AGE_DAYS = "14";

    private final PackageManager pm;
    private final KerplappApplication appCtx;
    private final AssetManager assetManager;
    private final SharedPreferences prefs;

    private String ipAddressString = "UNSET";
    private String uriString = "UNSET";

    private Map<String, App> apps = new HashMap<String, App>();

    private File xmlIndex = null;
    private File xmlIndexJar = null;
    private File xmlIndexJarUnsigned = null;
    public File webRoot = null;
    public File fdroidDir = null;
    public File repoDir = null;
    public File iconsDir = null;

    public KerplappRepo(Context c) {
        webRoot = c.getFilesDir();
        pm = c.getPackageManager();
        appCtx = (KerplappApplication) c.getApplicationContext();
        assetManager = c.getAssets();
        prefs = PreferenceManager.getDefaultSharedPreferences(c);
    }

    public File getRepoDir() {
        return repoDir;
    }

    public File getIndex() {
        return xmlIndex;
    }

    public void setIpAddressString(String ipAddressString) {
        this.ipAddressString = ipAddressString;
    }

    public void setUriString(String uriString) {
        this.uriString = uriString;
    }

    public void init() throws Exception {
        /* /fdroid/repo is the standard path for user repos */
        fdroidDir = new File(webRoot, "fdroid");
        if (!fdroidDir.exists())
            if (!fdroidDir.mkdir())
                throw new IllegalStateException("Unable to create empty base: " + fdroidDir);

        repoDir = new File(fdroidDir, "repo");
        Log.i(TAG, "init in " + repoDir);
        if (!repoDir.exists())
            if (!repoDir.mkdir())
                throw new IllegalStateException("Unable to create empty repo: " + repoDir);

        iconsDir = new File(repoDir, "icons");
        if (!iconsDir.exists())
            if (!iconsDir.mkdir())
                throw new IllegalStateException("Unable to create icons folder: " + iconsDir);

        xmlIndex = new File(repoDir, "index.xml");
        xmlIndexJar = new File(repoDir, "index.jar");
        xmlIndexJarUnsigned = new File(repoDir, "index.unsigned.jar");

        if (!xmlIndex.exists())
            if (!xmlIndex.createNewFile())
                throw new IllegalStateException("Unable to create empty index.xml file");
    }

    public void writeIndexPage(Uri repoUri)
    {
        String fdroidPkg = "org.fdroid.fdroid";
        ApplicationInfo appInfo;

        String fdroidClientURL = "https://f-droid.org/FDroid.apk";

        try {
            appInfo = pm.getApplicationInfo(fdroidPkg, PackageManager.GET_META_DATA);
            File apkFile = new File(appInfo.publicSourceDir);
            File fdroidApkLink = new File(webRoot, "fdroid.client.apk");
            fdroidApkLink.delete();
            if (copyFile(apkFile.getAbsolutePath(), fdroidApkLink))
                fdroidClientURL = "/" + fdroidApkLink.getName();
        } catch (NameNotFoundException e) {
            // nop
        }

        try {
            File indexHtml = new File(webRoot, "index.html");
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(assetManager.open("index.template.html"), "UTF-8"));
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(indexHtml)));

            while (in.ready()) { //
                String line = in.readLine();
                line = line.replaceAll("\\{\\{REPO_URL\\}\\}", repoUri.toString());
                line = line.replaceAll("\\{\\{CLIENT_URL\\}\\}", fdroidClientURL);
                out.write(line);
            }
            in.close();
            out.close();
            // make symlinks/copies in each subdir of the repo to make sure that
            // the user will always find the bootstrap page.
            File fdroidDirIndex = new File(fdroidDir, "index.html");
            fdroidDirIndex.delete();
            copyFile(indexHtml.getCanonicalPath(), fdroidDirIndex);
            File repoDirIndex = new File(repoDir, "index.html");
            repoDirIndex.delete();
            copyFile(indexHtml.getCanonicalPath(), repoDirIndex);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void deleteContents(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteContents(files[i]);
                }
                else {
                    files[i].delete();
                }
            }
        }
    }

    public void deleteRepo() {
        deleteContents(repoDir);
    }

    public void copyApksToRepo() {
        copyApksToRepo(new ArrayList<String>(apps.keySet()));
    }

    public void copyApksToRepo(List<String> appsToCopy) {
        for (String packageName : appsToCopy) {
            App app = apps.get(packageName);

            for (Apk apk : app.apks) {
                File outFile = new File(repoDir, apk.apkName);
                if (!copyFile(apk.apkSourcePath, outFile)) {
                    throw new IllegalStateException("Unable to copy APK");
                }
            }
        }
    }

    public static boolean hasApi(int apiLevel) {
        return Build.VERSION.SDK_INT >= apiLevel;
    }

    public static int getApi() {
        return Build.VERSION.SDK_INT;
    }

    public static boolean copyFile(String inFileName, File outFile) {
        /* use symlinks if they are available, otherwise fall back to copying */
        if (new File("/system/bin/ln").exists()) {
            return doSymLink(inFileName, outFile);
        } else {
            return doCopyFile(inFileName, outFile);
        }
    }

    public static boolean doSymLink(String inFileName, File outFile) {
        int exitCode = -1;
        try {
            Process sh = Runtime.getRuntime().exec("sh");
            OutputStream out = sh.getOutputStream();
            String command = "/system/bin/ln -s " + inFileName + " " + outFile + "\nexit\n";
            Log.i(TAG, "Running: " + command);
            out.write(command.getBytes("ASCII"));

            final char buf[] = new char[40];
            InputStreamReader reader = new InputStreamReader(sh.getInputStream());
            while (reader.read(buf) != -1)
                throw new IOException("stdout: " + new String(buf));
            reader = new InputStreamReader(sh.getErrorStream());
            while (reader.read(buf) != -1)
                throw new IOException("stderr: " + new String(buf));

            exitCode = sh.waitFor();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return false;
        } catch (InterruptedException e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
        Log.i(TAG, "symlink exitcode: " + exitCode);
        return exitCode == 0;
    }

    public static boolean doCopyFile(String inFileName, File outFile) {
        InputStream inStream = null;
        OutputStream outStream = null;

        try {
            inStream = new FileInputStream(inFileName);
            outStream = new FileOutputStream(outFile);

            return doCopyStream(inStream, outStream);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    public static boolean doCopyStream(InputStream inStream, OutputStream outStream)
    {
        byte[] buf = new byte[1024];
        int readBytes;
        try
        {
            while ((readBytes = inStream.read(buf)) > 0) {
                outStream.write(buf, 0, readBytes);
            }
            inStream.close();
            outStream.close();
            return true;
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    public interface ScanListener {
        public void processedApp(String packageName, int index, int total);
    }

    public App addApp(String packageName) {
        ApplicationInfo appInfo;
        PackageInfo packageInfo;
        try {
            appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES
                    | PackageManager.GET_PERMISSIONS);
        } catch (NameNotFoundException e) {
            Log.e(TAG, e.getMessage());
            return null;
        }

        App app = new App();
        app.name = (String) appInfo.loadLabel(pm);
        app.summary = (String) appInfo.loadDescription(pm);
        app.icon = getIconFile(packageName, packageInfo.versionCode).getName();
        app.id = appInfo.packageName;
        app.added = new Date(packageInfo.firstInstallTime);
        app.lastUpdated = new Date(packageInfo.lastUpdateTime);
        app.appInfo = appInfo;
        app.apks = new ArrayList<Apk>();

        // TODO: use pm.getInstallerPackageName(packageName) for something

        File apkFile = new File(appInfo.publicSourceDir);
        Apk apk = new Apk();
        apk.version = packageInfo.versionName;
        apk.vercode = packageInfo.versionCode;
        apk.detail_hashType = "sha256";
        apk.detail_hash = Utils.getBinaryHash(apkFile, apk.detail_hashType);
        apk.added = new Date(packageInfo.lastUpdateTime);
        apk.apkSourcePath = apkFile.getAbsolutePath();
        apk.apkSourceName = apkFile.getName();
        apk.minSdkVersion = appInfo.targetSdkVersion;
        apk.id = app.id;
        apk.file = apkFile;
        apk.detail_permissions = packageInfo.requestedPermissions;
        apk.apkName = apk.id + "_" + apk.vercode + ".apk";

        FeatureInfo[] features = packageInfo.reqFeatures;

        if (features != null && features.length > 0) {
            String[] featureNames = new String[features.length];

            for (int i = 0; i < features.length; i++)
                featureNames[i] = features[i].name;

            apk.features = featureNames;
        }

        // Signature[] sigs = pkgInfo.signatures;

        byte[] rawCertBytes;
        try {
            JarFile apkJar = new JarFile(apkFile);
            JarEntry aSignedEntry = (JarEntry) apkJar.getEntry("AndroidManifest.xml");

            if (aSignedEntry == null) {
                apkJar.close();
                return null;
            }

            InputStream tmpIn = apkJar.getInputStream(aSignedEntry);
            byte[] buff = new byte[2048];
            while (tmpIn.read(buff, 0, buff.length) != -1) {
                // NOP - apparently have to READ from the JarEntry before you
                // can call
                // getCerficates() and have it return != null. Yay Java.
            }
            tmpIn.close();

            if (aSignedEntry.getCertificates() == null
                    || aSignedEntry.getCertificates().length == 0) {
                apkJar.close();
                return null;
            }

            Certificate signer = aSignedEntry.getCertificates()[0];
            rawCertBytes = signer.getEncoded();

            apkJar.close();

            /*
             * I don't fully understand the loop used here. I've copied it
             * verbatim from getsig.java bundled with FDroidServer. I *believe*
             * it is taking the raw byte encoding of the certificate &
             * converting it to a byte array of the hex representation of the
             * original certificate byte array. This is then MD5 sum'd. It's a
             * really bad way to be doing this if I'm right... If I'm not right,
             * I really don't know! see lines 67->75 in getsig.java bundled with
             * Fdroidserver
             */
            byte[] fdroidSig = new byte[rawCertBytes.length * 2];
            for (int j = 0; j < rawCertBytes.length; j++) {
                byte v = rawCertBytes[j];
                int d = (v >> 4) & 0xF;
                fdroidSig[j * 2] = (byte) (d >= 10 ? ('a' + d - 10) : ('0' + d));
                d = v & 0xF;
                fdroidSig[j * 2 + 1] = (byte) (d >= 10 ? ('a' + d - 10) : ('0' + d));
            }
            apk.sig = Utils.hashBytes(fdroidSig, "md5");

        } catch (CertificateEncodingException e) {
            return null;
        } catch (IOException e) {
            return null;
        }

        app.apks.add(apk);

        if (!validApp(app))
            return null;

        apps.put(packageName, app);
        return app;
    }

    public void removeApp(String packageName) {
        apps.remove(packageName);
    }

    public List<String> getApps() {
        return new ArrayList<String>(apps.keySet());
    }

    public boolean validApp(App app) {
        if (app == null)
            return false;

        if (app.name == null || app.name.equals(""))
            return false;

        if (app.id == null | app.id.equals(""))
            return false;

        if (app.apks == null || app.apks.size() != 1)
            return false;

        File apkFile = app.apks.get(0).file;
        if (apkFile == null || !apkFile.canRead())
            return false;

        return true;
    }

    public void copyIconsToRepo() {
        for (App app : apps.values()) {
            if (app.apks.size() > 0) {
                Apk apk = app.apks.get(0);
                copyIconToRepo(app.appInfo.loadIcon(pm), app.id, apk.vercode);
            }
        }
    }

    /**
     * Extracts the icon from an APK and writes it to the repo as a PNG
     *
     * @return path to the PNG file
     */
    public void copyIconToRepo(Drawable drawable, String packageName, int versionCode) {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        File png = getIconFile(packageName, versionCode);
        OutputStream out;
        try {
            out = new BufferedOutputStream(new FileOutputStream(png));
            bitmap.compress(CompressFormat.PNG, 100, out);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private File getIconFile(String packageName, int versionCode) {
        return new File(iconsDir, packageName + "_" + versionCode + ".png");
    }

    public void writeIndexXML() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc = builder.newDocument();
        Element rootElement = doc.createElement("fdroid");
        doc.appendChild(rootElement);

        // max age is an EditTextPreference, which is always a String
        int repoMaxAge = Float.valueOf(prefs.getString("max_repo_age_days",
                DEFAULT_REPO_MAX_AGE_DAYS)).intValue();

        Element repo = doc.createElement("repo");
        repo.setAttribute("icon", "blah.png");
        repo.setAttribute("name", "Kerplapp on " + ipAddressString);
        repo.setAttribute("url", uriString);
        long timestamp = System.currentTimeMillis() / 1000L;
        repo.setAttribute("timestamp", String.valueOf(timestamp));
        repo.setAttribute("maxage", String.valueOf(repoMaxAge));
        rootElement.appendChild(repo);

        Element repoDesc = doc.createElement("description");
        repoDesc.setTextContent("A repo generated from apps installed on an Android Device");
        repo.appendChild(repoDesc);

        SimpleDateFormat dateToStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        for (Entry<String, App> entry : apps.entrySet()) {
            String latestVersion = "0";
            String latestVerCode = "0";
            App app = entry.getValue();
            Element application = doc.createElement("application");
            application.setAttribute("id", app.id);
            rootElement.appendChild(application);

            Element appID = doc.createElement("id");
            appID.setTextContent(app.id);
            application.appendChild(appID);

            Element added = doc.createElement("added");
            added.setTextContent(dateToStr.format(app.added));
            application.appendChild(added);

            Element lastUpdated = doc.createElement("lastupdated");
            lastUpdated.setTextContent(dateToStr.format(app.lastUpdated));
            application.appendChild(lastUpdated);

            Element name = doc.createElement("name");
            name.setTextContent(app.name);
            application.appendChild(name);

            Element summary = doc.createElement("summary");
            summary.setTextContent(app.name);
            application.appendChild(summary);

            Element description = doc.createElement("description");
            description.setTextContent(app.name);
            application.appendChild(description);

            Element desc = doc.createElement("desc");
            desc.setTextContent(app.name);
            application.appendChild(desc);

            Element icon = doc.createElement("icon");
            icon.setTextContent(app.icon);
            application.appendChild(icon);

            Element license = doc.createElement("license");
            license.setTextContent("Unknown");
            application.appendChild(license);

            Element categories = doc.createElement("categories");
            categories.setTextContent("Kerplapp," + ipAddressString);
            application.appendChild(categories);

            Element category = doc.createElement("category");
            category.setTextContent("Kerplapp," + ipAddressString);
            application.appendChild(category);

            Element web = doc.createElement("web");
            application.appendChild(web);

            Element source = doc.createElement("source");
            application.appendChild(source);

            Element tracker = doc.createElement("tracker");
            application.appendChild(tracker);

            Element marketVersion = doc.createElement("marketversion");
            application.appendChild(marketVersion);

            Element marketVerCode = doc.createElement("marketvercode");
            application.appendChild(marketVerCode);

            for (Apk apk : app.apks) {
                Element packageNode = doc.createElement("package");

                Element version = doc.createElement("version");
                latestVersion = apk.version;
                version.setTextContent(apk.version);
                packageNode.appendChild(version);

                // F-Droid unfortunately calls versionCode versioncode...
                Element versioncode = doc.createElement("versioncode");
                latestVerCode = String.valueOf(apk.vercode);
                versioncode.setTextContent(latestVerCode);
                packageNode.appendChild(versioncode);

                Element apkname = doc.createElement("apkname");
                apkname.setTextContent(apk.apkName);
                packageNode.appendChild(apkname);

                Element hash = doc.createElement("hash");
                hash.setAttribute("type", apk.detail_hashType);
                hash.setTextContent(apk.detail_hash.toLowerCase(Locale.US));
                packageNode.appendChild(hash);

                Element sig = doc.createElement("sig");
                sig.setTextContent(apk.sig.toLowerCase(Locale.US));
                packageNode.appendChild(sig);

                Element size = doc.createElement("size");
                size.setTextContent(String.valueOf(apk.file.length()));
                packageNode.appendChild(size);

                Element sdkver = doc.createElement("sdkver");
                sdkver.setTextContent(String.valueOf(apk.minSdkVersion));
                packageNode.appendChild(sdkver);

                Element apkAdded = doc.createElement("added");
                apkAdded.setTextContent(dateToStr.format(apk.added));
                packageNode.appendChild(apkAdded);

                Element features = doc.createElement("features");
                if (apk.features != null && apk.features.length > 0) {
                    StringBuilder buff = new StringBuilder();

                    for (int i = 0; i < apk.features.length; i++) {
                        buff.append(apk.features[i]);

                        if (i != apk.features.length - 1)
                            buff.append(",");
                    }

                    features.setTextContent(buff.toString());
                }
                packageNode.appendChild(features);

                Element permissions = doc.createElement("permissions");
                if (apk.detail_permissions != null && apk.detail_permissions.length > 0) {
                    StringBuilder buff = new StringBuilder();

                    for (int i = 0; i < apk.detail_permissions.length; i++) {
                        buff.append(apk.detail_permissions[i].replace("android.permission.", ""));

                        if (i != apk.detail_permissions.length - 1)
                            buff.append(",");
                    }

                    permissions.setTextContent(buff.toString());
                }
                packageNode.appendChild(permissions);

                application.appendChild(packageNode);
            }

            // now mark the latest version in the feed for this particular app
            marketVersion.setTextContent(latestVersion);
            marketVerCode.setTextContent(latestVerCode);
        }

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        DOMSource domSource = new DOMSource(doc);
        StreamResult result = new StreamResult(xmlIndex);

        transformer.transform(domSource, result);
    }

    public void writeIndexJar() throws IOException {
        BufferedOutputStream bo = new BufferedOutputStream(
                new FileOutputStream(xmlIndexJarUnsigned));
        JarOutputStream jo = new JarOutputStream(bo);

        BufferedInputStream bi = new BufferedInputStream(new FileInputStream(xmlIndex));

        JarEntry je = new JarEntry("index.xml");
        jo.putNextEntry(je);

        byte[] buf = new byte[1024];
        int bytesRead;

        while ((bytesRead = bi.read(buf)) != -1) {
            jo.write(buf, 0, bytesRead);
        }

        bi.close();
        jo.close();
        bo.close();

        KerplappKeyStore kerplappStore = appCtx.getKeyStore();
        kerplappStore.signZip(xmlIndexJarUnsigned, xmlIndexJar);

        xmlIndexJarUnsigned.delete();
    }
}
