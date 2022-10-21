/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.languageSetting

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments

val commonLanguageSettings: Map<String, SimpleLanguageSetting<*, *>> = listOf(
    SimpleLanguageSetting<CommonCompilerArguments, String>(
        key = "api-version",
        description = "Allow using declarations only from the specified version of bundled libraries",
        consistencyRule = languageVersionConsistencyRule,
        contributor = { apiVersion = it },
        serializer = StringSerializer
    ),
    SimpleLanguageSetting<CommonCompilerArguments, String>(
        key = "language-version",
        description = "Provide source compatibility with the specified version of Kotlin",
        consistencyRule = languageVersionConsistencyRule,
        contributor = { languageVersion = it },
        serializer = StringSerializer
    ),
    SimpleLanguageSetting<CommonCompilerArguments, Boolean>(
        key = "progressive",
        description = "Enable progressive compiler mode.\n" +
                "In this mode, deprecations and bug fixes for unstable code take effect immediately,\n" +
                "instead of going through a graceful migration cycle.\n" +
                "Code written in the progressive mode is backward compatible; however, code written in\n" +
                "non-progressive mode may cause compilation errors in the progressive mode.",
        consistencyRule = ConsistencyRelation.Constant, // OR Monotonic? "non progressive" refines "progressive" seemed allowed
        contributor = { progressiveMode = it },
        serializer = BoolSerializer
    ),
    SimpleLanguageSetting<CommonCompilerArguments, Boolean>(
        key = "no-inline",
        description = "Disable method inlining",
        consistencyRule = ConsistencyRelation.Constant, // TODO: unclear consequences of having it inconsistent,
        contributor = { noInline = it },
        serializer = BoolSerializer
    )
).associateBy { it.key }