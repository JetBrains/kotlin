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

package org.jetbrains.kotlin.jps.build

import com.intellij.util.PathUtilRt
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.jetbrains.kotlin.daemon.common.OSKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

class SimpleKotlinJpsBuildTest : AbstractKotlinJpsBuildTestCase() {
    override fun setUp() {
        super.setUp()
        workDir = KotlinTestUtils.tmpDirForTest(this)
    }

    fun testLoadingKotlinFromDifferentModules() {
        val aFile = createFile("m1/K.kt",
                               """
                                   package m1;

                                   interface K {
                                   }
                               """)
        createFile("m1/J.java",
                               """
                                   package m1;

                                   public interface J {
                                       K bar();
                                   }
                               """)
        val a = addModule("m1", PathUtilRt.getParentPath(aFile))

        val bFile = createFile("m2/m2.kt",
                               """
                                    import m1.J;
                                    import m1.K;

                                    interface M2: J {
                                        override fun bar(): K
                                    }
                               """)
        val b = addModule("b", PathUtilRt.getParentPath(bFile))
        JpsJavaExtensionService.getInstance().getOrCreateDependencyExtension(
                b.dependenciesList.addModuleDependency(a)
        ).isExported = false

        addKotlinStdlibDependency()
        rebuildAllModules()
    }

    // TODO: add JS tests
    fun testDaemon() {
        withDaemon {
            withSystemProperty(CompilerSystemProperties.COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY.property, "true") {
                withSystemProperty(JpsKotlinCompilerRunner.FAIL_ON_FALLBACK_PROPERTY, "true") {
                    testLoadingKotlinFromDifferentModules()
                }
            }
        }
    }
}

// copied from CompilerDaemonTest.kt
// TODO: find shared place for this function
// java.util.Logger used in the daemon silently forgets to log into a file specified in the config on Windows,
// if file path is given in windows form (using backslash as a separator); the reason is unknown
// this function makes a path with forward slashed, that works on windows too
internal val File.loggerCompatiblePath: String
    get() =
    if (OSKind.current == OSKind.Windows) absolutePath.replace('\\', '/')
    else absolutePath
