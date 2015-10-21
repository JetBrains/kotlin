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

import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.android.synthetic.res.SyntheticFileGenerator
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import java.io.File
import kotlin.test.assertNotNull

public abstract class AbstractAndroidXml2KConversionTest : UsefulTestCase() {

    public fun doTest(path: String) {
        val testDirectory = File(path)

        val jetCoreEnvironment = getEnvironment()
        val layoutPaths = getResPaths(path)
        val supportV4 = testDirectory.name.startsWith("support")
        val parser = CliSyntheticFileGeneratorForConversionTest(
                jetCoreEnvironment.project, File(testDirectory.parentFile, "AndroidManifest.xml").path, layoutPaths, supportV4)

        val actual = parser.gen().toMapBy { it.name }

        val expectedLayoutFiles = testDirectory
                .listFiles { it.isFile && it.name.endsWith(".kt") }
                ?.toMapBy { it.name.substringBefore(".kt") } ?: mapOf()

        if (expectedLayoutFiles.size == 0 && actual.size > 0) {
            for (file in actual) {
                val syntheticFile = file.value
                File(testDirectory, syntheticFile.name + ".kt").writeText(syntheticFile.contents)
            }
            fail("No expected synthetic .kt files found, generated from actual")
        }

        assertEquals(expectedLayoutFiles.size, actual.size)

        for ((name, file) in expectedLayoutFiles) {
            val actualContents = actual[name]
            assertNotNull(actualContents, "File $name was not generated")
            KotlinTestUtils.assertEqualsToFile(file, actualContents!!.contents)
        }
    }

    public fun doNoManifestTest(path: String) {
        try {
            doTest(path)
            fail("NoAndroidManifestFound not thrown")
        }
        catch (e: SyntheticFileGenerator.NoAndroidManifestFound) {}
    }

    private fun getEnvironment(): KotlinCoreEnvironment {
        val configuration = KotlinTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.ANDROID_API)
        return KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
    }
}