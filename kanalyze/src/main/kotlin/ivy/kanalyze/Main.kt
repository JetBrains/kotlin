/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package ivy.kanalyze

import com.intellij.psi.PsiFileFactory
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.idea.KotlinLanguage

fun main() {
    println("Hello")
    val environment = KotlinCoreEnvironment.createForProduction(
        { },
        CompilerConfiguration(),
        EnvironmentConfigFiles.JVM_CONFIG_FILES,
    )
    val code = """
            package ivy
            
            fun main() {
              println("Hello, world!")
            }
        """.trimIndent()
    val file = PsiFileFactory.getInstance(environment.project)
        .createFileFromText("MyFile.kt", KotlinLanguage.INSTANCE, code)

    file.children.forEach {
        println(it.text)
    }
}