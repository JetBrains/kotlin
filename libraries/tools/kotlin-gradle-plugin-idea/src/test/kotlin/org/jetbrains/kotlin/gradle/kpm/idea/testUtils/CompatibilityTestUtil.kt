/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea.testUtils

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmProject
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationLogger
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.TestIdeaKpmClassLoaderProjectSerializer
import java.io.File
import java.net.URLClassLoader
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun classLoaderForBackwardsCompatibleClasses(): ClassLoader {
    val uris = classpathForBackwardsCompatibleClasses().map { file -> file.toURI().toURL() }.toTypedArray()
    return URLClassLoader.newInstance(uris, null)
}

fun classpathForBackwardsCompatibleClasses(): List<File> {
    val compatibilityTestClasspath = System.getProperty("compatibilityTestClasspath")
        ?: error("Missing compatibilityTestClasspath system property")

    return compatibilityTestClasspath.split(";").map { path -> File(path) }
        .onEach { file -> if (!file.exists()) println("[WARNING] Missing $file") }
        .flatMap { file -> if (file.isDirectory) file.listFiles().orEmpty().toList() else listOf(file) }
}

fun deserializeIdeaKpmProjectWithBackwardsCompatibleClasses(project: IdeaKpmProject): Any {
    return deserializeIdeaKpmProjectWithBackwardsCompatibleClasses(
        TestIdeaKpmClassLoaderProjectSerializer().serialize(project)
    )
}

fun deserializeIdeaKpmProjectWithBackwardsCompatibleClasses(project: ByteArray): Any {
    val classLoader = classLoaderForBackwardsCompatibleClasses()
    val serializer = TestIdeaKpmClassLoaderProjectSerializer(classLoader)

    val deserialized = assertNotNull(
        serializer.deserialize(project),
        "Failed to deserialize project: ${serializer.reports}"
    )

    assertEquals(
        0, serializer.reports.count { it.severity > IdeaKpmSerializationLogger.Severity.WARNING },
        "Expected no severe deserialization reports. Found ${serializer.reports}"
    )

    assertSame(
        classLoader, deserialized::class.java.classLoader,
        "Expected model do be deserialized in with old classes"
    )

    return deserialized
}
