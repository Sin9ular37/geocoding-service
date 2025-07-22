package com.example.geocode.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranAddress {
    public static String tranAddressStreet(String address) {

        // 找到第一个"街道"的位置
        int streetIndex = address.indexOf("街道");

        String location = "", name = "";
        if (streetIndex != -1) {
            location = address.substring(0, streetIndex + 2); // +2 包含"街道"
            name = address.substring(streetIndex + 2);      // 剩余部分

            System.out.println("地址部分: " + location); // "黑龙江省哈尔滨市平房区保国街道"
            System.out.println("街道部分: " + name);   // "哈尔滨市第一六二中学校"
        } else {
            System.out.println("未找到'街道'");
        }
        return location;
    }

    public static String tranAddressAcuStreet(String address){
        // 支持 "街道"、"街"、"路"、"巷" 等结尾
        Pattern pattern = Pattern.compile("(.*?(?:街道|街|路|巷))(.*)");
        Matcher matcher = pattern.matcher(address);

        String location = "", name = "";
        if (matcher.matches()) {
            location = matcher.group(1);
            name = matcher.group(2);

            System.out.println("地址部分: " + location);
            System.out.println("街道部分: " + name);
        } else {
            System.out.println("未找到'街道'");
        }
        return location;
    }

    public static String tranAddressAdname(String address) {
        // 找到第一个"街道"的位置
        int streetIndex = address.indexOf("区");

        String location = "", name = "";
        if (streetIndex != -1) {
            location = address.substring(0, streetIndex + 1); // +2 包含"街道"
            name = address.substring(streetIndex + 1);      // 剩余部分

            System.out.println("地址部分: " + location); // "黑龙江省哈尔滨市平房区保国街道"
            System.out.println("区县部分: " + name);   // "哈尔滨市第一六二中学校"
        } else {
            System.out.println("未找到'区县'");
        }
        return location;
    }
}
