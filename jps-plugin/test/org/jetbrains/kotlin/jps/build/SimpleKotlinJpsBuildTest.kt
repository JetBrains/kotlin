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

import com.intellij.util.PathUtil
import org.jetbrains.jps.model.java.JpsJavaExtensionService
import org.jetbrains.kotlin.rmi.COMPILE_DAEMON_CLIENT_ALIVE_PATH_PROPERTY
import org.jetbrains.kotlin.rmi.COMPILE_DAEMON_ENABLED_PROPERTY
import org.jetbrains.kotlin.rmi.COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY
import org.jetbrains.kotlin.test.JetTestUtils
import java.io.File

public class SimpleKotlinJpsBuildTest : AbstractKotlinJpsBuildTestCase() {
    override fun setUp() {
        super.setUp()
        workDir = JetTestUtils.tmpDirForTest(this)
    }

    public fun testLoadingKotlinFromDifferentModules() {
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
        val a = addModule("m1", PathUtil.getParentPath(aFile))

        val bFile = createFile("m2/m2.kt",
                               """
                                    import m1.J;
                                    import m1.K;

                                    interface M2: J {
                                        override fun bar(): K
                                    }
                               """)
        val b = addModule("b", PathUtil.getParentPath(bFile))
        JpsJavaExtensionService.getInstance().getOrCreateDependencyExtension(
                b.dependenciesList.addModuleDependency(a)
        ).isExported = false

        addKotlinRuntimeDependency()
        rebuildAll()
    }

    public fun testDaemon() {
        System.setProperty(COMPILE_DAEMON_ENABLED_PROPERTY,"")
        System.setProperty(COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY, "")
        // spaces in the name to test proper file name handling
        val flagFile = File.createTempFile("kotlin-jps - tests-", "-is-running");
        try {
            System.setProperty(COMPILE_DAEMON_CLIENT_ALIVE_PATH_PROPERTY, flagFile.absolutePath)
            testLoadingKotlinFromDifferentModules()
        }
        finally {
            flagFile.delete()
            System.clearProperty(COMPILE_DAEMON_CLIENT_ALIVE_PATH_PROPERTY)
            System.clearProperty(COMPILE_DAEMON_VERBOSE_REPORT_PROPERTY)
            System.clearProperty(COMPILE_DAEMON_ENABLED_PROPERTY)
        }
    }
}
