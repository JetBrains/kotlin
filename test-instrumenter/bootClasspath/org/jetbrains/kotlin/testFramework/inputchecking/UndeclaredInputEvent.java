/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFramework.inputchecking;

import jdk.jfr.Event;
import jdk.jfr.Name;

@Name("jetbrains.UndeclaredInput")
public class UndeclaredInputEvent extends Event {
    private final String path;

    public static void emit(String path) {
        new UndeclaredInputEvent(path).commit();
    }

    public UndeclaredInputEvent(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
