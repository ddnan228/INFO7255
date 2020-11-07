package com.example.restservice.authClient;

import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestHeader;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class AuthClient {
    private static String finalKey = "doudounan1989022";

    public String getToken() throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        // create token
        JSONObject object = new JSONObject();
        object.put("organization", "example.com");
        object.put("user", "sample");

        // add expired time for 1 hour
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 60);
        Date date = calendar.getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        object.put("expiredAt", df.format(date));

        String token = object.toString();
        System.out.println("Token values is " + token);
        System.out.println("ExpiredTime is : " + object.get("expiredAt"));


        String initVector = "RandomInitVector";
        IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
        SecretKeySpec skeySpec = new SecretKeySpec(finalKey.getBytes("UTF-8"), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

        byte[] encrypted = cipher.doFinal(token.getBytes());

        // encoded token (Base64 encoding)
        String finalToken = org.apache.tomcat.util.codec.binary.Base64.encodeBase64String(encrypted);
        System.out.println(finalToken);
        return finalToken;
    }

    public boolean validateToken(@RequestHeader HttpHeaders headers) {
        String token  = headers.getFirst("Authorization");
        if(token == null || token.isEmpty()) {
            System.out.println("no token");
            return false;
        }

        if(!token.contains("Bearer ")) {
            System.out.println("not Bearer");
            return false;
        }

        token = token.substring(7);
        System.out.println("token：" + token);

        try {
            String initVector = "RandomInitVector";
            IvParameterSpec iv = new IvParameterSpec(initVector.getBytes("UTF-8"));
            SecretKeySpec skeySpec = new SecretKeySpec(finalKey.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            byte[] original = cipher.doFinal(org.apache.tomcat.util.codec.binary.Base64.decodeBase64(token));
            String jsonDecoded = new String(original);

            JSONObject jsonObject = new JSONObject(jsonDecoded);
            String expiredTime = jsonObject.get("expiredAt").toString();

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date expired = formatter.parse(expiredTime);

            Calendar calendar = Calendar.getInstance();
            Date curr = calendar.getTime();

            if(!curr.before(expired)){
                System.out.println("Token is expired.");
                return false;
            }
            return true;

        } catch(Exception e) {
            System.out.println("Exception at validateToken: " + e);
            return false;
        }

    }
}
