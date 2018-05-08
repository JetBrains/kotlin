/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.tools.klint.checks;

import static com.android.tools.klint.checks.PluralsDatabase.Quantity.few;
import static com.android.tools.klint.checks.PluralsDatabase.Quantity.many;
import static com.android.tools.klint.checks.PluralsDatabase.Quantity.one;
import static com.android.tools.klint.checks.PluralsDatabase.Quantity.two;
import static com.android.tools.klint.checks.PluralsDatabase.Quantity.zero;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.klint.detector.api.LintUtils;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * Database used by the {@link PluralsDetector} to get information
 * about plural forms for a given language
 */
public class PluralsDatabase {
    private static final EnumSet<Quantity> NONE = EnumSet.noneOf(Quantity.class);

    private static final PluralsDatabase sInstance = new PluralsDatabase();
    private final Map<String, EnumSet<Quantity>> mPlurals = Maps.newHashMap();

    /** Bit set if this language uses quantity zero */
    @SuppressWarnings("PointlessBitwiseExpression")
    static final int FLAG_ZERO  = 1 << 0;
    /** Bit set if this language uses quantity one */
    static final int FLAG_ONE   = 1 << 1;
    /** Bit set if this language uses quantity two */
    static final int FLAG_TWO   = 1 << 2;
    /** Bit set if this language uses quantity few */
    static final int FLAG_FEW   = 1 << 3;
    /** Bit set if this language uses quantity many */
    static final int FLAG_MANY  = 1 << 4;
    /** Bit set if this language has multiple values that match quantity zero */
    static final int FLAG_MULTIPLE_ZERO = 1 << 5;
    /** Bit set if this language has multiple values that match quantity one */
    static final int FLAG_MULTIPLE_ONE  = 1 << 6;
    /** Bit set if this language has multiple values that match quantity two */
    static final int FLAG_MULTIPLE_TWO  = 1 << 7;

    @NonNull
    public static PluralsDatabase get() {
        return sInstance;
    }

    private static int getFlags(@NonNull String language) {
        int index = getLanguageIndex(language);
        if (index != -1) {
            return FLAGS[index];
        }
        return 0;
    }

    private static int getLanguageIndex(@NonNull String language) {
        int index = Arrays.binarySearch(LANGUAGE_CODES, language);
        if (index >= 0) {
            assert LANGUAGE_CODES[index].equals(language);
            return index;
        } else {
            return -1;
        }
    }

    @Nullable
    public EnumSet<Quantity> getRelevant(@NonNull String language) {
        EnumSet<Quantity> set = mPlurals.get(language);
        if (set == null) {
            int index = getLanguageIndex(language);
            if (index == -1) {
                mPlurals.put(language, NONE);
                return null;
            }

            // Process each item and look for relevance
            int flag = FLAGS[index];

            set = EnumSet.noneOf(Quantity.class);
            if ((flag & FLAG_ZERO) != 0) {
                set.add(zero);
            }
            if ((flag & FLAG_ONE) != 0) {
                set.add(one);
            }
            if ((flag & FLAG_TWO) != 0) {
                set.add(two);
            }
            if ((flag & FLAG_FEW) != 0) {
                set.add(few);
            }
            if ((flag & FLAG_MANY) != 0) {
                set.add(many);
            }

            mPlurals.put(language, set);
        }
        return set == NONE ? null : set;
    }

    @SuppressWarnings("MethodMayBeStatic")
    public boolean hasMultipleValuesForQuantity(
            @NonNull String language,
            @NonNull Quantity quantity) {
        if (quantity == one) {
            return (getFlags(language) & FLAG_MULTIPLE_ONE) != 0;
        } else if (quantity == two) {
            return (getFlags(language) & FLAG_MULTIPLE_TWO) != 0;
        } else {
            return quantity == zero && (getFlags(language) & FLAG_MULTIPLE_ZERO) != 0;
        }
    }

    @SuppressWarnings("MethodMayBeStatic")
    @Nullable
    public String findIntegerExamples(@NonNull String language, @NonNull Quantity quantity) {
        if (quantity == one) {
            return getExampleForQuantityOne(language);
        } else if (quantity == two) {
            return getExampleForQuantityTwo(language);
        } else if (quantity == zero) {
            return getExampleForQuantityZero(language);
        } else {
            return null;
        }
    }

    public enum Quantity {
        // deliberately lower case to match attribute names
        few, many, one, two, zero, other;

        @Nullable
        public static Quantity get(@NonNull String name) {
            for (Quantity quantity : values()) {
                if (name.equals(quantity.name())) {
                    return quantity;
                }
            }

            return null;
        }

        public static String formatSet(@NonNull EnumSet<Quantity> set) {
            List<String> list = new ArrayList<String>(set.size());
            for (Quantity quantity : set) {
                list.add('`' + quantity.name() + '`');
            }
            return LintUtils.formatList(list, Integer.MAX_VALUE);
        }
    }

    // GENERATED DATA.
    // This data is generated by the #testDatabaseAccurate method in PluralsDatabaseTest
    // which will generate the following if it can find an ICU plurals database file
    // in the unit test data folder.

    /** Set of language codes relevant to plurals data */
    private static final String[] LANGUAGE_CODES = new String[] {
            "af", "ak", "am", "ar", "as", "az", "be", "bg", "bh", "bm",
            "bn", "bo", "br", "bs", "ca", "ce", "cs", "cy", "da", "de",
            "dv", "dz", "ee", "el", "en", "eo", "es", "et", "eu", "fa",
            "ff", "fi", "fo", "fr", "fy", "ga", "gd", "gl", "gu", "gv",
            "ha", "he", "hi", "hr", "hu", "hy", "id", "ig", "ii", "in",
            "is", "it", "iu", "iw", "ja", "ji", "jv", "ka", "kk", "kl",
            "km", "kn", "ko", "ks", "ku", "kw", "ky", "lb", "lg", "ln",
            "lo", "lt", "lv", "mg", "mk", "ml", "mn", "mr", "ms", "mt",
            "my", "nb", "nd", "ne", "nl", "nn", "no", "nr", "ny", "om",
            "or", "os", "pa", "pl", "ps", "pt", "rm", "ro", "ru", "se",
            "sg", "si", "sk", "sl", "sn", "so", "sq", "sr", "ss", "st",
            "sv", "sw", "ta", "te", "th", "ti", "tk", "tl", "tn", "to",
            "tr", "ts", "ug", "uk", "ur", "uz", "ve", "vi", "vo", "wa",
            "wo", "xh", "yi", "yo", "zh", "zu"
    };

    /**
     * Relevant flags for each language (corresponding to each language listed
     * in the same position in {@link #LANGUAGE_CODES})
     */
    private static final int[] FLAGS = new int[] {
            0x0002, 0x0042, 0x0042, 0x001f, 0x0042, 0x0002, 0x005a, 0x0002,
            0x0042, 0x0000, 0x0042, 0x0000, 0x00de, 0x004a, 0x0002, 0x0002,
            0x000a, 0x001f, 0x0002, 0x0002, 0x0002, 0x0000, 0x0002, 0x0002,
            0x0002, 0x0002, 0x0002, 0x0002, 0x0002, 0x0042, 0x0042, 0x0002,
            0x0002, 0x0042, 0x0002, 0x001e, 0x00ce, 0x0002, 0x0042, 0x00ce,
            0x0002, 0x0016, 0x0042, 0x004a, 0x0002, 0x0042, 0x0000, 0x0000,
            0x0000, 0x0000, 0x0042, 0x0002, 0x0006, 0x0016, 0x0000, 0x0002,
            0x0000, 0x0002, 0x0002, 0x0002, 0x0000, 0x0042, 0x0000, 0x0002,
            0x0002, 0x0006, 0x0002, 0x0002, 0x0002, 0x0042, 0x0000, 0x004a,
            0x0063, 0x0042, 0x0042, 0x0002, 0x0002, 0x0042, 0x0000, 0x001a,
            0x0000, 0x0002, 0x0002, 0x0002, 0x0002, 0x0002, 0x0002, 0x0002,
            0x0002, 0x0002, 0x0002, 0x0002, 0x0042, 0x001a, 0x0002, 0x0042,
            0x0002, 0x000a, 0x005a, 0x0006, 0x0000, 0x0042, 0x000a, 0x00ce,
            0x0002, 0x0002, 0x0002, 0x004a, 0x0002, 0x0002, 0x0002, 0x0002,
            0x0002, 0x0002, 0x0000, 0x0042, 0x0002, 0x0042, 0x0002, 0x0000,
            0x0002, 0x0002, 0x0002, 0x005a, 0x0002, 0x0002, 0x0002, 0x0000,
            0x0002, 0x0042, 0x0000, 0x0002, 0x0002, 0x0000, 0x0000, 0x0042
    };

    @Nullable
    private static String getExampleForQuantityZero(@NonNull String language) {
        int index = getLanguageIndex(language);
        switch (index) {
            // set14
            case 72: // lv
                return "0, 10~20, 30, 40, 50, 60, 100, 1000, 10000, 100000, 1000000, \u2026";
            case -1:
            default:
                return null;
        }
    }

    @Nullable
    private static String getExampleForQuantityOne(@NonNull String language) {
        int index = getLanguageIndex(language);
        switch (index) {
            // set1
            case 2: // am
            case 4: // as
            case 10: // bn
            case 29: // fa
            case 38: // gu
            case 42: // hi
            case 61: // kn
            case 77: // mr
            case 135: // zu
                return "0, 1";
            // set11
            case 50: // is
                return "1, 21, 31, 41, 51, 61, 71, 81, 101, 1001, \u2026";
            // set12
            case 74: // mk
                return "1, 11, 21, 31, 41, 51, 61, 71, 101, 1001, \u2026";
            // set13
            case 117: // tl
                return "0~3, 5, 7, 8, 10~13, 15, 17, 18, 20, 21, 100, 1000, 10000, 100000, 1000000, \u2026";
            // set14
            case 72: // lv
                return "1, 21, 31, 41, 51, 61, 71, 81, 101, 1001, \u2026";
            // set2
            case 30: // ff
            case 33: // fr
            case 45: // hy
                return "0, 1";
            // set20
            case 13: // bs
            case 43: // hr
            case 107: // sr
                return "1, 21, 31, 41, 51, 61, 71, 81, 101, 1001, \u2026";
            // set21
            case 36: // gd
                return "1, 11";
            // set22
            case 103: // sl
                return "1, 101, 201, 301, 401, 501, 601, 701, 1001, \u2026";
            // set27
            case 6: // be
                return "1, 21, 31, 41, 51, 61, 71, 81, 101, 1001, \u2026";
            // set28
            case 71: // lt
                return "1, 21, 31, 41, 51, 61, 71, 81, 101, 1001, \u2026";
            // set30
            case 98: // ru
            case 123: // uk
                return "1, 21, 31, 41, 51, 61, 71, 81, 101, 1001, \u2026";
            // set31
            case 12: // br
                return "1, 21, 31, 41, 51, 61, 81, 101, 1001, \u2026";
            // set33
            case 39: // gv
                return "1, 11, 21, 31, 41, 51, 61, 71, 101, 1001, \u2026";
            // set4
            case 101: // si
                return "0, 1";
            // set5
            case 1: // ak
            case 8: // bh
            case 69: // ln
            case 73: // mg
            case 92: // pa
            case 115: // ti
            case 129: // wa
                return "0, 1";
            // set7
            case 95: // pt
                return "0, 1";
            case -1:
            default:
                return null;
        }
    }

    @Nullable
    private static String getExampleForQuantityTwo(@NonNull String language) {
        int index = getLanguageIndex(language);
        switch (index) {
            // set21
            case 36: // gd
                return "2, 12";
            // set22
            case 103: // sl
                return "2, 102, 202, 302, 402, 502, 602, 702, 1002, \u2026";
            // set31
            case 12: // br
                return "2, 22, 32, 42, 52, 62, 82, 102, 1002, \u2026";
            // set33
            case 39: // gv
                return "2, 12, 22, 32, 42, 52, 62, 72, 102, 1002, \u2026";
            case -1:
            default:
                return null;
        }
    }
}
