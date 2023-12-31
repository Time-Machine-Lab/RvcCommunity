package com.tml.utils;

import com.tml.config.SystemConfig;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @Description
 * @Author welsir
 * @Date 2023/12/7 15:18
 */
@Component
public class FileUtil {

    @Resource
    SystemConfig systemConfig;

    private static String imageSize= "";
    private static String modelSize = "";
    private static String[] imageTypes;
    @PostConstruct
    public void init(){
        imageSize = systemConfig.getImageSize();
        modelSize = systemConfig.getModelFileSize();
        imageTypes = systemConfig.getImageType();
    }

    public static boolean isImageFile(String filename) {
        // 获取文件的扩展名
        String extension = getExtension(filename).toLowerCase();

        // 检查扩展名是否在图片扩展名集合中
        return Arrays.asList(imageTypes).contains(extension);
    }

    private static String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }


    public static String getMD5Checksum(InputStream inputStream) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream fis = inputStream) {
            byte[] buffer = new byte[1024];
            int numRead;
            while ((numRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, numRead);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] md5Bytes = md.digest();

        // 将字节数组转换为十六进制值
        BigInteger bigInt = new BigInteger(1, md5Bytes);
        return String.format("%032x", bigInt);
    }
    public static double getFileSizeInKB(MultipartFile file) {
        return (double) file.getSize() / 1024;
    }

    public static boolean imageSizeIsAvailable(MultipartFile file){
        double fileSizeInKB = getFileSizeInKB(file);
        double realImageSize = Double.parseDouble(imageSize);
        return fileSizeInKB>realImageSize;
    }

    public static boolean checkModelFileIsAvailable(MultipartFile file){
        double fileSizeInKB = getFileSizeInKB(file);
        double defaultModelFileSize = Double.parseDouble(modelSize);
        return fileSizeInKB>defaultModelFileSize;
    }

    public static boolean isModelFile(String filename){
        String extension = getExtension(filename).toLowerCase();
        return false;
    }
}
