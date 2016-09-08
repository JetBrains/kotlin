/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.getFileByName
import org.junit.Test
import java.io.File
import java.io.FileFilter

class Kapt2IT: BaseGradleIT() {
    companion object {
        private const val GRADLE_VERSION = "2.10"
        private const val GRADLE_2_14_VERSION = "2.14.1"
        private const val ANDROID_GRADLE_PLUGIN_VERSION = "1.5.+"
    }

    private fun androidBuildOptions() =
            BuildOptions(withDaemon = true,
                    androidHome = File("../../../dependencies/android-sdk-for-tests"),
                    androidGradlePluginVersion = ANDROID_GRADLE_PLUGIN_VERSION)

    override fun defaultBuildOptions(): BuildOptions =
            super.defaultBuildOptions().copy(withDaemon = true)

    private fun CompiledProject.assertKaptSuccessful() {
        assertContains("Kapt: Annotation processing complete, 0 errors, 0 warnings")
    }

    @Test
    fun testSimple() {
        val project = Project("simple", GRADLE_VERSION, directoryPrefix = "kapt2")

        project.build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/generated/source/kapt2/main/example/TestClassGenerated.java")
            assertFileExists("build/classes/main/example/TestClass.class")
            assertFileExists("build/classes/main/example/TestClassGenerated.class")
            assertFileExists("build/classes/main/example/SourceAnnotatedTestClassGenerated.class")
            assertFileExists("build/classes/main/example/BinaryAnnotatedTestClassGenerated.class")
            assertFileExists("build/classes/main/example/RuntimeAnnotatedTestClassGenerated.class")
            assertContains("example.JavaTest PASSED")
        }

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin UP-TO-DATE")
            assertContains(":compileJava UP-TO-DATE")
        }
    }

    @Test
    fun testSimpleWithIC() {
        val options = defaultBuildOptions().copy(incremental = true)
        val project = Project("simple", GRADLE_VERSION, directoryPrefix = "kapt2")

        project.build("build", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertContains(":compileKotlin")
            assertContains(":compileJava")
        }

        val files = listOf("InternalDummy.kt", "test.kt")
        kotlin.repeat(2) { i ->
            project.projectDir.getFileByName(files[i]).appendText(" ")

            project.build("build", options = options) {
                assertSuccessful()
                assertKaptSuccessful()
                assertContains(":compileKotlin")
                assertContains(":compileJava")
            }
        }
    }

    @Test
    fun testInheritedAnnotations() {
        Project("inheritedAnnotations", GRADLE_VERSION, directoryPrefix = "kapt2").build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("build/generated/source/kapt2/main/example/TestClassGenerated.java")
            assertFileExists("build/generated/source/kapt2/main/example/AncestorClassGenerated.java")
            assertFileExists("build/classes/main/example/TestClassGenerated.class")
            assertFileExists("build/classes/main/example/AncestorClassGenerated.class")
        }
    }

    @Test
    fun testButterKnife() {
        val project = Project("android-butterknife", GRADLE_VERSION, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("compileReleaseSources", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("app/build/generated/source/kapt2/release/org/example/kotlin/butterknife/SimpleActivity\$\$ViewBinder.java")
            assertFileExists("app/build/intermediates/classes/release/org/example/kotlin/butterknife/SimpleActivity\$\$ViewBinder.class")
            assertFileExists("app/build/intermediates/classes/release/org/example/kotlin/butterknife/SimpleAdapter\$ViewHolder.class")
        }

        project.build("compileReleaseSources", options = options) {
            assertSuccessful()
            assertContains(":compileReleaseKotlin UP-TO-DATE")
            assertContains(":compileReleaseJavaWithJavac UP-TO-DATE")
        }
    }

    @Test
    fun testDagger() {
        val project = Project("android-dagger", GRADLE_VERSION, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("compileReleaseSources", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("app/build/generated/source/kapt2/release/com/example/dagger/kotlin/DaggerApplicationComponent.java")
            assertFileExists("app/build/generated/source/kapt2/release/com/example/dagger/kotlin/ui/HomeActivity_MembersInjector.java")
            assertFileExists("app/build/intermediates/classes/release/com/example/dagger/kotlin/DaggerApplicationComponent.class")
            assertFileExists("app/build/intermediates/classes/release/com/example/dagger/kotlin/AndroidModule.class")
        }
    }

    @Test
    fun testDbFlow() {
        val project = Project("android-dbflow", GRADLE_VERSION, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("compileReleaseSources", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("app/build/generated/source/kapt2/release/com/raizlabs/android/dbflow/config/GeneratedDatabaseHolder.java")
            assertFileExists("app/build/generated/source/kapt2/release/com/raizlabs/android/dbflow/config/AppDatabaseapp_Database.java")
            assertFileExists("app/build/generated/source/kapt2/release/mobi/porquenao/poc/kotlin/core/Item_Table.java")
            assertFileExists("app/build/generated/source/kapt2/release/mobi/porquenao/poc/kotlin/core/Item_Adapter.java")
        }
    }

    @Test
    fun testRealm() {
        val project = Project("android-realm", GRADLE_VERSION, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("compileReleaseSources", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("build/generated/source/kapt2/release/io/realm/CatRealmProxy.java")
            assertFileExists("build/generated/source/kapt2/release/io/realm/CatRealmProxyInterface.java")
            assertFileExists("build/generated/source/kapt2/release/io/realm/DefaultRealmModule.java")
            assertFileExists("build/generated/source/kapt2/release/io/realm/DefaultRealmModuleMediator.java")
        }
    }

    @Test
    fun testGeneratedDirectoryIsUpToDate() {
        val project = Project("generatedDirUpToDate", GRADLE_2_14_VERSION, directoryPrefix = "kapt2")

        project.build("build") {
            assertSuccessful()
            assertKaptSuccessful()
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/classes/main/example/TestClass.class")

            assertFileExists("build/generated/source/kapt2/main/example/TestClassGenerated.java")
            assertFileExists("build/generated/source/kapt2/main/example/SourceAnnotatedTestClassGenerated.java")
            assertFileExists("build/generated/source/kapt2/main/example/BinaryAnnotatedTestClassGenerated.java")
            assertFileExists("build/generated/source/kapt2/main/example/RuntimeAnnotatedTestClassGenerated.java")

            assertFileExists("build/classes/main/example/TestClassGenerated.class")
            assertFileExists("build/classes/main/example/SourceAnnotatedTestClassGenerated.class")
            assertFileExists("build/classes/main/example/BinaryAnnotatedTestClassGenerated.class")
            assertFileExists("build/classes/main/example/RuntimeAnnotatedTestClassGenerated.class")
        }

        val testKt = project.projectDir.getFileByName("test.kt")
        testKt.writeText(testKt.readText().replace("@ExampleBinaryAnnotation", ""))

        project.build("build") {
            assertSuccessful()
            assertContains(":compileKotlin")
            assertContains(":compileJava")
            assertFileExists("build/classes/main/example/TestClass.class")

            assertFileExists("build/generated/source/kapt2/main/example/TestClassGenerated.java")
            assertFileExists("build/generated/source/kapt2/main/example/SourceAnnotatedTestClassGenerated.java")
            /*!*/   assertNoSuchFile("build/generated/source/kapt2/main/example/BinaryAnnotatedTestClassGenerated.java")
            assertFileExists("build/generated/source/kapt2/main/example/RuntimeAnnotatedTestClassGenerated.java")

            assertFileExists("build/classes/main/example/TestClassGenerated.class")
            assertFileExists("build/classes/main/example/SourceAnnotatedTestClassGenerated.class")
            /*!*/   assertNoSuchFile("build/classes/main/example/BinaryAnnotatedTestClassGenerated.class")
            assertFileExists("build/classes/main/example/RuntimeAnnotatedTestClassGenerated.class")
        }
    }
}