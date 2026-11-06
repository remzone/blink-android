package com.remzone.blink.tv;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Feed URLs and VPN credentials stay in Keystore-encrypted, non-backed-up app storage. */
final class SecureStore {
    private static final String ALIAS="blink-subscription-v1";
    private final SharedPreferences prefs;
    SecureStore(Context c){prefs=c.getSharedPreferences("subscription",Context.MODE_PRIVATE);}
    private static synchronized SecretKey key()throws Exception{
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(ALIAS))return (SecretKey)ks.getKey(ALIAS,null);
        KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build());return gen.generateKey();
    }
    JSONObject load()throws Exception{synchronized(SecureStore.class){return loadLocked();}}
    private JSONObject loadLocked()throws Exception{
        String value=prefs.getString("sealed",null);if(value==null)return new JSONObject();
        try{
            JSONObject sealed=new JSONObject(value);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(sealed.getString("iv"),Base64.NO_WRAP)));
            return new JSONObject(new String(cipher.doFinal(Base64.decode(sealed.getString("data"),Base64.NO_WRAP)),StandardCharsets.UTF_8));
        }catch(Exception ex){throw new Exception("Не удалось прочитать защищённую подписку. Импортируйте её заново.");}
    }
    void save(JSONObject value)throws Exception{synchronized(SecureStore.class){saveLocked(value);}}
    private void saveLocked(JSONObject value)throws Exception{
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        JSONObject sealed=new JSONObject().put("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).put("data",Base64.encodeToString(cipher.doFinal(value.toString().getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP));
        if(!prefs.edit().putString("sealed",sealed.toString()).commit())throw new Exception("Не удалось сохранить подписку");
    }
    interface Mutation{void apply(JSONObject current)throws Exception;}
    JSONObject update(Mutation mutation)throws Exception{synchronized(SecureStore.class){JSONObject current=loadLocked();mutation.apply(current);saveLocked(current);return current;}}
    void clear(){synchronized(SecureStore.class){prefs.edit().clear().apply();}}
    Subscription subscription()throws Exception{return Subscription.parse(load().getString("document"),System.currentTimeMillis()/1000,0);}
}
