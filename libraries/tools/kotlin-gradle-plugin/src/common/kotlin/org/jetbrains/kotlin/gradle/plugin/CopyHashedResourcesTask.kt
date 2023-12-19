/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

//import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.baseModuleName
import org.jetbrains.kotlin.gradle.plugin.mpp.moduleNameForCompilation
import org.jetbrains.kotlin.gradle.tasks.registerTask

internal inline fun <reified T: AbstractCopyTask> KotlinCompilation<*>.registerCopyHashedResourcesTask(
    name: String,
    destination: Any
): TaskProvider<T> {
    return project.registerTask<T>(name) { copy ->
        copyResourcesByHashing(copy)
        copy.into(destination)
        copy.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

internal inline fun <reified T: AbstractCopyTask> KotlinCompilation<*>.copyResourcesByHashing(copy: T) {
    val resourceFiles = allKotlinSourceSets.flatMap { it.resources.map { it } }.filter { it.isFile }
    resourceFiles.forEach {
        copy.from(it.canonicalPath) { copy ->
            copy.into(this.project.baseModuleName().get())
//            copy.rename { _ -> DigestUtils.sha256Hex(it.inputStream()) }
            copy.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}
