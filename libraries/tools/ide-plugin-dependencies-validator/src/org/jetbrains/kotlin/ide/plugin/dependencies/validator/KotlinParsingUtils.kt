/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.plugin.dependencies.validator

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.impl.PsiFileFactoryImpl
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.streams.asSequence

fun forEachKtFileInDirectory(directory: Path, action: (KtFile, Path) -> Unit) {
    val project = createProjectForParsing()
    try {
        Files.walk(directory)
            .asSequence()
            .filter { it.extension == "kt" }
            .forEach { file ->
                val ktFile = file.parseAsKtFile(project)
                action(ktFile, file)
            }
    } finally {
        Disposer.dispose(project)
    }
}

private fun Path.parseAsKtFile(project: Project): KtFile {
    return PsiFileFactoryImpl(project).createFileFromText(name, KotlinLanguage.INSTANCE, readText()) as KtFile
}

private fun createProjectForParsing(): Project {
    return KotlinCoreEnvironment.createForProduction(
        Disposer.newDisposable("Disposable for project of ${ExperimentalOptInUsageInSourceChecker::class.simpleName}"),
        CompilerConfiguration(),
        EnvironmentConfigFiles.JVM_CONFIG_FILES
    ).project
}
