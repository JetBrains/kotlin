/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.kapt3.test

import org.jetbrains.kotlin.kapt3.util.isJava9OrLater
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.concurrent.TimeUnit

interface Java9TestLauncher {
    fun doTestWithJdk9(mainClass: Class<*>, arg: String) {
        // Already under Java 9
        if (isJava9OrLater) return

        val jdk9Home = KotlinTestUtils.getJdk9HomeIfPossible() ?: run {
            println("JDK9 not found, the test was skipped")
            return
        }

        val javaExe = File(jdk9Home, "bin/java.exe").takeIf { it.exists() } ?: File(jdk9Home, "bin/java")
        assert(javaExe.exists()) { "Can't find 'java' executable in $jdk9Home" }

        val currentJavaHome = System.getProperty("java.home")
        val classpath = collectClasspath(AbstractClassFileToSourceStubConverterTest::class.java.classLoader)
                .filter { !it.path.startsWith(currentJavaHome) }

        val process = ProcessBuilder(
                javaExe.absolutePath,
                "--illegal-access=warn",
                "-ea",
                "-classpath",
                classpath.joinToString(File.pathSeparator),
                mainClass.name,
                arg
        ).inheritIO().start()

        process.waitFor(3, TimeUnit.MINUTES)
        if (process.exitValue() != 0) {
            throw AssertionError("Java 9 test process exited with exit code ${process.exitValue()} \n")
        }
    }

    private fun collectClasspath(classLoader: ClassLoader?): List<URL> = when (classLoader) {
        is URLClassLoader -> classLoader.urLs.asList() + collectClasspath(classLoader.parent)
        is ClassLoader -> collectClasspath(classLoader.parent)
        else -> emptyList()
    }

}