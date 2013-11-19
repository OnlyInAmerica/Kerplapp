package net.binaryparadox.kerplapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;

import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v1CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.SimpleWebServer;

public class KerplappHTTPD extends SimpleWebServer {
	public String host;
	public int port;
	public File wwwroot;

	private File keyStoreFile;
	
	static {
		Security.insertProviderAt(
				new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
	}

	public KerplappHTTPD(String host, int port, File wwwroot, boolean quiet,
			File keyStoreFile) {
		super(host, port, wwwroot, quiet);
		
		this.host = host;
		this.wwwroot = wwwroot;
		this.keyStoreFile = keyStoreFile;

		try {
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

			if (!keyStoreFile.exists())
				keyStore = generateNewKeyStore();

			InputStream keyStoreFileIS = new FileInputStream(keyStoreFile);
			keyStore.load(keyStoreFileIS, "".toCharArray());

			KeyManagerFactory keyManagerFactory = KeyManagerFactory
					.getInstance(KeyManagerFactory.getDefaultAlgorithm());
			keyManagerFactory.init(keyStore, "".toCharArray());

			SSLServerSocketFactory factory = NanoHTTPD.makeSSLSocketFactory(
					keyStore, keyManagerFactory);
			makeSecure(factory);

		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (UnrecoverableKeyException e) {
			e.printStackTrace();
		}
	}
	
	private KeyStore generateNewKeyStore()
	{
		KeyPair randKP = generateRandomKeypair();
		Certificate[] certChain = generateSelfSignedCertChain(randKP);
		
		try{
			KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
			/*
			 * Note: using "".toCharArray() to set a blank passphrase. There isn't a
			 * lot of value to protecting the key with an annoying passphrase. Maybe
			 * one day it'll make sense to add cacheword support for the SSL key?
			 */
			ks.load(null, "".toCharArray());
			ks.setKeyEntry("https", (Key) randKP.getPrivate(),
					"".toCharArray(), certChain);

			ks.store(new FileOutputStream(keyStoreFile), "".toCharArray());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (CertificateException e) {
			e.printStackTrace();
		}

		return null;		
	}
	
	private KeyPair generateRandomKeypair()
	{
		try
		{
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(2048);
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			return keyPair;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private Certificate[] generateSelfSignedCertChain(KeyPair keyPair)
	{
		try {
			SecureRandom rand = new SecureRandom();
			PrivateKey privKey = keyPair.getPrivate();
			ContentSigner sigGen = new JcaContentSignerBuilder("SHA1withRSA").build(privKey);
			
			SubjectPublicKeyInfo subPubKeyInfo = new SubjectPublicKeyInfo(
	    	    ASN1Sequence.getInstance(keyPair.getPublic().getEncoded()));
				   			        
		    Date startDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
		    Date endDate = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000);
			   
		    X509v1CertificateBuilder v1CertGen = new X509v1CertificateBuilder(
	              new X500Name("CN="+host), 
	              BigInteger.valueOf(rand.nextLong()), 
	              startDate, endDate, 
	              new X500Name("CN="+host), 
	              subPubKeyInfo);
			    
			X509CertificateHolder certHolder = v1CertGen.build(sigGen);
			
			Certificate[] certChain = new Certificate[1];
			certChain[0] = new JcaX509CertificateConverter().getCertificate(certHolder);
			
			return certChain;
		} catch (OperatorCreationException e) {		
			e.printStackTrace();
		} catch(CertificateException e) {
			e.printStackTrace();
		} 
		
		return null;
	}

}
