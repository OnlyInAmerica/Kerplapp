package net.binaryparadox.kerplapp.repo;

import java.io.File;
import java.util.Date;

public class Apk {
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
  public String[] detail_permissions; // null if empty or
                                                // unknown
  public String[] features; // null if empty or unknown

  // ID (md5 sum of public key) of signature. Might be null, in the
  // transition to this field existing.
  public String sig;

  public String apkPath;
  public String apkName;
}