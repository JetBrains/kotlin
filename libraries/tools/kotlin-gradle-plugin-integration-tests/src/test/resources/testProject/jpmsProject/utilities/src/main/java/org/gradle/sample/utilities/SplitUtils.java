package org.gradle.sample.utilities;

import org.gradle.sample.list.LinkedList;

class SplitUtils {
    public static LinkedList split(String source) {
        int lastFind = 0;
        int currentFind = 0;
        LinkedList result = new LinkedList();

        while ((currentFind = source.indexOf(" ", lastFind)) != -1) {
            String token = source.substring(lastFind);
            if (currentFind != -1) {
                token = token.substring(0, currentFind - lastFind);
            }

            addIfValid(token, result);
            lastFind = currentFind + 1;
        }

        String token = source.substring(lastFind);
        addIfValid(token, result);

        return result;
    }

    private static void addIfValid(String token, LinkedList list) {
        if (isTokenValid(token)) {
            list.add(token);
        }
    }

    private static boolean isTokenValid(String token) {
        return !token.isEmpty();
    }
}
