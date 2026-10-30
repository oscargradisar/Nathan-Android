package com.nathan.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** API keys are encrypted by a non-exportable Android Keystore key. */
public final class KeyVault {
    private static SharedPreferences store;
    private static SecretKey key;
    public static synchronized void init(Context context) throws Exception {
        store = context.getSharedPreferences("provider_keys", Context.MODE_PRIVATE);
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
        if (!ks.containsAlias("nathan_provider_keys")) {
            KeyGenerator gen = KeyGenerator.getInstance("AES", "AndroidKeyStore");
            gen.init(new KeyGenParameterSpec.Builder("nathan_provider_keys",
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
            gen.generateKey();
        }
        key = (SecretKey) ks.getKey("nathan_provider_keys", null);
    }
    public static synchronized void put(String provider, String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE, key);
        String encoded = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":" +
            Base64.encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
        if (!store.edit().putString(provider, encoded).commit()) throw new java.io.IOException("Key save failed");
    }
    public static synchronized String get(String provider) throws Exception {
        String encoded = store.getString(provider, null); if (encoded == null) return null;
        String[] parts = encoded.split(":", 2);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }
    public static synchronized void remove(String provider) {
        if (!store.edit().remove(provider).commit()) throw new IllegalStateException("Key removal failed");
    }
}
