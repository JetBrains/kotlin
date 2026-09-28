/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jsr223.bta.test

import org.jetbrains.kotlin.mainKts.MainKtsScript
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.script.experimental.dependencies.DependsOn
import kotlin.script.templates.standard.ScriptTemplateWithBindings

internal fun classpathFromSystemProperty(propertyName: String): List<Path> =
    System.getProperty(propertyName)
        ?.split(File.pathSeparator)
        ?.filter { it.isNotBlank() }
        ?.map { Paths.get(it) }
        ?: error("system property '$propertyName' is not set -- run this test via its Gradle test task")

internal val stdlibPath: Path
    get() = Paths.get(KotlinVersion::class.java.protectionDomain.codeSource.location.toURI())

internal val scriptRuntimePath: Path
    get() = Paths.get(
        ScriptTemplateWithBindings::class.java.protectionDomain.codeSource.location.toURI()
    )

internal val mainKtsPaths: List<Path>
    get() = listOf(
        Paths.get(MainKtsScript::class.java.protectionDomain.codeSource.location.toURI()),
        Paths.get(DependsOn::class.java.protectionDomain.codeSource.location.toURI()),
    )
