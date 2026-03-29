package com.ar.Interview.util;

public class FileSizeParser {

    public static long parse(String size) {
        size = size.toLowerCase().trim();

        if (size.endsWith("kb")) {
            return Long.parseLong(size.replace("kb", "")) * 1024;
        } else if (size.endsWith("mb")) {
            return Long.parseLong(size.replace("mb", "")) * 1024 * 1024;
        } else {
            throw new IllegalArgumentException("Invalid size format");
        }
    }
}