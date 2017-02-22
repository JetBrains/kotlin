package org.jetbrains.kotlin.maven.kapt;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class KaptOption {
    @NotNull
    private final String key;

    @NotNull
    private final String value;

    KaptOption(@NotNull String key, boolean value) {
        this(key, String.valueOf(value));
    }

    KaptOption(@NotNull String key, @Nullable String[] value) {
        this(key, renderStringArray(value));
    }

    KaptOption(@NotNull String key, @Nullable String value) {
        this.key = key;
        this.value = String.valueOf(value);
    }

    @NotNull
    private static String renderStringArray(@Nullable String[] arr) {
        if (arr == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (String s : arr) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(s);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "plugin:org.jetbrains.kotlin.kapt3:" + key + "=" + value;
    }
}
