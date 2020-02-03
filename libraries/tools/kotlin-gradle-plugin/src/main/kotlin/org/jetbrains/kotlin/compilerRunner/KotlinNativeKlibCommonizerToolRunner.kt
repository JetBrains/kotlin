/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

// TODO: implement this runner
internal class KotlinNativeKlibCommonizerToolRunner(project: Project) : KotlinToolRunner(project) {
    override val displayName get() = "Kotlin/Native KLIB commonizer"

    override val mainClass: String get() = TODO("not implemented")
    override val classpath: FileCollection get() = TODO("not implemented")

    override fun getIsolatedClassLoader(): ClassLoader {
        TODO("not implemented")
    }

    override val mustRunViaExec get() = false
}
