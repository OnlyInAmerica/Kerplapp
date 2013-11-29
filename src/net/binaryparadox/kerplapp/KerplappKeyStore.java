package net.binaryparadox.kerplapp;

import org.spongycastle.asn1.ASN1Sequence;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.SubjectPublicKeyInfo;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v1CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.Date;

import javax.net.ssl.KeyManagerFactory;

public class KerplappKeyStore {
    static {
        Security.insertProviderAt(
                new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    private static final String INDEX_CERT_ALIAS = "fdroid";
    private static final String HTTP_CERT_ALIAS  = "https";

    private static final String DEFAULT_SIG_ALG  = "SHA1withRSA";
    private static final String DEFAULT_KEY_ALGO = "RSA";
    private static final int    DEFAULT_KEY_BITS = 2048;

    private KeyStore            keyStore;
    private KeyManagerFactory   keyManagerFactory;
    private File                backingFile;

    public KerplappKeyStore(File backingFile) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, OperatorCreationException, UnrecoverableKeyException {
        this.backingFile = backingFile;
        this.keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

        //If there isn't a persisted BKS keystore on disk we need to
        //create a new empty keystore
        if (!backingFile.exists())
        {
            //Init a new keystore with a blank passphrase
            keyStore.load(null, "".toCharArray());

            //Generate a random key pair to associate with the INDEX_CERT_ALIAS 
            //certificate in the keystore. This keypair will be used for the HTTPS cert
            //as well.
            KeyPair rndKeys = generateRandomKeypair();

            //Generate a self signed certificate for signing the index.jar
            //We can't generate the HTTPS certificate until we know what the IP
            //address will be to use for the CN field.
            X500Name subject = new X500Name("O=Kerplapp,OU=GuardianProject");
            Certificate indexCert = generateSelfSignedCertChain(rndKeys, subject);

            addToStore(INDEX_CERT_ALIAS, rndKeys, indexCert);
        } else {
            keyStore.load(new FileInputStream(backingFile), "".toCharArray());
        }

        keyManagerFactory = KeyManagerFactory
                .getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "".toCharArray());
    }

    public void setupHTTPSCertificate(String hostname) throws CertificateException, OperatorCreationException, KeyStoreException, NoSuchAlgorithmException, FileNotFoundException, IOException, UnrecoverableKeyException
    {
        //Get the existing private/public keypair to use for the HTTPS cert
        KeyPair kerplappKeypair = getKerplappKeypair();
        
        //Once we have a hostname we can generate a self signed cert with a valid
        //CN field to stash into the keystore in a predictable place. If the hostname
        //changes we should run this method again to stomp old HTTPS_CERT_ALIAS entries.
        X500Name subject = new X500Name("CN="+hostname);
        Certificate indexCert = generateSelfSignedCertChain(kerplappKeypair, subject);

        addToStore(HTTP_CERT_ALIAS, kerplappKeypair, indexCert);
    }

    public KeyStore getKeyStore()
    {
        return keyStore;
    }

    public KeyManagerFactory getKeyManagerFactory()
    {
        return keyManagerFactory;
    }
    
    private KeyPair getKerplappKeypair() throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException
    {
        //You can't store a keypair without an associated certificate chain so,
        //we'll use the INDEX_CERT_ALIAS as the de-facto keypair/certificate chain
        //This cert/key is initialized when the KerplappKeyStore is constructed for 
        //the first time and should *always* be present.
        Key key = keyStore.getKey(INDEX_CERT_ALIAS, "".toCharArray());
        
        if(key instanceof PrivateKey) {
            Certificate cert = keyStore.getCertificate(INDEX_CERT_ALIAS);
            PublicKey publicKey = cert.getPublicKey();
            return new KeyPair(publicKey, (PrivateKey) key);
        }            
       
        return null;
    }

    private void addToStore(String alias, KeyPair kp, Certificate cert) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException, UnrecoverableKeyException
    {
        Certificate[] chain = new Certificate[] { cert };
        keyStore.setKeyEntry(alias, (Key) kp.getPrivate(),
                "".toCharArray(), chain);

        keyStore.store(new FileOutputStream(backingFile), "".toCharArray());
        keyManagerFactory.init(keyStore, "".toCharArray());
    }

    private KeyPair generateRandomKeypair() throws NoSuchAlgorithmException
    {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(DEFAULT_KEY_ALGO);
        keyPairGenerator.initialize(DEFAULT_KEY_BITS);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return keyPair;
    }

    private Certificate generateSelfSignedCertChain(KeyPair kp, X500Name subject) throws CertificateException, OperatorCreationException
    {
        SecureRandom rand = new SecureRandom();
        PrivateKey privKey = kp.getPrivate();
        PublicKey  pubKey  = kp.getPublic();
        ContentSigner sigGen = new JcaContentSignerBuilder(DEFAULT_SIG_ALG).build(privKey);

        SubjectPublicKeyInfo subPubKeyInfo = new SubjectPublicKeyInfo(
            ASN1Sequence.getInstance(pubKey.getEncoded()));

        Date startDate = new Date(); //now

        Calendar c = Calendar.getInstance();
        c.setTime(startDate);
        c.add(Calendar.YEAR, 1);
        Date endDate = c.getTime();

        X509v1CertificateBuilder v1CertGen = new X509v1CertificateBuilder(
              subject,
              BigInteger.valueOf(rand.nextLong()),
              startDate, endDate,
              subject,
              subPubKeyInfo);

        X509CertificateHolder certHolder = v1CertGen.build(sigGen);
        return new JcaX509CertificateConverter().getCertificate(certHolder);
    }

}
