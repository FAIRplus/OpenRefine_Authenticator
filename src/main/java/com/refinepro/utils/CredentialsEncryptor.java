package com.refinepro.utils;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

public class CredentialsEncryptor {

    private static final StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
    private final static String algorithm = "PBEWithMD5AndDES";
    private final static String password = "T]T,L92B0gCvep:ZC.LQSv'[i@(ne9p1";

    public static String encrypt(String value) {
        if (!encryptor.isInitialized()) {
            encryptor.setAlgorithm(algorithm);
            encryptor.setPassword(password);
        }
        return encryptor.encrypt(value);
    }

    public static String decrypt(String value) {
        if (!encryptor.isInitialized()) {
            encryptor.setAlgorithm(algorithm);
            encryptor.setPassword(password);
        }
        return encryptor.decrypt(value);
    }
}
