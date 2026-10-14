package com.gamedev.aistudio;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** AndroidKeyStore-encrypted GitHub token; never exported to backups. */
final class SecureTokenStore {
    private static final String ALIAS="gamedev_aistudio_token_key_v1";
    private static final String PREF="gamedev_aistudio_private";
    private static final String FIELD="encrypted_credential";
    private SecureTokenStore() {}

    static void save(Context context,String token) throws Exception {
        if(token==null||token.trim().length()<20)
            throw new IllegalArgumentException("Informe um token GitHub válido (mínimo de 20 caracteres).");
        SecretKey key=key();
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,key);
        byte[] iv=cipher.getIV();
        byte[] encrypted=cipher.doFinal(token.trim().getBytes(StandardCharsets.UTF_8));
        byte[] blob=ByteBuffer.allocate(4+iv.length+encrypted.length)
            .putInt(iv.length).put(iv).put(encrypted).array();
        boolean ok=prefs(context).edit().putString(FIELD,Base64.encodeToString(blob,Base64.NO_WRAP)).commit();
        if(!ok) throw new IllegalStateException("Não foi possível salvar a configuração.");
    }

    static String read(Context context) {
        String encoded=prefs(context).getString(FIELD,"");
        if(encoded==null||encoded.isEmpty()) return "";
        try {
            byte[] blob=Base64.decode(encoded,Base64.DEFAULT);
            ByteBuffer bb=ByteBuffer.wrap(blob);
            int len=bb.getInt();
            if(len!=12) throw new IllegalStateException("Token salvo inválido");
            byte[] iv=new byte[len];
            bb.get(iv);
            byte[] ciphertext=new byte[bb.remaining()];
            bb.get(ciphertext);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,iv));
            return new String(cipher.doFinal(ciphertext),StandardCharsets.UTF_8);
        } catch(Exception e) {
            // A token encrypted under a missing/invalidated Keystore key cannot be recovered.
            return "";
        }
    }

    static void clear(Context context) {
        prefs(context).edit().remove(FIELD).commit();
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
    }

    private static SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if(!store.containsAlias(ALIAS)){
            KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).build());
            return generator.generateKey();
        }
        return ((KeyStore.SecretKeyEntry)store.getEntry(ALIAS,null)).getSecretKey();
    }
}
