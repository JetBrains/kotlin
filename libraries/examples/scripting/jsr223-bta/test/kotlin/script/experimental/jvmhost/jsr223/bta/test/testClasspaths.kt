/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.bta.test

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

internal fun classpathFromSystemProperty(propertyName: String): List<Path> =
    System.getProperty(propertyName)
        ?.split(File.pathSeparator)
        ?.filter { it.isNotBlank() }
        ?.map { Paths.get(it) }
        ?: error("system property '$propertyName' is not set -- run this test via its Gradle test task")

/** The stdlib jar the snippet compile classpath needs, resolved from this test JVM's own classpath. */
internal val stdlibPath: Path
    get() = Paths.get(KotlinVersion::class.java.protectionDomain.codeSource.location.toURI())

/**
 * The `kotlin-script-runtime` jar, needed on the snippet compile classpath because a main-kts-based
 * synthetic bindings snippet declares `ScriptTemplateWithBindings` as an implicit receiver.
 */
internal val scriptRuntimePath: Path
    get() = Paths.get(
        kotlin.script.templates.standard.ScriptTemplateWithBindings::class.java.protectionDomain.codeSource.location.toURI()
    )
