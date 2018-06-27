package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.isLegacyAndroidGradleVersion
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Test
import java.io.File

class Kapt3Android32IT : Kapt3AndroidIT() {
    override val androidGradlePluginVersion: String
        get() = "3.2.0-beta01"

    override val gradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.AtLeast("4.6")
}

open class Kapt3AndroidIT : Kapt3BaseIT() {
    protected open val androidGradlePluginVersion: String
        get() = "2.3.0"

    protected open val gradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.AtLeast("4.1")

    private fun androidBuildOptions() =
        BuildOptions(
            withDaemon = true,
            androidHome = KotlinTestUtils.findAndroidSdk(),
            androidGradlePluginVersion = androidGradlePluginVersion,
            freeCommandLineArgs = listOf("-Pkapt.verbose=true")
        )

    override fun defaultBuildOptions() = androidBuildOptions()

    @Test
    fun testButterKnife() {
        val project = Project("android-butterknife", gradleVersion, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("app/build/generated/source/kapt/debug/org/example/kotlin/butterknife/SimpleActivity\$\$ViewBinder.java")

            val butterknifeJavaClassesDir =
                if (isLegacyAndroidGradleVersion(androidGradlePluginVersion))
                    "app/build/intermediates/classes/debug/org/example/kotlin/butterknife/"
                else
                    "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/org/example/kotlin/butterknife/"

            assertFileExists(butterknifeJavaClassesDir + "SimpleActivity\$\$ViewBinder.class")

            assertFileExists("app/build/tmp/kotlin-classes/debug/org/example/kotlin/butterknife/SimpleAdapter\$ViewHolder.class")

            if (isLegacyAndroidGradleVersion(androidGradlePluginVersion)) {
                // we don't copy classes with new AGP
                assertFileExists("app/build/intermediates/classes/debug/org/example/kotlin/butterknife/SimpleAdapter\$ViewHolder.class")
            }
        }

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            assertTasksUpToDate(":compileDebugKotlin", ":compileDebugJavaWithJavac")
        }
    }

    @Test
    fun testDagger() {
        val project = Project("android-dagger", gradleVersion, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("app/build/generated/source/kapt/debug/com/example/dagger/kotlin/DaggerApplicationComponent.java")
            assertFileExists("app/build/generated/source/kapt/debug/com/example/dagger/kotlin/ui/HomeActivity_MembersInjector.java")

            val daggerJavaClassesDir =
                if (isLegacyAndroidGradleVersion(androidGradlePluginVersion))
                    "app/build/intermediates/classes/debug/com/example/dagger/kotlin/"
                else
                    "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/example/dagger/kotlin/"

            assertFileExists(daggerJavaClassesDir + "DaggerApplicationComponent.class")

            assertFileExists("app/build/tmp/kotlin-classes/debug/com/example/dagger/kotlin/AndroidModule.class")
            if (isLegacyAndroidGradleVersion(androidGradlePluginVersion)) {
                // we don't copy classes with new AGP
                assertFileExists("app/build/intermediates/classes/debug/com/example/dagger/kotlin/AndroidModule.class")
            }
        }
    }

    @Test
    fun testKt15001() {
        val project = Project("kt15001", gradleVersion, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
        }
    }

    @Test
    fun testDbFlow() {
        val project = Project("android-dbflow", gradleVersion, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("app/build/generated/source/kapt/debug/com/raizlabs/android/dbflow/config/GeneratedDatabaseHolder.java")
            assertFileExists("app/build/generated/source/kapt/debug/com/raizlabs/android/dbflow/config/AppDatabaseapp_Database.java")
            assertFileExists("app/build/generated/source/kapt/debug/mobi/porquenao/poc/kotlin/core/Item_Table.java")
            assertFileExists("app/build/generated/source/kapt/debug/mobi/porquenao/poc/kotlin/core/Item_Adapter.java")
        }
    }

    @Test
    fun testRealm() {
        val project = Project("android-realm", gradleVersion, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("build/generated/source/kapt/debug/io/realm/CatRealmProxy.java")
            assertFileExists("build/generated/source/kapt/debug/io/realm/CatRealmProxyInterface.java")
            assertFileExists("build/generated/source/kapt/debug/io/realm/DefaultRealmModule.java")
            assertFileExists("build/generated/source/kapt/debug/io/realm/DefaultRealmModuleMediator.java")
        }
    }

    @Test
    open fun testDatabinding() {
        val project = Project("android-databinding", gradleVersion, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        if (!isLegacyAndroidGradleVersion(androidGradlePluginVersion)) {
            project.setupWorkingDir()

            // With new AGP, there's no need in the Databinding kapt dependency:
            project.gradleBuildScript("app").modify {
                it.lines().filterNot {
                    it.contains("kapt \"com.android.databinding:compiler")
                }.joinToString("\n")
            }

            // Workaround for KT-24915
            project.gradleBuildScript("app").appendText(
                "\n" + """
               afterEvaluate {
                    kaptDebugKotlin.dependsOn dataBindingExportFeaturePackageIdsDebug
               }
            """.trimIndent()
            )
        }

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("app/build/generated/source/kapt/debug/com/example/databinding/BR.java")

            if (isLegacyAndroidGradleVersion(androidGradlePluginVersion)) {
                assertFileExists("app/build/generated/source/kapt/debug/com/example/databinding/databinding/ActivityTestBinding.java")
            } else {
                assertFileExists("app/build/generated/source/kapt/debug/com/example/databinding/databinding/ActivityTestBindingImpl.java")
            }

            // KT-23866
            assertNotContains("The following options were not recognized by any processor")
        }
    }
}