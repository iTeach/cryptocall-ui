package org.cryptocall.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.x500.X500Principal;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.spongycastle.asn1.DERObjectIdentifier;
import org.spongycastle.asn1.misc.MiscObjectIdentifiers;
import org.spongycastle.asn1.misc.NetscapeCertType;
import org.spongycastle.asn1.x509.AuthorityKeyIdentifier;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.asn1.x509.GeneralName;
import org.spongycastle.asn1.x509.GeneralNames;
import org.spongycastle.asn1.x509.KeyUsage;
import org.spongycastle.asn1.x509.SubjectKeyIdentifier;
import org.spongycastle.asn1.x509.X509Extensions;
import org.spongycastle.asn1.x509.X509Name;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSecretKeyRingCollection;
import org.spongycastle.x509.X509V3CertificateGenerator;
import org.spongycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.spongycastle.x509.extension.SubjectKeyIdentifierStructure;

//import com.sun.security.auth.callback.TextCallbackHandler;
import java.text.DateFormat;

/**
 * This class aims to provide utility methods to bridge OpenPGP and X.509 credentials. The idea is
 * to produce a self-signed X.509 certificate from a PGP public key, the certificate being signed by
 * the PGP private key. The key and certificate can then be exported in a KeyStore for use as HTTPS
 * client credentials, for example. Currently, the PKCS#12 format is probably the most useful, since
 * it can be used directly as a KeyStore in Java, and can be imported by tools such as Mozilla
 * Firefox, Apple Keychain, and a variety of other web-browsers or e-mail clients.
 * 
 * @author Bruno Harbulot.
 * 
 */
public class PgpX509Bridge {

//    private final static Log LOGGER = LogFactory.getLog(PgpX509Bridge.class);
    public final static String DN_COMMON_PART_O = "OpenPGP to X.509 Bridge";
    public final static String DN_COMMON_PART_OU = "RDFauth Test";

    /**
     * Creates a self-signed certificate from a public and private key. The (critical) key-usage
     * extension is set up with: digital signature, non-repudiation, key-encipherment, key-agreement
     * and certificate-signing. The (non-critical) Netscape extension is set up with: SSL client and
     * S/MIME. A URI subjectAltName may also be set up.
     * 
     * @param pubKey
     *            public key
     * @param privKey
     *            private key
     * @param subject
     *            subject (and issuer) DN for this certificate, RFC 2253 format preferred.
     * @param startDate
     *            date from which the certificate will be valid (defaults to current date and time
     *            if null)
     * @param endDate
     *            date until which the certificate will be valid (defaults to current date and time
     *            if null) *
     * @param subjAltNameURI
     *            URI to be placed in subjectAltName
     * @return self-signed certificate
     * @throws InvalidKeyException
     * @throws SignatureException
     * @throws NoSuchAlgorithmException
     * @throws IllegalStateException
     * @throws NoSuchProviderException
     * @throws CertificateException
     * @throws Exception
     */
    public static X509Certificate createSelfSignedCert(PublicKey pubKey, PrivateKey privKey,
            X509Name subject, Date startDate, Date endDate, String subjAltNameURI)
            throws InvalidKeyException, IllegalStateException, NoSuchAlgorithmException,
            SignatureException, CertificateException, NoSuchProviderException {

        X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();

        certGenerator.reset();
        /*
         * Sets up the subject distinguished name. Since it's a self-signed certificate, issuer and
         * subject are the same.
         */
        certGenerator.setIssuerDN(subject);
        certGenerator.setSubjectDN(subject);

        /*
         * Sets up the validity dates.
         */
        if (startDate == null) {
            startDate = new Date(System.currentTimeMillis());
        }
        certGenerator.setNotBefore(startDate);
        if (endDate == null) {
            endDate = new Date(startDate.getTime() + (365L * 24L * 60L * 60L * 1000L));
            System.out.println("end date is=" + DateFormat.getDateInstance().format(endDate));
        }

        certGenerator.setNotAfter(endDate);

        /*
         * The serial-number of this certificate is 1. It makes sense because it's self-signed.
         */
        certGenerator.setSerialNumber(BigInteger.ONE);
        /*
         * Sets the public-key to embed in this certificate.
         */
        certGenerator.setPublicKey(pubKey);
        /*
         * Sets the signature algorithm.
         */
        String pubKeyAlgorithm = pubKey.getAlgorithm();
        if (pubKeyAlgorithm.equals("DSA")) {
            certGenerator.setSignatureAlgorithm("SHA1WithDSA");
        } else if (pubKeyAlgorithm.equals("RSA")) {
            certGenerator.setSignatureAlgorithm("SHA1WithRSAEncryption");
        } else {
            RuntimeException re = new RuntimeException("Algorithm not recognised: "
                    + pubKeyAlgorithm);
//            LOGGER.error(re.getMessage(), re);
            throw re;
        }

        /*
         * Adds the Basic Constraint (CA: true) extension.
         */
        certGenerator.addExtension(X509Extensions.BasicConstraints, true,
                new BasicConstraints(true));

        /*
         * Adds the Key Usage extension.
         */
        certGenerator.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(
                KeyUsage.digitalSignature | KeyUsage.nonRepudiation | KeyUsage.keyEncipherment
                        | KeyUsage.keyAgreement | KeyUsage.keyCertSign));

        /*
         * Adds the Netscape certificate type extension.
         */
        certGenerator.addExtension(MiscObjectIdentifiers.netscapeCertType, false,
                new NetscapeCertType(NetscapeCertType.sslClient | NetscapeCertType.smime));

        /*
         * Adds the subject key identifier extension.
         */
        SubjectKeyIdentifier subjectKeyIdentifier = new SubjectKeyIdentifierStructure(pubKey);
        certGenerator
                .addExtension(X509Extensions.SubjectKeyIdentifier, false, subjectKeyIdentifier);

        /*
         * Adds the authority key identifier extension.
         */
        AuthorityKeyIdentifier authorityKeyIdentifier = new AuthorityKeyIdentifierStructure(pubKey);
        certGenerator.addExtension(X509Extensions.AuthorityKeyIdentifier, false,
                authorityKeyIdentifier);

        /*
         * Adds the subject alternative-name extension.
         */
        if (subjAltNameURI != null) {
            GeneralNames subjectAltNames = new GeneralNames(new GeneralName(
                    GeneralName.uniformResourceIdentifier, subjAltNameURI));
            certGenerator.addExtension(X509Extensions.SubjectAlternativeName, false,
                    subjectAltNames);
        }

        /*
         * Creates and sign this certificate with the private key corresponding to the public key of
         * the certificate (hence the name "self-signed certificate").
         */
        X509Certificate cert = certGenerator.generate(privKey);

        /*
         * Checks that this certificate has indeed been correctly signed.
         */
        cert.verify(pubKey);

        return cert;
    }

    /**
     * Creates a self-signed certificate from a PGP Secret Key.
     * 
     * @param pgpSecKey
     *            PGP Secret Key (from which one can extract the public and private keys and other
     *            attributes).
     * @param pgpPrivKey
     *            PGP Private Key corresponding to the Secret Key (password callbacks should be done
     *            before calling this method)
     * @param subjAltNameURI
     *            optional URI to embed in the subject alternative-name
     * @return self-signed certificate
     * @throws PGPException
     * @throws NoSuchProviderException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     * @throws CertificateException
     */
    protected static X509Certificate createSelfSignedCert(PGPSecretKey pgpSecKey,
            PGPPrivateKey pgpPrivKey, String subjAltNameURI) throws PGPException,
            NoSuchProviderException, InvalidKeyException, NoSuchAlgorithmException,
            SignatureException, CertificateException {
        PGPPublicKey pgpPubKey = pgpSecKey.getPublicKey();

//        LOGGER.info("Key ID: " + Long.toHexString(pgpPubKey.getKeyID() & 0xffffffffL));

        /*
         * The X.509 Name to be the subject DN is prepared. The CN is extracted from the Secret Key
         * user ID.
         */
        Vector<DERObjectIdentifier> x509NameOids = new Vector<DERObjectIdentifier>();
        Vector<String> x509NameValues = new Vector<String>();

        x509NameOids.add(X509Name.O);
        x509NameValues.add(DN_COMMON_PART_O);

        x509NameOids.add(X509Name.OU);
        x509NameValues.add(DN_COMMON_PART_OU);

        for (@SuppressWarnings("unchecked")
        Iterator<Object> it = (Iterator<Object>) pgpSecKey.getUserIDs(); it.hasNext();) {
            Object attrib = it.next();
            x509NameOids.add(X509Name.CN);
            x509NameValues.add("Henry Story");
            // x509NameValues.add(attrib.toString());
        }

        /*
         * Currently unused.
         */
//        LOGGER.info("User attributes: ");
        for (@SuppressWarnings("unchecked")
        Iterator<Object> it = (Iterator<Object>) pgpSecKey.getUserAttributes(); it.hasNext();) {
            Object attrib = it.next();
//            LOGGER.info(" - " + attrib + " -- " + attrib.getClass());
        }

        X509Name x509name = new X509Name(x509NameOids, x509NameValues);

//        LOGGER.info("Subject DN: " + x509name);

        /*
         * To check the signature from the certificate on the recipient side, the creation time
         * needs to be embedded in the certificate. It seems natural to make this creation time be
         * the "not-before" date of the X.509 certificate. Unlimited PGP keys have a validity of 0
         * second. In this case, the "not-after" date will be the same as the not-before date. This
         * is something that needs to be checked by the service receiving this certificate.
         */
        Date creationTime = pgpPubKey.getCreationTime();
        System.out.println("pgp pub key creation time="
                + DateFormat.getDateInstance().format(creationTime));
        System.out.println("pgp valid seconds=" + pgpPubKey.getValidSeconds());
        Date validTo = null;
        if (pgpPubKey.getValidSeconds() > 0)
            validTo = new Date(creationTime.getTime() + 1000L * pgpPubKey.getValidSeconds());

        X509Certificate selfSignedCert = createSelfSignedCert(pgpPubKey.getKey("BC"),
                pgpPrivKey.getKey(), x509name, creationTime, validTo, subjAltNameURI);

        return selfSignedCert;
    }

    public final static String PGP_KEY_PASSWORD_PROMPT = "Password for the PGP key? ";
    public final static String KEYSTORE_KEY_PASSWORD_PROMPT = "Password for the key in the KeyStore? ";
    public final static String KEYSTORE_PASSWORD_PROMPT = "Password for the KeyStore? ";

    /**
     * Creates a self-seigned certificate from a PGP Secret Key and stores it in a Java KeyStore. It
     * also calls back the user-interface for passwords if needed.
     * 
     * @param keyStore
     *            KeyStore (must use "load" and "save" b efore and after call this methods,
     *            respectively)
     * @param pgpSecKey
     *            PGP Secret Key from which to get the public and private keys.
     * @param subjAltNameURI
     *            optional URI to embed in the subject alternative-name
     * @param pgpPwdCallbackHandler
     *            CallbackHandler to read the PGP password
     * @param keystorePwdCallbackHandler
     *            Callback Handler to read the key password in the Keystore
     * @throws PGPException
     * @throws NoSuchProviderException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws IOException
     * @throws UnsupportedCallbackException
     */
    public static void createAndSaveSelfSignedCert(KeyStore keyStore, PGPSecretKey pgpSecKey,
            String subjAltNameURI, CallbackHandler pgpPwdCallbackHandler,
            CallbackHandler keystorePwdCallbackHandler) throws PGPException,
            NoSuchProviderException, InvalidKeyException, NoSuchAlgorithmException,
            SignatureException, CertificateException, KeyStoreException, IOException,
            UnsupportedCallbackException {

        PasswordCallback pgpSecKeyPasswordCallBack = new PasswordCallback(PGP_KEY_PASSWORD_PROMPT,
                false);
        pgpPwdCallbackHandler.handle(new Callback[] { pgpSecKeyPasswordCallBack });
        PGPPrivateKey pgpPrivKey = pgpSecKey.extractPrivateKey(
                pgpSecKeyPasswordCallBack.getPassword(), "BC");
        pgpSecKeyPasswordCallBack.clearPassword();

        X509Certificate selfSignedCert = createSelfSignedCert(pgpSecKey, pgpPrivKey, subjAltNameURI);

        PasswordCallback keystorePasswordCallBack = new PasswordCallback(
                KEYSTORE_KEY_PASSWORD_PROMPT, false);
        keystorePwdCallbackHandler.handle(new Callback[] { keystorePasswordCallBack });
        keyStore.setKeyEntry(selfSignedCert.getSubjectX500Principal()
                .getName(X500Principal.RFC2253), pgpPrivKey.getKey(), keystorePasswordCallBack
                .getPassword(), new Certificate[] { selfSignedCert });
        keystorePasswordCallBack.clearPassword();
    }

    /**
     * Creates a self-seigned certificate from a PGP Secret Key and stores it in a PKCS#12 file. It
     * also calls back the user-interface for passwords if needed. In the PKCS#12 keystore, the key
     * password must be the same as the keystore password.
     * 
     * @param pgpSecKey
     * @param subjAltNameURI
     * @param pgpPwdCallbackHandler
     * @param keystorePwdCallbackHandler
     * @param pkcs12Filename
     * @throws PGPException
     * @throws NoSuchProviderException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws IOException
     * @throws UnsupportedCallbackException
     */
    public static void createAndSaveSelfSignedCertInPKCS12(PGPSecretKey pgpSecKey,
            String subjAltNameURI, CallbackHandler pgpPwdCallbackHandler,
            CallbackHandler keystorePwdCallbackHandler, String pkcs12Filename) throws PGPException,
            NoSuchProviderException, InvalidKeyException, NoSuchAlgorithmException,
            SignatureException, CertificateException, KeyStoreException, IOException,
            UnsupportedCallbackException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        Iterator userIDs = pgpSecKey.getUserIDs();
        while (userIDs.hasNext()) {
            System.out.println("id=" + userIDs.next());
        }

        Iterator attributes = pgpSecKey.getUserAttributes();
        while (attributes.hasNext()) {
            System.out.println("attributes" + attributes.next());
        }

        System.out.println("key is " + (pgpSecKey.isMasterKey() ? "is" : "is not")
                + " a master key");
        System.out.println("key is " + (pgpSecKey.isSigningKey() ? "is" : "is not")
                + " a signing key");
        System.out.println("its public key was created on "
                + pgpSecKey.getPublicKey().getCreationTime());
        System.out.println("its public key is "
                + (pgpSecKey.getPublicKey().isRevoked() ? "is" : "is not") + " revoked");

        PasswordCallback keystorePasswordCallBack = new PasswordCallback(KEYSTORE_PASSWORD_PROMPT,
                false);
        keystorePwdCallbackHandler.handle(new Callback[] { keystorePasswordCallBack });
        PredefinedPasswordCallbackHandler predefinedPasswordCallbackHandler = new PredefinedPasswordCallbackHandler(
                keystorePasswordCallBack.getPassword(), KEYSTORE_KEY_PASSWORD_PROMPT);

        File keyStorefile = new File(pkcs12Filename);
        FileInputStream fis = null;
        if (keyStorefile.exists()) {
            fis = new FileInputStream(keyStorefile);
        }
        keyStore.load(fis, keystorePasswordCallBack.getPassword());

        createAndSaveSelfSignedCert(keyStore, pgpSecKey, subjAltNameURI, pgpPwdCallbackHandler,
                predefinedPasswordCallbackHandler);

        keyStore.store(new FileOutputStream(keyStorefile), keystorePasswordCallBack.getPassword());
        keystorePasswordCallBack.clearPassword();
    }

    /**
     * Creates a self-seigned certificate from a PGP Secret Key and stores it in the Apple Keychain.
     * This will throw a NoSuchProviderException or KeyStoreException if you're not using the Apple
     * JVM (or at least its security provider).
     * 
     * @param pgpSecKey
     * @param subjAltNameURI
     * @param pgpPwdCallbackHandler
     * @throws PGPException
     * @throws NoSuchProviderException
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     * @throws SignatureException
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws IOException
     * @throws UnsupportedCallbackException
     */
    public static void createAndSaveSelfSignedCertInOSXKeychain(PGPSecretKey pgpSecKey,
            String subjAltNameURI, CallbackHandler pgpPwdCallbackHandler) throws PGPException,
            NoSuchProviderException, InvalidKeyException, NoSuchAlgorithmException,
            SignatureException, CertificateException, KeyStoreException, IOException,
            UnsupportedCallbackException {
        KeyStore keyStore = KeyStore.getInstance("KeychainStore");

        PredefinedPasswordCallbackHandler predefinedPasswordCallbackHandler = new PredefinedPasswordCallbackHandler(
                "-".toCharArray(), KEYSTORE_KEY_PASSWORD_PROMPT);

        keyStore.load(null, "-".toCharArray());

        createAndSaveSelfSignedCert(keyStore, pgpSecKey, subjAltNameURI, pgpPwdCallbackHandler,
                predefinedPasswordCallbackHandler);

        keyStore.store(null, "-".toCharArray());
    }

    private static void usage(String message) {
        System.out.println("error: " + message);
        System.out.println("");
        System.out.println("PgpX509Bridge -gpg secretRing [-foaf idurl] [ -out outputFile ]");
        System.out.println("args:");
        System.out.println("  secretRing: a file such as ~/.gnupg/secring.gpg");
        System.out.println("  idurl: foaf id, eg: http://bblfish.net/people/henry/card");
        System.out.println("  outputFile: file to send output to, if missing standard out");
        System.out
                .println("the pgp passoword can be passed as a property (someone who needs this, fill in details)");
        System.exit(-1);
    }

//    /**
//     * Usage: java -Dpgp509bridge.pgpPassword=XXXX -Dpgp509bridge.keystorePassword=YYYYYY
//     * ~/.gnupg/secring.gpg ~/test.p12
//     * 
//     * -Dpgp509bridge.pgpPassword=XXXX -Dpgp509bridge.keystorePassword=YYYYYY are optional and
//     * should not be used for anything else than testing (putting password in readable system
//     * properties could be read by things that are not supposed to if the security manager isn't
//     * configured properly).
//     * 
//     * if the output file is not given then it will try to save the key in the OSX keychain
//     * 
//     * 
//     * @param args
//     * @throws Exception
//     */
//    public static void main(String[] args) throws Exception {
//        String gpgSecRingFilename = null;
//        String foafid = null;
//        String pkcs12Filename = null;
//
//        for (int i = 0; i < args.length; i++) {
//            if ("-gpg".equals(args[i])) {
//                gpgSecRingFilename = args[++i];
//            } else if ("-foaf".equals(args[i])) {
//                foafid = args[++i];
//            } else if ("-out".equals(args[i])) {
//                pkcs12Filename = args[++i];
//            }
//        }
//
//        if (gpgSecRingFilename == null) {
//            usage("missing secret ring file");
//        }
//        if (pkcs12Filename == null) {
//            usage("missing pkcs12 output file name");
//        }
//
//        Provider p = new BouncyCastleProvider();
//        Security.addProvider(p);
//
//        CallbackHandler pgpPwdCallbackHandler = null;
//        CallbackHandler keystorePwdCallbackHandler = null;
//
//        String pgpPassword = System.getProperty("pgp509bridge.pgpPassword");
//        if (pgpPassword != null) {
//            pgpPwdCallbackHandler = new PredefinedPasswordCallbackHandler(pgpPassword,
//                    PGP_KEY_PASSWORD_PROMPT);
//        } else {
//            pgpPwdCallbackHandler = new TextCallbackHandler();
//        }
//        String keystorePassword = System.getProperty("pgp509bridge.keystorePassword");
//        if (keystorePassword != null) {
//            keystorePwdCallbackHandler = new PredefinedPasswordCallbackHandler(keystorePassword,
//                    KEYSTORE_PASSWORD_PROMPT);
//        } else {
//            keystorePwdCallbackHandler = new TextCallbackHandler();
//        }
//
//        FileInputStream secringFileInputStream = new FileInputStream(gpgSecRingFilename);
//
//        PGPSecretKeyRingCollection pgpSecKeyRings = new PGPSecretKeyRingCollection(
//                secringFileInputStream);
//        @SuppressWarnings("unchecked")
//        Iterator<PGPSecretKeyRing> ringIt = (Iterator<PGPSecretKeyRing>) pgpSecKeyRings
//                .getKeyRings();
//
//        while (ringIt.hasNext()) {
//            PGPSecretKeyRing pgpSecKeyRing = ringIt.next();
//            @SuppressWarnings("unchecked")
//            Iterator<PGPSecretKey> keyIt = (Iterator<PGPSecretKey>) pgpSecKeyRing.getSecretKeys();
//
//            while (keyIt.hasNext()) {
//                PGPSecretKey pgpSecKey = keyIt.next();
//
//                if (pgpSecKey != null) {
//
//                    if (pkcs12Filename != null) {
//                        createAndSaveSelfSignedCertInPKCS12(pgpSecKey, foafid,
//                                pgpPwdCallbackHandler, keystorePwdCallbackHandler, pkcs12Filename);
//                    } else {
//                        createAndSaveSelfSignedCertInOSXKeychain(pgpSecKey, foafid,
//                                pgpPwdCallbackHandler);
//                    }
//
//                    break;
//                }
//            }
//        }
//    }

    /**
     * This is a password callback handler that will fill in a password automatically. Useful to
     * configure passwords in advance, but should be used with caution depending on how much you
     * allow passwords to be stored within your application.
     * 
     * @author Bruno Harbulot.
     * 
     */
    public final static class PredefinedPasswordCallbackHandler implements CallbackHandler {

        private char[] password;
        private String prompt;

        public PredefinedPasswordCallbackHandler(String password) {
            this(password == null ? null : password.toCharArray(), null);
        }

        public PredefinedPasswordCallbackHandler(char[] password) {
            this(password, null);
        }

        public PredefinedPasswordCallbackHandler(String password, String prompt) {
            this(password == null ? null : password.toCharArray(), prompt);
        }

        public PredefinedPasswordCallbackHandler(char[] password, String prompt) {
            this.password = password;
            this.prompt = prompt;
        }

        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof PasswordCallback) {
                    PasswordCallback pwCallback = (PasswordCallback) callback;
                    if ((this.prompt == null) || (this.prompt.equals(pwCallback.getPrompt()))) {
                        pwCallback.setPassword(this.password);
                    }
                } else {
                    throw new UnsupportedCallbackException(callback, "Unrecognised callback.");
                }
            }
        }

        protected final Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException();
        }
    }
}
