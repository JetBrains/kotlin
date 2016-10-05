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

package org.jetbrains.kotlin.incremental

import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.lang.java.JavaLanguage
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiClass
import org.jetbrains.kotlin.com.intellij.psi.PsiFile
import org.jetbrains.kotlin.com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.com.intellij.psi.PsiJavaFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.gradle.tasks.kotlinDebug
import org.jetbrains.kotlin.incremental.ChangesEither
import org.jetbrains.kotlin.incremental.LookupSymbol
import org.jetbrains.kotlin.incremental.snapshots.FileCollectionDiff
import java.io.File
import java.util.*

internal class ChangedJavaFilesProcessor {
    private val log = Logging.getLogger(this.javaClass.simpleName)
    private val allSymbols = HashSet<LookupSymbol>()
    private val javaLang = JavaLanguage.INSTANCE
    private val psiFileFactory: PsiFileFactory by lazy {
        val rootDisposable = Disposer.newDisposable()
        val configuration = CompilerConfiguration()
        val environment = KotlinCoreEnvironment.createForProduction(rootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val project = environment.project
        PsiFileFactory.getInstance(project)
    }

    val allChangedSymbols: Collection<LookupSymbol>
            get() = allSymbols

    fun process(filesDiff: FileCollectionDiff): ChangesEither {
        if (filesDiff.removed.any()) {
            log.kotlinDebug { "Some java files are removed: [${filesDiff.removed.joinToString()}]" }
            return ChangesEither.Unknown()
        }

        val symbols = HashSet<LookupSymbol>()
        for (javaFile in filesDiff.newOrModified) {
            assert(javaFile.extension.equals("java", ignoreCase = true))

            val psiFile = javaFile.psiFile()
            if (psiFile !is PsiJavaFile) {
                log.kotlinDebug { "Expected PsiJavaFile, got ${psiFile?.javaClass}" }
                return ChangesEither.Unknown()
            }

            psiFile.classes.forEach { it.addLookupSymbols(symbols) }
        }
        allSymbols.addAll(symbols)
        return ChangesEither.Known(lookupSymbols = symbols)
    }

    private fun PsiClass.addLookupSymbols(symbols: MutableSet<LookupSymbol>) {
        val fqn = qualifiedName.orEmpty()

        symbols.add(LookupSymbol(name.orEmpty(), if (fqn == name) "" else fqn.removeSuffix("." + name!!)))
        methods.forEach { symbols.add(LookupSymbol(it.name, fqn)) }
        fields.forEach { symbols.add(LookupSymbol(it.name.orEmpty(), fqn)) }
        innerClasses.forEach { it.addLookupSymbols(symbols) }
    }

    private fun File.psiFile(): PsiFile? =
            psiFileFactory.createFileFromText(nameWithoutExtension, javaLang, readText())
}