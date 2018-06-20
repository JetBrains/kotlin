package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.isLegacyAndroidGradleVersion
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Test
import java.io.File

class Kapt3Android30IT : Kapt3AndroidIT() {
    override val androidGradlePluginVersion: String
        get() = "3.0.0-beta1"
}

open class Kapt3AndroidIT : Kapt3BaseIT() {
    companion object {
        private val GRADLE_VERSION = GradleVersionRequired.AtLeast("4.1")
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

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("app/build/generated/source/kapt/debug/org/example/kotlin/butterknife/SimpleActivity\$\$ViewBinder.java")
            assertFileExists("app/build/intermediates/classes/debug/org/example/kotlin/butterknife/SimpleActivity\$\$ViewBinder.class")
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
        val project = Project("android-dagger", GRADLE_VERSION, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("app/build/generated/source/kapt/debug/com/example/dagger/kotlin/DaggerApplicationComponent.java")
            assertFileExists("app/build/generated/source/kapt/debug/com/example/dagger/kotlin/ui/HomeActivity_MembersInjector.java")
            assertFileExists("app/build/intermediates/classes/debug/com/example/dagger/kotlin/DaggerApplicationComponent.class")
            assertFileExists("app/build/tmp/kotlin-classes/debug/com/example/dagger/kotlin/AndroidModule.class")
            if (isLegacyAndroidGradleVersion(androidGradlePluginVersion)) {
                // we don't copy classes with new AGP
                assertFileExists("app/build/intermediates/classes/debug/com/example/dagger/kotlin/AndroidModule.class")
            }
        }
    }

    @Test
    fun testKt15001() {
        val project = Project("kt15001", GRADLE_VERSION, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
        }
    }

    @Test
    fun testDbFlow() {
        val project = Project("android-dbflow", GRADLE_VERSION, directoryPrefix = "kapt2")
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
        val project = Project("android-realm", GRADLE_VERSION, directoryPrefix = "kapt2")
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
        val project = Project("android-databinding", GRADLE_VERSION, directoryPrefix = "kapt2")
        val options = androidBuildOptions()

        project.build("assembleDebug", options = options) {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("app/build/generated/source/kapt/debug/com/example/databinding/BR.java")
            assertFileExists("app/build/generated/source/kapt/debug/com/example/databinding/databinding/ActivityTestBinding.java")
        }
    }
}