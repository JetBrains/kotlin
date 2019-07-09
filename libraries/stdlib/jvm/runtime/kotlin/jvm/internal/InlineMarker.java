/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal;

public class InlineMarker {
    public static void mark(int i) {
    }

    public static void mark(String name) {
    }

    public static void beforeInlineCall() {
    }

    public static void afterInlineCall() {
    }

    public static void finallyStart(int finallyDepth) {
    }

    public static void finallyEnd(int finallyDepth) {
    }
}
