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

package org.jetbrains.kotlin.jps.build

import com.intellij.openapi.util.io.FileUtil
import junit.framework.TestCase.assertTrue
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.jetbrains.kotlin.jps.build.JpsBuildTestCase.change
import java.io.File

inline fun withSystemProperty(property: String, newValue: String?, fn: ()->Unit) {
    val backup = System.getProperty(property)
    setOrClearSysProperty(property, newValue)

    try {
        fn()
    }
    finally {
        setOrClearSysProperty(property, backup)
    }
}


@Suppress("NOTHING_TO_INLINE")
inline fun setOrClearSysProperty(property: String, newValue: String?) {
    if (newValue != null) {
        System.setProperty(property, newValue)
    }
    else {
        System.clearProperty(property)
    }
}

fun withDaemon(fn: () -> Unit) {
    val daemonHome = FileUtil.createTempDirectory("daemon-home", "testJpsDaemonIC")

    withSystemProperty(CompilerSystemProperties.COMPILE_DAEMON_CUSTOM_RUN_FILES_PATH_FOR_TESTS.property, daemonHome.absolutePath) {
        withSystemProperty(CompilerSystemProperties.COMPILE_DAEMON_ENABLED_PROPERTY.property, "true") {
            try {
                fn()
            } finally {
                JpsKotlinCompilerRunner.shutdownDaemon()

                // Try to force directory deletion to prevent test failure later in tearDown().
                // Working Daemon can prevent folder deletion on Windows, because Daemon shutdown
                // is asynchronous.
                var attempts = 0
                daemonHome.deleteRecursively()
                while (daemonHome.exists() && attempts < 100) {
                    daemonHome.deleteRecursively()
                    attempts++
                    Thread.sleep(50)
                }

                if (daemonHome.exists()) {
                    error("Couldn't delete Daemon home directory")
                }
            }
        }
    }
}

interface Action {
    fun apply()
}

class TouchAction(val path: String): Action {
    override fun apply() {
        assertTrue(File(path).exists())
        change(path)
    }
}

class DeleteAction(val path: String): Action {
    override fun apply() {
        val file = File(path)
        assertTrue(file.exists())
        assertTrue("Can not delete file \"" + file.absolutePath + "\"", file.delete())
    }
}

class ChangeAction(val path: String, val newContent: String): Action {
    override fun apply() {
        val file = File(path)
        assertTrue(file.exists())
        change(path, newContent)
    }
}