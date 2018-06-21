package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.isLegacyAndroidGradleVersion
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Test

class Kapt3Android30IT : Kapt3AndroidIT() {
    override val androidGradlePluginVersion: String
        get() = "3.2.0-alpha18"
}

open class Kapt3AndroidIT : Kapt3BaseIT() {
    companion object {
        private val GRADLE_VERSION = GradleVersionRequired.AtLeast("4.6")
    }

    protected open val androidGradlePluginVersion: String
        get() = "2.3.0"

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
        val project = Project("android-butterknife", GRADLE_VERSION, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("build", options = options) {
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
                assertFileExists("app/build/intermediates/classes/release/org/example/kotlin/butterknife/SimpleAdapter\$ViewHolder.class")
            }
        }

        project.build("build", options = options) {
            assertSuccessful()
            assertTasksUpToDate(":compileReleaseKotlin", ":compileReleaseJavaWithJavac")
        }
    }

    @Test
    fun testDagger() {
        val project = Project("android-dagger", GRADLE_VERSION, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("build", options = options) {
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
                assertFileExists("app/build/intermediates/classes/release/com/example/dagger/kotlin/AndroidModule.class")
            }
        }
    }

    @Test
    fun testKt15001() {
        val project = Project("kt15001", GRADLE_VERSION, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("compileReleaseSources", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
        }
    }

    @Test
    fun testDbFlow() {
        val project = Project("android-dbflow", GRADLE_VERSION, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("compileReleaseSources", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("app/build/generated/source/kapt/release/com/raizlabs/android/dbflow/config/GeneratedDatabaseHolder.java")
            assertFileExists("app/build/generated/source/kapt/release/com/raizlabs/android/dbflow/config/AppDatabaseapp_Database.java")
            assertFileExists("app/build/generated/source/kapt/release/mobi/porquenao/poc/kotlin/core/Item_Table.java")
            assertFileExists("app/build/generated/source/kapt/release/mobi/porquenao/poc/kotlin/core/Item_Adapter.java")
        }
    }

    @Test
    fun testRealm() {
        val project = Project("android-realm", GRADLE_VERSION, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("compileReleaseSources", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("build/generated/source/kapt/release/io/realm/CatRealmProxy.java")
            assertFileExists("build/generated/source/kapt/release/io/realm/CatRealmProxyInterface.java")
            assertFileExists("build/generated/source/kapt/release/io/realm/DefaultRealmModule.java")
            assertFileExists("build/generated/source/kapt/release/io/realm/DefaultRealmModuleMediator.java")
        }
    }

    @Test
    open fun testDatabinding() {
        val project = Project("android-databinding", GRADLE_VERSION, directoryPrefix = "kapt2")
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