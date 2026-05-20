/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFramework;

import net.bytebuddy.asm.Advice;

import java.io.File;

public class FileExistsAdvice {

    @Advice.OnMethodExit
    public static void advice(@Advice.This File file, @Advice.Return boolean returnValue) {
        // We skip non-existent files as we don't consider them inputs.
        // If a file is actually needed, the test would have failed. Failed test results are not cached.
        if (returnValue) {
            UndeclaredInputsGuard.checkFile(file.getPath());
        }
    }
}
