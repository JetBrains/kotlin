/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.generateAllTests;

import org.jetbrains.kotlin.generators.tests.*;
import org.jetbrains.kotlin.generators.InconsistencyChecker;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        GenerateCompilerTestsKt.main(args);
        GenerateTestsKt.main(args);
        GenerateJsTestsKt.main(args);
        GenerateJava8TestsKt.main(args);
        GenerateRuntimeDescriptorTestsKt.main(args);

        boolean dryRun = InconsistencyChecker.Companion.hasDryRunArg(args);
        List<String> affectedFiles = InconsistencyChecker.Companion.inconsistencyChecker(dryRun).getAffectedFiles();
        int size = affectedFiles.size();
        if (size > 0) {
            throw new IllegalStateException("There " + (size == 1 ? "is a test" : "are " + size + " tests") + " to be regenerated:\n"
                                            + String.join("\n", affectedFiles));
        }
    }
}
