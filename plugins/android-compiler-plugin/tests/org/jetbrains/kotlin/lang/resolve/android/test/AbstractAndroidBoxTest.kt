/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.lang.resolve.android.test

import org.jetbrains.kotlin.codegen.generated.AbstractBlackBoxCodegenTest
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File
import com.intellij.util.Processor
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.util.regex.Pattern
import com.intellij.openapi.util.io.FileUtil
import java.util.ArrayList
import java.util.Collections
import com.intellij.util.ArrayUtil
import org.jetbrains.kotlin.codegen.CodegenTestFiles

public abstract class AbstractAndroidBoxTest : AbstractBlackBoxCodegenTest() {

    private fun createAndroidAPIEnvironment(path: String) {
        return createEnvironmentForConfiguration(KotlinTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.ANDROID_API), path)
    }

    private fun createFakeAndroidEnvironment(path: String) {
        return createEnvironmentForConfiguration(KotlinTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK), path)
    }

    private fun createEnvironmentForConfiguration(configuration: CompilerConfiguration, path: String) {
        val layoutPaths = File(path).listFiles { it.name.startsWith("layout") && it.isDirectory }!!.map { "$path${it.name}/" }
        myEnvironment = createAndroidTestEnvironment(configuration, layoutPaths)
    }

    public fun doCompileAgainstAndroidSdkTest(path: String) {
        createAndroidAPIEnvironment(path)
        doMultiFileTest(path)
    }

    public fun doFakeInvocationTest(path: String) {
        if (needsInvocationTest(path)) {
            createFakeAndroidEnvironment(path)
            doMultiFileTest(path, getFakeFiles(path))
        }
    }

    private fun getFakeFiles(path: String): Collection<String> {
        return FileUtil.findFilesByMask(Pattern.compile("^Fake.*\\.kt$"), File(path.replace(getTestName(true), ""))) map { relativePath(it) }
    }

    private fun needsInvocationTest(path: String): Boolean {
        return !FileUtil.findFilesByMask(Pattern.compile("^0.kt$"), File(path)).isEmpty()
    }

    override fun codegenTestBasePath(): String {
        return "plugins/android-compiler-plugin/testData/codegen/"
    }

    private fun doMultiFileTest(path: String, additionalFiles: Collection<String>? = null) {
        val files = ArrayList<String>(2)
        FileUtil.processFilesRecursively(File(path), object : Processor<File> {
            override fun process(file: File?): Boolean {
                when (file!!.getName()) {
                    "1.kt" -> {
                        if (additionalFiles == null) files.add(relativePath(file))
                    }
                    "0.kt" -> {
                        if (additionalFiles != null) files.add(relativePath(file))
                    }
                    else -> {
                        if (file.name.endsWith(".kt")) files.add(relativePath(file))
                    }
                }
                return true
            }
        })
        Collections.sort(files);
        if (additionalFiles != null) {
            files.addAll(additionalFiles)
        }
        myFiles = CodegenTestFiles.create(
                myEnvironment!!.project,
                ArrayUtil.toStringArray(files),
                KotlinTestUtils.getHomeDirectory() + "/plugins/android-compiler-plugin/testData"
        )
        blackBox();
    }
}