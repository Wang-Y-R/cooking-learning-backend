package com.example.cooking.utils;

public class TimeParser {
    public static int parseMinutes(String s){
        // 去掉非数字（暂时只考虑分钟）  eg：10分钟
        String num = s.replaceAll("[^0-9]", "");
        if(num.isEmpty()) return 0;
        return Integer.parseInt(num);
    }
}
