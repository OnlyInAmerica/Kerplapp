package net.binaryparadox.kerplapp.repo;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Date;

import org.spongycastle.jce.X509Principal;
import org.spongycastle.x509.X509V1CertificateGenerator;
import org.spongycastle.x509.X509V3CertificateGenerator;

public class Crypto
{
  static {
    Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
  }
  
  public KeyStore createKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, InvalidKeyException, IllegalStateException, NoSuchProviderException, SignatureException
  {
    
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();

    Date now = new Date();

    X509V3CertificateGenerator cert = new X509V3CertificateGenerator();   
    cert.setSerialNumber(BigInteger.valueOf(1));   //or generate a random number  
    cert.setSubjectDN(new X509Principal("CN=localhost"));  //see examples to add O,OU etc  
    cert.setIssuerDN(new X509Principal("CN=localhost")); //same since it is self-signed  
    cert.setPublicKey(keyPair.getPublic());  
    cert.setNotBefore(now);  
    cert.setNotAfter(new Date(now.getTime() + (10000 * 60 * 60 * 24 * 360)));
    cert.setSignatureAlgorithm("SHA1WithRSAEncryption");   
    PrivateKey signingKey = keyPair.getPrivate();    
    
    Certificate[] certChain = new Certificate[1];  
    certChain[0] = cert.generate(signingKey, "BC"); ;  
    
    KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
    
    ks.load(null, "ahhh".toCharArray());
    ks.setKeyEntry("key1", (Key)keyPair.getPrivate(), "ahhh".toCharArray(), certChain); 
    
    return ks;
  }
  

}
