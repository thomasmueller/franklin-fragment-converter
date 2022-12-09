package com.adobe.franklin.fragments.utils;

public class ProgressLogger {
    static long start = System.currentTimeMillis();
    static long last = System.currentTimeMillis();
    
    public static void logMessage(String message) {
        long now = System.currentTimeMillis();
        long elapsed = now - start;
        long time = now - last;
        last = now;
        if (message.startsWith("done")) {
            if (message.startsWith("done ")) {
                message = "done in " + time + " ms; " + message.substring("done ".length());
            } else {
                message = "done in " + time + " ms";
            }
        } else {
            message += "...";
        }
        System.out.printf("-- %1.2f s %s\n", (elapsed / 1000.), message);
    }

    public static void logDone() {
        logMessage("done");
    }

    public static void logDone(String message) {
        logMessage("done " + message);
    }
}
