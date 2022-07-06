/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3.base

import java.io.File

fun File.newSourcesFolder(): File = newFolder("sources")
fun File.newClassesFolder(): File = newFolder("classes")
fun File.newStubsFolder(): File = newFolder("stubs")
fun File.newCacheFolder(): File = newFolder("cache")
fun File.newGeneratedSourcesFolder(): File = newFolder("generatedSources")
fun File.newCompiledSourcesFolder(): File = newFolder("compiledSources")

fun File.newFolder(name: String): File {
    return resolve(name).also { it.mkdir() }
}

fun File.newFile(name: String): File {
    return resolve(name).also { it.createNewFile() }
}
