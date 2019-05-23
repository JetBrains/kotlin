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

package androidx.compose.plugins.kotlin

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.testFramework.LightVirtualFile
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
        for (file in loader.allGeneratedFiles.filter {
            it.relativePath.endsWith(".class")
        }) {
            println("------\nFILE: ${file.relativePath}\n------")
            println(file.asText())
        }
    }

    protected fun classLoader(
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
