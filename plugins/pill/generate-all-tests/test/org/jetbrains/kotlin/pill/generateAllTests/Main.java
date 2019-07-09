/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pill.generateAllTests;

import org.jetbrains.kotlin.generators.tests.GenerateCompilerTestsKt;
import org.jetbrains.kotlin.generators.tests.GenerateJava8TestsKt;
import org.jetbrains.kotlin.generators.tests.GenerateJsTestsKt;
import org.jetbrains.kotlin.generators.tests.GenerateTestsKt;

public class Main {
    public static void main(String[] args) {
        GenerateCompilerTestsKt.main(args);
        GenerateTestsKt.main(args);
        GenerateJsTestsKt.main(args);
        GenerateJava8TestsKt.main(args);
    }
}
