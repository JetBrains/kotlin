/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

import kotlin.SinceKotlin;

@SinceKotlin(version = "1.2")
public class MagicApiIntrinsics {

    /**
     * This method is used as a reified marker for plugin-defined compiler intrinsics.
     * See JvmIrIntrinsicExtension.kt in the compiler:backend:jvm:codegen
     *
     * @param data Arbitrary data to pass to plugin. Must be string constant (loaded by LDC instruction).
     */
    public static void voidMagicApiCall(Object data) {
    }

    public static <T> T anyMagicApiCall(int id) {
        return null;
    }

    public static void voidMagicApiCall(int id) {
    }

    public static int intMagicApiCall(int id) {
        return 0;
    }

    public static <T> T anyMagicApiCall(Object data) {
        return null;
    }

    public static int intMagicApiCall(Object data) {
        return 0;
    }

    public static int intMagicApiCall(int id, long longData, Object anyData) {
        return 0;
    }

    public static int intMagicApiCall(int id, long longData1, long longData2, Object anyData) {
        return 0;
    }

    public static int intMagicApiCall(int id, Object anyData1, Object anyData2) {
        return 0;
    }

    public static int intMagicApiCall(int id, Object anyData1, Object anyData2, Object anyData3, Object anyData4) {
        return 0;
    }

    public static <T> T anyMagicApiCall(int id, long longData, Object anyData) {
        return null;
    }

    public static <T> T anyMagicApiCall(int id, long longData1, long longData2, Object anyData) {
        return null;
    }

    public static <T> T anyMagicApiCall(int id, Object anyData1, Object anyData2) {
        return null;
    }

    public static <T> T anyMagicApiCall(int id, Object anyData1, Object anyData2, Object anyData3, Object anyData4) {
        return null;
    }
}
