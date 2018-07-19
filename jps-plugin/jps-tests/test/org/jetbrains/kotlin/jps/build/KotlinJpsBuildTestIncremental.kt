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
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.jps.builders.JpsBuildTestCase
import org.jetbrains.kotlin.compilerRunner.JpsKotlinCompilerRunner
import org.jetbrains.kotlin.config.IncrementalCompilation
import org.jetbrains.kotlin.config.LanguageVersion
import kotlin.reflect.KMutableProperty1
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_CUSTOM_RUN_FILES_PATH_FOR_TESTS
import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_ENABLED_PROPERTY
import org.jetbrains.kotlin.daemon.common.isDaemonEnabled
import org.jetbrains.kotlin.jps.model.kotlinCommonCompilerArguments
import org.jetbrains.kotlin.jps.model.kotlinCompilerArguments
import java.io.File

class KotlinJpsBuildTestIncremental : KotlinJpsBuildTest() {
    var isICEnabledBackup: Boolean = false

    override fun setUp() {
        super.setUp()
        isICEnabledBackup = IncrementalCompilation.isEnabledForJvm()
        IncrementalCompilation.setIsEnabledForJvm(true)
    }

    override fun tearDown() {
        IncrementalCompilation.setIsEnabledForJvm(isICEnabledBackup)
        super.tearDown()
    }

    fun testJpsDaemonIC() {
        fun testImpl() {
            assertTrue("Daemon was not enabled!", isDaemonEnabled())

            doTest()
            val module = myProject.modules.get(0)
            val mainKtClassFile = findFileInOutputDir(module, "MainKt.class")
            assertTrue("$mainKtClassFile does not exist!", mainKtClassFile.exists())

            val fooKt = File(workDir, "src/Foo.kt")
            JpsBuildTestCase.change(fooKt.path, null)
            buildAllModules().assertSuccessful()
            assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, "src/Foo.kt")

            JpsBuildTestCase.change(fooKt.path, "class Foo(val x: Int = 0)")
            buildAllModules().assertSuccessful()
            assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, "src/main.kt", "src/Foo.kt")
        }

        val daemonHome = FileUtil.createTempDirectory("daemon-home", "testJpsDaemonIC")
        try {
            withSystemProperty(COMPILE_DAEMON_CUSTOM_RUN_FILES_PATH_FOR_TESTS, daemonHome.absolutePath) {
                withSystemProperty(COMPILE_DAEMON_ENABLED_PROPERTY, "true") {
                    withSystemProperty(JpsKotlinCompilerRunner.FAIL_ON_FALLBACK_PROPERTY, "true") {
                        testImpl()
                    }
                }
            }
        }
        finally {
            daemonHome.deleteRecursively()
        }
    }

    fun testManyFiles() {
        doTest()

        val module = myProject.modules.get(0)
        assertFilesExistInOutput(module, "foo/MainKt.class", "boo/BooKt.class", "foo/Bar.class")

        checkWhen(touch("src/main.kt"), null, packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"), arrayOf("src/Bar.kt"), arrayOf(module("kotlinProject"), klass("kotlinProject", "foo.Bar")))

        checkWhen(del("src/main.kt"),
                  pathsToCompile = null,
                  pathsToDelete = packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        assertFilesExistInOutput(module, "boo/BooKt.class", "foo/Bar.class")
        assertFilesNotExistInOutput(module, "foo/MainKt.class")

        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"), null, arrayOf(module("kotlinProject"), klass("kotlinProject", "foo.Bar")))
    }

    fun testManyFilesForPackage() {
        doTest()

        val module = myProject.modules.get(0)
        assertFilesExistInOutput(module, "foo/MainKt.class", "boo/BooKt.class", "foo/Bar.class")

        checkWhen(touch("src/main.kt"), null, packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"),
                  arrayOf("src/Bar.kt"),
                  arrayOf(klass("kotlinProject", "foo.Bar"),
                          packagePartClass("kotlinProject", "src/Bar.kt", "foo.MainKt"),
                          module("kotlinProject")))

        checkWhen(del("src/main.kt"),
                  pathsToCompile = null,
                  pathsToDelete = packageClasses("kotlinProject", "src/main.kt", "foo.MainKt"))
        assertFilesExistInOutput(module, "boo/BooKt.class", "foo/Bar.class")

        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooKt"))
        checkWhen(touch("src/Bar.kt"), null,
                  arrayOf(klass("kotlinProject", "foo.Bar"),
                          packagePartClass("kotlinProject", "src/Bar.kt", "foo.MainKt"),
                          module("kotlinProject")))
    }

    @WorkingDir("LanguageOrApiVersionChanged")
    fun testLanguageVersionChanged() {
        languageOrApiVersionChanged(CommonCompilerArguments::languageVersion)
    }

    @WorkingDir("LanguageOrApiVersionChanged")
    fun testApiVersionChanged() {
        languageOrApiVersionChanged(CommonCompilerArguments::apiVersion)
    }

    private fun languageOrApiVersionChanged(versionProperty: KMutableProperty1<CommonCompilerArguments, String?>) {
        initProject(LibraryDependency.JVM_MOCK_RUNTIME)

        assertEquals(1, myProject.modules.size)
        val module = myProject.modules.first()
        val args = module.kotlinCompilerArguments

        fun setVersion(newVersion: String) {
            versionProperty.set(args, newVersion)
            myProject.kotlinCommonCompilerArguments = args
        }

        assertNull(args.apiVersion)
        buildAllModules().assertSuccessful()

        setVersion(LanguageVersion.LATEST_STABLE.versionString)
        buildAllModules().assertSuccessful()
        assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME)

        setVersion(LanguageVersion.KOTLIN_1_0.versionString)
        buildAllModules().assertSuccessful()
        assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, "src/Bar.kt", "src/Foo.kt")
    }
}