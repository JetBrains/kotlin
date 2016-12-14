/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.metadata.K2MetadataCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.ChangedFiles
import java.io.File

internal open class KotlinCompileCommon : AbstractKotlinCompile<K2MetadataCompilerArguments>() {
    override val compiler = K2MetadataCompiler()

    override fun populateCompilerArguments(): K2MetadataCompilerArguments =
            K2MetadataCompilerArguments()

    override fun getSourceRoots(): SourceRoots =
            SourceRoots.KotlinOnly.create(getSource())

    override fun callCompiler(args: K2MetadataCompilerArguments, sourceRoots: SourceRoots, changedFiles: ChangedFiles) {
        val classpathList = classpath.files.toMutableList()
        val friendTask = friendTaskName?.let { project.tasks.findByName(it) } as? AbstractCompile
        friendTask?.let { classpathList.add(it.destinationDir) }

        with(args) {
            classpath = classpathList.joinToString(File.pathSeparator)
            destination = destinationDir.canonicalPath
            freeArgs = sourceRoots.kotlinSourceFiles.map { it.canonicalPath }
        }

        val messageCollector = GradleMessageCollector(project.logger)
        val exitCode = compiler.exec(messageCollector, Services.EMPTY, args)
        throwGradleExceptionIfError(exitCode)
    }
}