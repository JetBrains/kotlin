/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.languageSetting

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.config.JvmTarget

val jvmLanguageSettings: Map<String, SimpleLanguageSetting<K2JVMCompilerArguments, *>> = listOf(
    SimpleLanguageSetting<K2JVMCompilerArguments, String>(
        key = "jvm-target",
        description = "Target version of the generated JVM bytecode (1.6 (DEPRECATED), 1.8, 9, 10, 11, 12, 13, 14, 15 or 16), default is 1.8",
        // TODO: Or constant? It gives ambiguity to apply jvmTarget X on a Fragment A,
        //  and at the same time apply jvmTarget Y on a Variant that refines A.
        consistencyRule = ConsistencyRelation.MonotonicDesc(
            comparator = compareBy { JvmTarget.fromString(it) ?: error("Unknown JVM Target: $it") }
        ),
        contributor = { jvmTarget = it },
        serializer = StringSerializer
    ),
    SimpleLanguageSetting<K2JVMCompilerArguments, String>(
        key = "string-concat",
        description = """Select code generation scheme for string concatenation.
            -Xstring-concat=indy-with-constants   Concatenate strings using `invokedynamic` `makeConcatWithConstants`. Requires `-jvm-target 9` or greater.
            -Xstring-concat=indy                Concatenate strings using `invokedynamic` `makeConcat`. Requires `-jvm-target 9` or greater.
            -Xstring-concat=inline              Concatenate strings using `StringBuilder`
            default: `indy-with-constants` for JVM target 9 or greater, `inline` otherwise""".trimIndent(),
        consistencyRule = ConsistencyRelation.DontCare,
        contributor = { stringConcat = it },
        serializer = StringSerializer
    ),
).associateBy { it.key }