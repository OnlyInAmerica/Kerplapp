package net.binaryparadox.kerplapp;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.SimpleWebServer;

import java.io.File;
import java.io.IOException;
import java.security.Security;

import javax.net.ssl.SSLServerSocketFactory;

public class KerplappHTTPD extends SimpleWebServer {
	public String host;
	public int port;
	public File wwwroot;


	static {
		Security.insertProviderAt(
				new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
	}

	public KerplappHTTPD(String host, int port, File wwwroot, boolean quiet,
			KerplappKeyStore keystore) {
		super(host, port, wwwroot, quiet);

		this.host = host;
		this.wwwroot = wwwroot;

        try {
            SSLServerSocketFactory factory = NanoHTTPD.makeSSLSocketFactory(
            			keystore.getKeyStore(),
            			keystore.getKeyManagerFactory());
            makeSecure(factory);
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
