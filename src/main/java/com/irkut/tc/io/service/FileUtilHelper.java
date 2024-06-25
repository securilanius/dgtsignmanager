package com.irkut.tc.io.service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileUtilHelper {

    public static boolean compareBinaryFiles(String file1Path, String file2Path) {
        try (FileInputStream fis1 = new FileInputStream(file1Path);
             FileInputStream fis2 = new FileInputStream(file2Path)) {
            int byte1, byte2;

            while ((byte1 = fis1.read()) != -1) {
                byte2 = fis2.read();

                if (byte1 != byte2) {
                    return false; // files are not identical
                }
            }

            // Check if the second file has any extra bytes
            if (fis2.read() != -1) {
                return false; // files are not identical
            }

            return true; // files are identical
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    public static boolean compareFiles(String file1Path, String file2Path) {
        try {
            File file1 = new File(file1Path);
            File file2 = new File(file2Path);

            byte[] file1Hash = hashFile(file1);
            byte[] file2Hash = hashFile(file2);

            return MessageDigest.isEqual(file1Hash, file2Hash);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static byte[] hashFile(File file) throws IOException, NoSuchAlgorithmException {
        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            return digest.digest();
        }
    }
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String calculateFileHash(String filePath){
        String hexHash = "";

        try {
            // Чтение всех байтов файла
            byte[] fileContent = Files.readAllBytes(Paths.get(filePath));

            // Создание экземпляра MessageDigest для алгоритма SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // Обновление дайджеста с содержимым файла
            digest.update(fileContent);

            // Вычисление хэш-кода
            byte[] hashCode = digest.digest();

            // Преобразование хэш-кода в строку
            hexHash = bytesToHex(hashCode);
            System.out.println("Хэш-код файла: " + hexHash);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Алгоритм хэширования не найден: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Ошибка при чтении файла: " + e.getMessage());
        }

        return (hexHash == null || hexHash.length() == 0) ? "" : hexHash;
    }

    public static String calculateFileHash_debug(String filePath, String algorithm) {
        // Создаем объект MessageDigest для алгоритма хеширования
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

        // Читаем данные файла и обновляем MessageDigest
        try (InputStream fis = new FileInputStream(filePath)) {
            byte[] byteArray = new byte[1024];
            int bytesCount;

            while ((bytesCount = fis.read(byteArray)) != -1) {
                digest.update(byteArray, 0, bytesCount);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        // Получаем хеш-код в виде массива байт
        byte[] bytes = digest.digest();

        // Преобразуем массив байт в шестнадцатеричный формат
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        // Возвращаем хеш-код в виде строки
        return sb.toString();
    }

}