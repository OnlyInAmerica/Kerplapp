package net.binaryparadox.kerplapp.repo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import kellinwood.security.zipsigner.ZipSigner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;

import net.binaryparadox.kerplapp.AppListEntry;
import net.binaryparadox.kerplapp.Utils;

public class KerplappRepo
{
  private static final String TAG = KerplappRepo.class.getCanonicalName();

  private PackageManager pm = null;
  
  private Map<String, App> apps = new HashMap<String, App>();
  
  private File xmlIndex = null;
  private File xmlIndexJar = null;
  private File xmlIndexJarUnsigned = null;
  public File webRoot = null;
  public File repoDir = null;
  
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
    xmlIndexJar = new File(repoDir, "index.jar");
    xmlIndexJarUnsigned = new File(repoDir, "index.unsigned.jar");
    
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
    copyApksToRepo(new ArrayList<String>(apps.keySet()));
  }
  
  public void copyApksToRepo(List<String> appsToCopy)
  {
    for(String pkg : appsToCopy)
    {
      App app = apps.get(pkg);
      
      for(Apk apk : app.apks)
      {
        File outFile = new File(repoDir, apk.apkName);
        if(!copyFile(apk.apkPath, outFile))
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
  
  public ArrayList<AppListEntry> loadInstalledPackageNames()
  {
    return loadInstalledPackageNames(null);
  }

  public ArrayList<AppListEntry> loadInstalledPackageNames(ScanListener callback)
  {
    List<ApplicationInfo> apps =
        pm.getInstalledApplications(PackageManager.GET_META_DATA);
    
    if(apps == null || apps.size() == 0)
      return null;
    
    ArrayList<AppListEntry> installedPkgs = new ArrayList<AppListEntry>();
    
    for(int i = 0; i < apps.size(); i++)
    {
      ApplicationInfo a = apps.get(i);
      
      String pkgName = a.packageName;
      String appName = (String) a.loadLabel(pm);
      
      installedPkgs.add(new AppListEntry(pkgName, appName, false));
      
      if(callback != null)
        callback.processedApp(pkgName, i, apps.size());
    }

    return installedPkgs;
  }
  
  public App addAppToRepo(String pkgName) throws NameNotFoundException
  {
    ApplicationInfo a = pm.getApplicationInfo(pkgName, PackageManager.GET_META_DATA);
    PackageInfo pkgInfo  = pm.getPackageInfo(pkgName, PackageManager.GET_SIGNATURES | PackageManager.GET_PERMISSIONS);   
    App appOb = new App();
    
    appOb.name = (String) a.loadLabel(pm);
    appOb.summary = (String) a.loadDescription(pm);
    appOb.icon = a.loadIcon(pm).toString();    
    appOb.id = a.packageName;    
    appOb.added = new Date(pkgInfo.firstInstallTime);
    appOb.lastUpdated = new Date(pkgInfo.lastUpdateTime);
    appOb.apks = new ArrayList<Apk>();
    
    //TODO: use pm.getInstallerPackageName(packageName) for something
    
    File apkFile = new File(a.publicSourceDir);     
    Apk apkOb = new Apk();
    apkOb.version = pkgInfo.versionName;
    apkOb.vercode = pkgInfo.versionCode;
    apkOb.detail_hashType = "sha256";
    apkOb.detail_hash = Utils.getBinaryHash(apkFile, apkOb.detail_hashType);
    apkOb.added = new Date(pkgInfo.lastUpdateTime);
    apkOb.apkPath = apkFile.getAbsolutePath();
    apkOb.apkName = apkFile.getName();
    apkOb.minSdkVersion = a.targetSdkVersion;
    apkOb.id = appOb.id;
    apkOb.file = apkFile;
    apkOb.detail_permissions = pkgInfo.requestedPermissions;
    
    FeatureInfo[] features = pkgInfo.reqFeatures;
    
    if(features != null && features.length > 0)
    {
      String[] featureNames = new String[features.length];
      
      for(int i = 0; i < features.length; i++)
        featureNames[i] = features[i].name;
      
      apkOb.features = featureNames;
    }
    
    //Signature[]        sigs     = pkgInfo.signatures;  
   
    byte[] rawCertBytes;
    try
    {
      JarFile apkJar = new JarFile(apkFile);
      JarEntry aSignedEntry = (JarEntry) apkJar.getEntry("AndroidManifest.xml");  
      
      if(aSignedEntry == null) 
        return null;
      
      InputStream tmpIn = apkJar.getInputStream(aSignedEntry);
      byte[] buff = new byte[2048];
      while(tmpIn.read(buff, 0, buff.length) != -1)
      { 
        //NOP - apparently have to READ from the JarEntry before you can call
        //      getCerficates() and have it return != null. Yay Java.
      }
      tmpIn.close();
           
      if(aSignedEntry.getCertificates() == null || aSignedEntry.getCertificates().length == 0)
        return null;
      
      Certificate signer = aSignedEntry.getCertificates()[0];
      rawCertBytes = signer.getEncoded();
      
      apkJar.close();
      
      /*
       * I don't fully understand the loop used here. I've copied it verbatim from
       * getsig.java bundled with FDroidServer. I *believe* it is taking the raw byte
       * encoding of the certificate & converting it to a byte array of the hex 
       * representation of the original certificat byte array. This is then MD5 sum'd.
       * It's a really bad way to be doing this if I'm right... If I'm not right, I really
       * don't know!
       * 
       * see lines 67->75 in getsig.java bundled with Fdroidserver
       */
      byte[] fdroidSig = new byte[rawCertBytes.length * 2];
      for(int j = 0; j < rawCertBytes.length; j++)
      {
        byte v = rawCertBytes[j];
        int d = (v >> 4) & 0xF;
        fdroidSig[j*2] = (byte)(d >= 10 ? ('a' + d - 10) : ('0' + d));
        d = v & 0xF;
        fdroidSig[j*2+1] = (byte)(d >= 10 ? ('a' + d - 10) : ('0' + d));
      } 
      apkOb.sig = Utils.hashBytes(fdroidSig, "md5");
      
    } catch (CertificateEncodingException e) {
      return null;
    } catch (IOException e) {
      return null;
    }
   
    appOb.apks.add(apkOb);
    
    if(!validApp(appOb))
      return null;
    
    apps.put(pkgName, appOb);
    return appOb;
  }
  
  public List<String> getInstalledPkgNames()
  {
    return new ArrayList<String>(this.apps.keySet());
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
    for(Entry<String,App> entry : apps.entrySet())
    {
      App a = entry.getValue();
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
      summary.setTextContent(a.name);
      app.appendChild(summary);
      
      Element description = doc.createElement("description");
      description.setTextContent(a.name);
      app.appendChild(description);
      
      Element desc        = doc.createElement("desc");
      desc.setTextContent(a.name);
      app.appendChild(desc);
      
      Element icon = doc.createElement("icon");
      icon.setTextContent(a.icon);
      app.appendChild(icon);
      
      Element license = doc.createElement("license");
      app.appendChild(license);
      
      Element category = doc.createElement("category");
      category.setTextContent("Kerplapp");
      app.appendChild(category);
      
      Element web = doc.createElement("web");
      app.appendChild(web);
      
      Element source = doc.createElement("source");
      app.appendChild(source);
      
      Element tracker = doc.createElement("tracker");
      app.appendChild(tracker);
      
      Element marketVersion = doc.createElement("marketversion");
      app.appendChild(marketVersion);
      
      Element marketVerCode = doc.createElement("marketversioncode");
      marketVerCode.setTextContent("0");
      app.appendChild(marketVerCode);
            
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
        sdkver.setTextContent(String.valueOf(apk.minSdkVersion));
        packageNode.appendChild(sdkver);
        
        Element apkAdded = doc.createElement("added");
        apkAdded.setTextContent(dateToStr.format(apk.added));
        packageNode.appendChild(apkAdded);
        
        Element features = doc.createElement("features");
        if(apk.features != null && apk.features.length > 0)
        {
          StringBuilder buff = new StringBuilder();
          
          for(int i = 0; i < apk.features.length; i++)
          {
            buff.append(apk.features[i]);
            
            if(i != apk.features.length - 1)
              buff.append(",");
          }
          
          features.setTextContent(buff.toString());
        }
        packageNode.appendChild(features);
        
        Element permissions = doc.createElement("permissions");
        if(apk.detail_permissions != null && apk.detail_permissions.length > 0)
        {
          StringBuilder buff = new StringBuilder();
          
          for(int i = 0; i < apk.detail_permissions.length; i++)
          {
            buff.append(apk.detail_permissions[i]);
            
            if(i != apk.detail_permissions.length - 1)
              buff.append(",");
          }
          
          permissions.setTextContent(buff.toString());
        }
        packageNode.appendChild(permissions);
        
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
  
  public void writeIndexJar() throws IOException
  {
    BufferedOutputStream bo = new BufferedOutputStream(new FileOutputStream(xmlIndexJarUnsigned));
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

    // Sign with the built-in default test key/certificate.
      ZipSigner zipSigner;
      try
      {
        zipSigner = new ZipSigner();
        zipSigner.setKeymode("testkey");
        zipSigner.signZip(xmlIndexJarUnsigned.getAbsolutePath(), xmlIndexJar.getAbsolutePath());
        
        Log.i(TAG, xmlIndexJar.getAbsolutePath());
        Log.i(TAG, "Signed zip");
      } catch(Throwable t) {
        t.printStackTrace();
        Log.e(TAG, t.getMessage());
      }
    
  }  
  
  public String pubkey; // null for an unsigned repo
  
  
  
}