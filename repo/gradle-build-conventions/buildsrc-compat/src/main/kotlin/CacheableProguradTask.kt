/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.JavaVersion
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.kotlin.dsl.property
import java.io.File

@CacheableTask
abstract class CacheableProguardTask : proguard.gradle.ProGuardTask() {
    @get:Internal
    abstract val javaLauncher: Property<JavaLauncher>

    @get:Internal
    val jdkHomePath: Provider<File> = javaLauncher.map { it.metadata.installationPath.asFile }

    @get:Optional
    @get:Input
    internal val jdkMajorVersion: Provider<JavaVersion> = javaLauncher.map {
        JavaVersion.toVersion(it.metadata.languageVersion.toString())
    }

    @CompileClasspath
    override fun getLibraryJarFileCollection(): FileCollection {
        return super.getLibraryJarFileCollection().filter { libraryFile ->
            jdkHomePath.orNull?.let { !libraryFile.absoluteFile.startsWith(it.absoluteFile) } ?: true
        }
    }
}
