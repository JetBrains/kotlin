/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.resolve.android.test

import com.intellij.testFramework.UsefulTestCase
import java.io.File
import java.io.IOException
import java.util.Scanner
import java.io.FileWriter
import org.junit.Assert
import kotlin.test.fail
import org.jetbrains.jet.lang.resolve.android.CliAndroidUIXmlProcessor
import org.jetbrains.jet.lang.resolve.android.AndroidUIXmlProcessor
import kotlin.test.assertEquals
import org.jetbrains.kotlin.test.JetTestUtils
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles

public abstract class AbstractAndroidXml2KConversionTest : UsefulTestCase() {

    public fun doTest(path: String) {
        val jetCoreEnvironment = getEnvironment(path)
        val parser = CliAndroidUIXmlProcessor(jetCoreEnvironment.getProject(), path + "AndroidManifest.xml", path + "/layout")

        val actual = parser.parse()

        val layoutFiles = File(path).listFiles {
            it.isFile() && it.name.startsWith("layout") && it.name.endsWith(".kt")
        }?.sortBy { it.name } ?: listOf()

        assertEquals(layoutFiles.size(), actual.size())

        for ((index, file) in layoutFiles.withIndex()) {
            JetTestUtils.assertEqualsToFile(file, actual[index])
        }
    }

    public fun doNoManifestTest(path: String) {
        try {
            doTest(path)
            fail("NoAndroidManifestFound not thrown")
        }
        catch (e: AndroidUIXmlProcessor.NoAndroidManifestFound) {
        }
    }

    private fun getEnvironment(testPath: String): JetCoreEnvironment {
        val configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.MOCK_JDK)
        return JetCoreEnvironment.createForTests(getTestRootDisposable()!!, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }
}
