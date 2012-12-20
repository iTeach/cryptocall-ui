package org.cryptocall.util;

import java.io.IOException;

import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;

public class PgpHelper {
    /**
     * Convert from byte[] to PGPKeyRing
     * 
     * @param keysBytes
     * @return
     */
    public static PGPKeyRing BytesToPGPKeyRing(byte[] keysBytes) {
        PGPObjectFactory factory = new PGPObjectFactory(keysBytes);
        PGPKeyRing keyRing = null;
        try {
            if ((keyRing = (PGPKeyRing) factory.nextObject()) == null) {
                Log.e(Constants.TAG, "No keys given!");
            }
        } catch (IOException e) {
            Log.e(Constants.TAG, "Error while converting to PGPKeyRing!", e);
        }

        return keyRing;
    }
}
