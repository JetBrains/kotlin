/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFramework.inputchecking;

import net.bytebuddy.asm.Advice;

import java.io.File;

public class InputCheckingFileExistsAdvice {

    @Advice.OnMethodExit
    public static void advice(@Advice.This File file, @Advice.Return boolean fileExistsReturnValue) {
        // We skip non-existent files as we don't consider them inputs; see detailed explanation in test-inputs-check-v2/README.md
        if (fileExistsReturnValue) {
            TestInputsChecker.getInstance().checkPath(file.getPath());
        }
    }
}
