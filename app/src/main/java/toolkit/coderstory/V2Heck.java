package toolkit.coderstory;

import static de.robv.android.xposed.XposedHelpers.ClassNotFoundError;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;

import android.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.robv.android.xposed.XposedHelpers;

public final class V2Heck {
    private static final String ANDROID_UTIL_APK_APK_SIGNING_BLOCK_UTILS = "android.util.apk.ApkSigningBlockUtils";
    static ClassLoader classloader;

    private static Class<?> findClass(String clsnm, ClassLoader clsldr) {
        //Fix for UnIntelliJ suggestion
        try {
            return de.robv.android.xposed.XposedHelpers.findClass(clsnm, clsldr);
        } catch (ClassNotFoundError ignored) {
        }
        return null;
    }

    private static <T> T runMethod(String cls, String mt, Object... args) throws XposedHelpers.InvocationTargetError {
        final Class<?> cs = findClass(cls, classloader);
        if (cs != null) {
            try {
                return (T) callStaticMethod(cs, mt, args);
            } catch (NoSuchMethodError ignored) {
                return null;
            }
        }
        return null;
    }

    private static ByteBuffer getLengthPrefixedSlice(ByteBuffer source) throws XposedHelpers.InvocationTargetError, IOException {
        return runMethod(ANDROID_UTIL_APK_APK_SIGNING_BLOCK_UTILS, "getLengthPrefixedSlice", source);
    }

    private static byte[] readLengthPrefixedByteArray(ByteBuffer buf) throws XposedHelpers.InvocationTargetError, IOException {
        return runMethod(ANDROID_UTIL_APK_APK_SIGNING_BLOCK_UTILS, "readLengthPrefixedByteArray", buf);
    }

    private static boolean isSupportedSignatureAlgorithm(int sigAlgorithm) {
        return runMethod(ANDROID_UTIL_APK_APK_SIGNING_BLOCK_UTILS, "isSupportedSignatureAlgorithm", sigAlgorithm);
    }

    private static int compareSignatureAlgorithm(int sigAlgorithm1, int sigAlgorithm2) {
        return runMethod(ANDROID_UTIL_APK_APK_SIGNING_BLOCK_UTILS, "compareSignatureAlgorithm", sigAlgorithm1, sigAlgorithm2);
    }

    private static String getSignatureAlgorithmJcaKeyAlgorithm(int sigAlgorithm) {
        return runMethod(ANDROID_UTIL_APK_APK_SIGNING_BLOCK_UTILS, "getSignatureAlgorithmJcaKeyAlgorithm", sigAlgorithm);
    }

    private static Pair<String, ? extends AlgorithmParameterSpec>
    getSignatureAlgorithmJcaSignatureAlgorithm(int sigAlgorithm) {
        return runMethod(ANDROID_UTIL_APK_APK_SIGNING_BLOCK_UTILS, "getSignatureAlgorithmJcaSignatureAlgorithm", sigAlgorithm);
    }

    private static int getSignatureAlgorithmContentDigestAlgorithm(int sigAlgorithm) {
        return runMethod(ANDROID_UTIL_APK_APK_SIGNING_BLOCK_UTILS, "getSignatureAlgorithmContentDigestAlgorithm", sigAlgorithm);
    }

    private static String getContentDigestAlgorithmJcaDigestAlgorithm(int digestAlgorithm) {
        return runMethod(ANDROID_UTIL_APK_APK_SIGNING_BLOCK_UTILS, "getContentDigestAlgorithmJcaDigestAlgorithm", digestAlgorithm);
    }

    private static X509Certificate makeVerbatimX509Certificate(X509Certificate cer, byte[] dat) throws XposedHelpers.InvocationTargetError {
        Class<?> cs = findClass("android.util.apk.VerbatimX509Certificate", classloader);
        if (cs != null) {
            try {
                return (X509Certificate) XposedHelpers.newInstance(cs, cer, dat);
            } catch (NoSuchMethodError | InstantiationError ignored) {
                return null;
            }
        } else {
            return null;
        }
    }

    private static void verifyAdditionalAttributes(ByteBuffer attrs) {
        runMethod("android.util.apk.ApkSignatureSchemeV2Verifier", "verifyAdditionalAttributes", attrs);
    }

    //modified to allow install by ignoring verify exception
    public static X509Certificate[] verifySigner(
            ByteBuffer signerBlock,
            Map<Integer, byte[]> contentDigests,
            CertificateFactory certFactory) throws SecurityException, IOException {
        ByteBuffer signedData = getLengthPrefixedSlice(signerBlock);
        ByteBuffer signatures = getLengthPrefixedSlice(signerBlock);
        byte[] publicKeyBytes = readLengthPrefixedByteArray(signerBlock);
        int signatureCount = 0;
        int bestSigAlgorithm = -1;
        byte[] bestSigAlgorithmSignatureBytes = null;
        List<Integer> signaturesSigAlgorithms = new ArrayList<>();
        while (signatures.hasRemaining()) {
            signatureCount++;
            try {
                ByteBuffer signature = getLengthPrefixedSlice(signatures);
                if (signature.remaining() < 8) {
                    throw new SecurityException("Signature record too short");
                }
                int sigAlgorithm = signature.getInt();
                signaturesSigAlgorithms.add(sigAlgorithm);
                if (!isSupportedSignatureAlgorithm(sigAlgorithm)) {
                    continue;
                }
                if ((bestSigAlgorithm == -1)
                        || (compareSignatureAlgorithm(sigAlgorithm, bestSigAlgorithm) > 0)) {
                    bestSigAlgorithm = sigAlgorithm;
                    bestSigAlgorithmSignatureBytes = readLengthPrefixedByteArray(signature);
                }
            } catch (IOException | BufferUnderflowException e) {
                throw new SecurityException(
                        "Failed to parse signature record #" + signatureCount,
                        e);
            }
        }
        if (bestSigAlgorithm == -1) {
            if (signatureCount == 0) {
                throw new SecurityException("No signatures found");
            } else {
                throw new SecurityException("No supported signatures found");
            }
        }
        String keyAlgorithm = getSignatureAlgorithmJcaKeyAlgorithm(bestSigAlgorithm);
        Pair<String, ? extends AlgorithmParameterSpec> signatureAlgorithmParams =
                getSignatureAlgorithmJcaSignatureAlgorithm(bestSigAlgorithm);
        String jcaSignatureAlgorithm = signatureAlgorithmParams.first;
        AlgorithmParameterSpec jcaSignatureAlgorithmParams = signatureAlgorithmParams.second;
        boolean sigVerified;
        try {
            PublicKey publicKey =
                    KeyFactory.getInstance(keyAlgorithm)
                            .generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            Signature sig = Signature.getInstance(jcaSignatureAlgorithm);
            sig.initVerify(publicKey);
            if (jcaSignatureAlgorithmParams != null) {
                sig.setParameter(jcaSignatureAlgorithmParams);
            }
            sig.update(signedData);
            sigVerified = sig.verify(bestSigAlgorithmSignatureBytes);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException
                | InvalidAlgorithmParameterException | SignatureException e) {
            throw new SecurityException(
                    "Failed to verify " + jcaSignatureAlgorithm + " signature", e);
        }
        // Signature over signedData why need verify?
        byte[] contentDigest = null;
        signedData.clear();
        ByteBuffer digests = getLengthPrefixedSlice(signedData);
        List<Integer> digestsSigAlgorithms = new ArrayList<>();
        int digestCount = 0;
        while (digests.hasRemaining()) {
            digestCount++;
            try {
                ByteBuffer digest = getLengthPrefixedSlice(digests);
                if (digest.remaining() < 8) {
                    throw new IOException("Record too short");
                }
                int sigAlgorithm = digest.getInt();
                digestsSigAlgorithms.add(sigAlgorithm);
                if (sigAlgorithm == bestSigAlgorithm) {
                    contentDigest = readLengthPrefixedByteArray(digest);
                }
            } catch (IOException | BufferUnderflowException e) {
                throw new IOException("Failed to parse digest record #" + digestCount, e);
            }
        }
        if (!signaturesSigAlgorithms.equals(digestsSigAlgorithms)) {
            throw new SecurityException(
                    "Signature algorithms don't match between digests and signatures records");
        }
        int digestAlgorithm = getSignatureAlgorithmContentDigestAlgorithm(bestSigAlgorithm);
        byte[] previousSignerDigest = contentDigests.put(digestAlgorithm, contentDigest);
        ByteBuffer certificates = getLengthPrefixedSlice(signedData);
        List<X509Certificate> certs = new ArrayList<>();
        int certificateCount = 0;
        while (certificates.hasRemaining()) {
            certificateCount++;
            byte[] encodedCert = readLengthPrefixedByteArray(certificates);
            X509Certificate certificate;
            try {
                certificate = (X509Certificate)
                        certFactory.generateCertificate(new ByteArrayInputStream(encodedCert));
            } catch (CertificateException e) {
                throw new SecurityException("Failed to decode certificate #" + certificateCount, e);
            }
            certificate = makeVerbatimX509Certificate(
                    certificate, encodedCert);
            certs.add(certificate);
        }
        if (certs.isEmpty()) {
            throw new SecurityException("No certificates listed");
        }
        X509Certificate mainCertificate = certs.get(0);
        byte[] certificatePublicKeyBytes = mainCertificate.getPublicKey().getEncoded();
        if (!Arrays.equals(publicKeyBytes, certificatePublicKeyBytes)) {
            throw new SecurityException(
                    "Public key mismatch between certificate and signature record");
        }
        ByteBuffer additionalAttrs = getLengthPrefixedSlice(signedData);
        verifyAdditionalAttributes(additionalAttrs);
        return certs.toArray(new X509Certificate[certs.size()]);
    }
}
