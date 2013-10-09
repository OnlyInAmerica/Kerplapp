package net.binaryparadox.kerplapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler.Callback;
import android.util.Log;

import net.binaryparadox.kerplapp.Utils.CommaSeparatedList;

public class KerplappRepo
{
  private static final String TAG = KerplappRepo.class.getCanonicalName();

  private PackageManager pm = null;
  private List<App> apps = null;
  
  private File xmlIndex = null;
  private File webRoot = null;
  private File repoDir = null;
  
  public KerplappRepo(Context c)
  {
    webRoot = c.getFilesDir();
    pm =  c.getPackageManager();
  }
  
  public File getRepoDir()
  {
    return repoDir;
  }
  
  public File getIndex()
  {
    return xmlIndex;
  }
  
  public void init() throws Exception
  {    
    repoDir = new File(webRoot, "repo");
    
    if(!repoDir.exists())
      if(!repoDir.mkdir())
        throw new IllegalStateException("Unable to create empty repo/ directory");
    
    xmlIndex = new File(repoDir, "index.xml");
    
    if(!xmlIndex.exists())
      if(!xmlIndex.createNewFile())
        throw new IllegalStateException("Unable to create empty index.xml file");
      
    //Log.i(TAG, xmlIndex.getAbsolutePath());
    // /data/app\
    
    ///data/data/net.binaryparadox.kerplapp/files/repo/index.xml
    /*
    if(apps.size() > 0)
    {
      //writeIndexXML();
      //copyApksToRepo();
    }*/
  }
  
  public void copyApksToRepo()
  {
    copyApksToRepo(apps);
  }
  public void copyApksToRepo(List<App> appsToCopy)
  {
    for(App app : appsToCopy)
    {
      for(Apk apk : app.apks)
      {
        File outFile = new File(repoDir, apk.id);
        if(!copyFile(apk.apkName, outFile))
        {
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
  
  public static boolean copyFile(String inFile, File out) {
    byte[] buf = new byte[1024];
    int readBytes;
    InputStream  inStream = null;
    OutputStream outStream = null;
   
    try {
      inStream = new FileInputStream(inFile);
    } catch(IOException e) {
      Log.e(TAG, e.getMessage());
      return false;
    }
    
    try{
      
      outStream = new FileOutputStream(out);


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
  
  public interface ScanListener
  {
    public void processedApp(String pkgName, int index, int total);
  }
  
  public void scanForApps() throws NameNotFoundException, ZipException, IOException
  {
    scanForApps(null);
  }
  
  public void scanForApps(ScanListener callback) throws NameNotFoundException, ZipException, IOException
  {    
    List<ApplicationInfo> apps =
        pm.getInstalledApplications(PackageManager.GET_META_DATA);
    
    if(apps == null || apps.size() == 0)
      return;
        
    List<KerplappRepo.App> installedAppObs = new java.util.ArrayList<KerplappRepo.App>();
    
    for(int i = 0; i < apps.size(); i++)
    {
      ApplicationInfo a = apps.get(i);
      PackageInfo pkgInfo  = pm.getPackageInfo(a.packageName, PackageManager.GET_SIGNATURES);   
      KerplappRepo.App appOb = new KerplappRepo.App();
      
      appOb.name = (String) a.loadLabel(pm);
      appOb.summary = (String) a.loadDescription(pm);
      appOb.icon = a.loadIcon(pm).toString();    
      appOb.id = a.packageName;    
      appOb.added = new Date(pkgInfo.firstInstallTime);
      appOb.lastUpdated = new Date(pkgInfo.lastUpdateTime);
      appOb.apks = new ArrayList<Apk>();
      
      File apkFile = new File(a.publicSourceDir);     
      KerplappRepo.Apk apkOb = new KerplappRepo.Apk();
      apkOb.version = pkgInfo.versionName;
      apkOb.vercode = pkgInfo.versionCode;
      apkOb.detail_hashType = "SHA1";
      apkOb.detail_hash = Utils.getBinaryHash(apkFile);
      apkOb.added = new Date(pkgInfo.lastUpdateTime);
      apkOb.apkName = apkFile.getAbsolutePath();
      apkOb.id = appOb.id;
      apkOb.file = apkFile;
      
      Signature[]        sigs     = pkgInfo.signatures;
      apkOb.sig = Utils.hashBytes(sigs[0].toByteArray());
      
      appOb.apks.add(apkOb);
      
      if(!validApp(appOb))
        continue;
      
      installedAppObs.add(appOb);   
      
      if(callback != null)
        callback.processedApp(appOb.id, i, apps.size());
    }

    this.apps = installedAppObs;
  }
  
  public List<App> getApps()
  {
    return this.apps;
  }
  
  public boolean validApp(App a)
  {
    if(a == null)
      return false;
    
    if(a.name == null || a.name.equals(""))
      return false;
    
    if(a.id == null | a.id.equals(""))
      return false;
    
    if(a.apks == null || a.apks.size() != 1)
      return false;
    
    File apkFile = a.apks.get(0).file;
    if(apkFile == null || !apkFile.canRead())
      return false;
    
    return true;
  }

  public void writeIndexXML() throws Exception
  {
    writeIndexXML(apps);
  }

  public void writeIndexXML(List<App> appsToWrite) throws Exception
  {  
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder = factory.newDocumentBuilder();
    
    Document doc = builder.newDocument();
    Element rootElement = doc.createElement("fdroid");
    doc.appendChild(rootElement);
    
    Element repo = doc.createElement("repo");
    repo.setAttribute("icon", "blah.png");
    repo.setAttribute("name", "Kerplapp Repo");
    repo.setAttribute("url", "http://localhost:8888");
    rootElement.appendChild(repo);
    
    Element repoDesc = doc.createElement("description");
    repoDesc.setTextContent("A repo generated from apps installed on an Android Device");
    repo.appendChild(repoDesc);
    
    SimpleDateFormat dateToStr = new SimpleDateFormat("y-M-d", Locale.US);
    for(App a : appsToWrite)
    {
      Element app = doc.createElement("application");
      app.setAttribute("id", a.id);
      
      Element appID = doc.createElement("id");
      appID.setTextContent(a.id);
      app.appendChild(appID);
      
      Element added = doc.createElement("added");
      added.setTextContent(dateToStr.format(a.added));
      app.appendChild(added);
     
      Element lastUpdated = doc.createElement("lastupdated");
      lastUpdated.setTextContent(dateToStr.format(a.lastUpdated));
      app.appendChild(lastUpdated);
      
      Element name = doc.createElement("name");
      name.setTextContent(a.name);
      app.appendChild(name);
      
      Element summary = doc.createElement("summary");
      summary.setTextContent(a.detail_description);
      app.appendChild(summary);
      
      Element icon = doc.createElement("icon");
      icon.setTextContent(a.icon);
      app.appendChild(icon);
      
      for(Apk apk : a.apks)
      {
        Element packageNode = doc.createElement("package");
        
        Element version = doc.createElement("version");
        version.setTextContent(apk.version);
        packageNode.appendChild(version);
        
        Element versionCode = doc.createElement("versionCode");
        versionCode.setTextContent(String.valueOf(apk.vercode));
        packageNode.appendChild(versionCode);
        
        Element apkname = doc.createElement("apkname");
        apkname.setTextContent(apk.apkName);
        packageNode.appendChild(apkname);
        
        Element hash = doc.createElement("hash");
        hash.setAttribute("type", apk.detail_hashType);
        hash.setTextContent(apk.detail_hash);
        packageNode.appendChild(hash);
        
        Element sig = doc.createElement("sig");
        sig.setTextContent(apk.sig);
        packageNode.appendChild(sig);
        
        Element size = doc.createElement("size");
        packageNode.appendChild(size);
        
        Element sdkver = doc.createElement("sdkver");
        packageNode.appendChild(sdkver);
        
        Element apkAdded = doc.createElement("added");
        apkAdded.setTextContent(dateToStr.format(apk.added));
        packageNode.appendChild(apkAdded);
        
        Element features = doc.createElement("features");
        packageNode.appendChild(features);
        
        app.appendChild(packageNode);
      }
      
      rootElement.appendChild(app);
    }
      
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    Transformer transformer = transformerFactory.newTransformer();
    
    DOMSource domSource = new DOMSource(doc);
    StreamResult result = new StreamResult(xmlIndex);
    
    transformer.transform(domSource, result);
  }
  
  public String pubkey; // null for an unsigned repo
  
  public static class App implements Comparable<App> {
    public App() {
        name = "Unknown";
        summary = "Unknown application";
        icon = "noicon.png";
        id = "unknown";
        license = "Unknown";
        category = "Uncategorized";
        detail_trackerURL = null;
        detail_sourceURL = null;
        detail_donateURL = null;
        detail_webURL = null;
        antiFeatures = null;
        requirements = null;
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

    public String license;
    public String category;

    // Null when !detail_Populated
    public String detail_webURL;

    // Null when !detail_Populated
    public String detail_trackerURL;

    // Null when !detail_Populated
    public String detail_sourceURL;

    // Donate link, or null
    // Null when !detail_Populated
    public String detail_donateURL;

    public String curVersion;
    public int curVercode;
    public Date added;
    public Date lastUpdated;

    // Installed version (or null) and version code. These are valid only
    // when getApps() has been called with getinstalledinfo=true.
    public String installedVersion;
    public int installedVerCode;

    // List of anti-features (as defined in the metadata
    // documentation) or null if there aren't any.
    public CommaSeparatedList antiFeatures;

    // List of special requirements (such as root privileges) or
    // null if there aren't any.
    public CommaSeparatedList requirements;

    // True if there are new versions (apks) that the user hasn't
    // explicitly ignored. (We're currently not using the database
    // field for this - we make the decision on the fly in getApps().
    public boolean hasUpdates;

    // The name of the version that would be updated to.
    public String updateVersion;

    // Used internally for tracking during repo updates.
    public boolean updated;

    // List of apks.
    public List<Apk> apks;

    // Get the current version - this will be one of the Apks from 'apks'.
    // Can return null if there are no available versions.
    // This should be the 'current' version, as in the most recent stable
    // one, that most users would want by default. It might not be the
    // most recent, if for example there are betas etc.
    public Apk getCurrentVersion() {

        // Try and return the real current version first...
        if (curVersion != null && curVercode > 0) {
            for (Apk apk : apks) {
                if (apk.vercode == curVercode)
                    return apk;
            }
        }

        // If we don't know the current version, or we don't have it, we
        // return the most recent version we have...
        int latestcode = -1;
        Apk latestapk = null;
        for (Apk apk : apks) {
            if (apk.vercode > latestcode) {
                latestapk = apk;
                latestcode = apk.vercode;
            }
        }
        return latestapk;
    }

    @Override
    public int compareTo(App arg0) {
        return name.compareToIgnoreCase(arg0.name);
    }

  }
 
  public static class Apk {
    public File file;

    public Apk() {
        detail_size = 0;
        added = null;
        detail_hash = null;
        detail_hashType = null;
        detail_permissions = null;
    }

    public String id;
    public String version;
    public int vercode;
    public int detail_size; // Size in bytes - 0 means we don't know!
    public String detail_hash;
    public String detail_hashType;
    public int minSdkVersion; // 0 if unknown
    public Date added;
    public CommaSeparatedList detail_permissions; // null if empty or
                                                  // unknown
    public CommaSeparatedList features; // null if empty or unknown

    // ID (md5 sum of public key) of signature. Might be null, in the
    // transition to this field existing.
    public String sig;

    public String apkName;
  }
}