/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.codegen.GeneratedClassLoader
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import java.io.File

abstract class AbstractCodegenTest : AbstractCompilerTest() {
    override fun setUp() {
        super.setUp()
        val classPath = createClasspath() + additionalPaths

        val configuration = newConfiguration()
        configuration.addJvmClasspathRoots(classPath)
        updateConfiguration(configuration)

        myEnvironment = KotlinCoreEnvironment.createForTests(
            myTestRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).also { setupEnvironment(it) }
    }

    open fun updateConfiguration(configuration: CompilerConfiguration) {
        configuration.put(JVMConfigurationKeys.IR, true)
        configuration.put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)
    }

    protected open fun helperFiles(): List<KtFile> = emptyList()

    protected fun dumpClasses(loader: GeneratedClassLoader) {
        for (
            file in loader.allGeneratedFiles.filter {
                it.relativePath.endsWith(".class")
            }
        ) {
            println("------\nFILE: ${file.relativePath}\n------")
            println(file.asText())
        }
    }

    protected fun validateBytecode(
        @Language("kotlin")
        src: String,
        dumpClasses: Boolean = false,
        validate: (String) -> Unit
    ): Unit = ensureSetup {
        val className = "Test_REPLACEME_${uniqueNumber++}"
        val fileName = "$className.kt"

        val loader = classLoader(
            """
           @file:OptIn(
             InternalComposeApi::class,
           )
           package test

           import androidx.compose.runtime.*

           $src

            fun used(x: Any?) {}
        """,
            fileName, dumpClasses
        )

        val apiString = loader
            .allGeneratedFiles
            .filter { it.relativePath.endsWith(".class") }
            .map {
                it.asText().replace('$', '%').replace(className, "Test")
            }.joinToString("\n")

        validate(apiString)
    }

    protected fun classLoader(
        @Language("kotlin")
        source: String,
        fileName: String,
        dumpClasses: Boolean = false
    ): GeneratedClassLoader {
        val files = mutableListOf<KtFile>()
        files.addAll(helperFiles())
        files.add(sourceFile(fileName, source))
        myFiles = CodegenTestFiles.create(files)
        val loader = createClassLoader()
        if (dumpClasses) dumpClasses(loader)
        return loader
    }

    protected fun classLoader(
        sources: Map<String, String>,
        dumpClasses: Boolean = false
    ): GeneratedClassLoader {
        val files = mutableListOf<KtFile>()
        files.addAll(helperFiles())
        for ((fileName, source) in sources) {
            files.add(sourceFile(fileName, source))
        }
        myFiles = CodegenTestFiles.create(files)
        val loader = createClassLoader()
        if (dumpClasses) dumpClasses(loader)
        return loader
    }

    protected fun testFile(source: String, dumpClasses: Boolean = false) {
        val files = mutableListOf<KtFile>()
        files.addAll(helperFiles())
        files.add(sourceFile("Test.kt", source))
        myFiles = CodegenTestFiles.create(files)
        val loader = createClassLoader()
        if (dumpClasses) dumpClasses(loader)
        val loadedClass = loader.loadClass("Test")
        val instance = loadedClass.newInstance()
        val instanceClass = instance::class.java
        val testMethod = instanceClass.getMethod("test")
        testMethod.invoke(instance)
    }

    protected fun testCompile(source: String, dumpClasses: Boolean = false) {
        val files = mutableListOf<KtFile>()
        files.addAll(helperFiles())
        files.add(sourceFile("Test.kt", source))
        myFiles = CodegenTestFiles.create(files)
        val loader = createClassLoader()
        if (dumpClasses) dumpClasses(loader)
    }

    protected val COMPOSE_VIEW_STUBS_IMPORTS = """
        import android.view.View
        import android.widget.TextView
        import android.widget.Button
        import android.view.Gravity
        import android.widget.LinearLayout
        import androidx.compose.runtime.Composable
    """.trimIndent()

    protected val COMPOSE_VIEW_STUBS = """
        @Composable
        fun TextView(
            id: Int = 0,
            gravity: Int = Gravity.TOP or Gravity.START,
            text: String = "",
            onClick: (() -> Unit)? = null,
            onClickListener: View.OnClickListener? = null
        ) {
            emitView(::TextView) {
                if (id != 0) it.id = id
                it.text = text
                it.gravity = gravity
                if (onClickListener != null) it.setOnClickListener(onClickListener)
                if (onClick != null) it.setOnClickListener(View.OnClickListener { onClick() })
            }
        }

        @Composable
        fun Button(
            id: Int = 0,
            text: String = "",
            onClick: (() -> Unit)? = null,
            onClickListener: View.OnClickListener? = null
        ) {
            emitView(::Button) {
                if (id != 0) it.id = id
                it.text = text
                if (onClickListener != null) it.setOnClickListener(onClickListener)
                if (onClick != null) it.setOnClickListener(View.OnClickListener { onClick() })
            }
        }

        @Composable
        fun LinearLayout(
            id: Int = 0,
            orientation: Int = LinearLayout.VERTICAL,
            onClickListener: View.OnClickListener? = null,
            content: @Composable () -> Unit
        ) {
            emitView(
                ::LinearLayout,
                {
                    if (id != 0) it.id = id
                    if (onClickListener != null) it.setOnClickListener(onClickListener)
                    it.orientation = orientation
                },
                content
            )
        }
    """.trimIndent()

    protected fun testCompileWithViewStubs(source: String, dumpClasses: Boolean = false) =
        testCompile(
            """
            $COMPOSE_VIEW_STUBS_IMPORTS

            $source

            $COMPOSE_VIEW_STUBS
        """,
            dumpClasses
        )

    protected fun sourceFile(name: String, source: String): KtFile {
        val result =
            createFile(name, source, myEnvironment!!.project)
        val ranges = AnalyzingUtils.getSyntaxErrorRanges(result)
        assert(ranges.isEmpty()) { "Syntax errors found in $name: $ranges" }
        return result
    }

    protected fun loadClass(className: String, source: String): Class<*> {
        myFiles = CodegenTestFiles.create(
            "file.kt",
            source,
            myEnvironment!!.project
        )
        val loader = createClassLoader()
        return loader.loadClass(className)
    }

    protected open val additionalPaths = emptyList<File>()
}

fun createFile(name: String, text: String, project: Project): KtFile {
    var shortName = name.substring(name.lastIndexOf('/') + 1)
    shortName = shortName.substring(shortName.lastIndexOf('\\') + 1)
    val virtualFile = object : LightVirtualFile(
        shortName,
        KotlinLanguage.INSTANCE,
        StringUtilRt.convertLineSeparators(text)
    ) {
        override fun getPath(): String = "/$name"
    }

    virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET)
    val factory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl

    return factory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false) as KtFile
}

fun tmpDir(name: String): File {
    return FileUtil.createTempDirectory(name, "", false).canonicalFile
}
