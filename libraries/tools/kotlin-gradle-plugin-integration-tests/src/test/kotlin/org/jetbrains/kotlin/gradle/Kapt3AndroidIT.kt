package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.gradle.util.checkedReplace
import org.jetbrains.kotlin.gradle.util.getFileByName
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Test

class Kapt3WorkersAndroid32IT : Kapt3Android32IT() {
    override fun kaptOptions(): KaptOptions =
        super.kaptOptions().copy(useWorkers = true)
}

open class Kapt3Android32IT : Kapt3AndroidIT() {
    override val androidGradlePluginVersion: AGPVersion
        get() = AGPVersion.v3_2_0

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.AtLeast("4.6")
}

class Kapt3Android31IT : Kapt3AndroidIT() {
    override val androidGradlePluginVersion: AGPVersion
        get() = AGPVersion.v3_1_0

    // there is a weird validation exception in testICWithAnonymousClasses with 5.0 todo: fix it
    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.InRange("4.4", "4.10.2")
}

open class Kapt3AndroidIT : Kapt3BaseIT() {
    protected open val androidGradlePluginVersion: AGPVersion
        get() = AGPVersion.v3_0_0

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.Until("4.10.2")

    override fun defaultBuildOptions() =
        super.defaultBuildOptions().copy(
            androidHome = KotlinTestUtils.findAndroidSdk(),
            androidGradlePluginVersion = androidGradlePluginVersion
        )

    @Test
    fun testButterKnife() {
        val project = Project("android-butterknife", directoryPrefix = "kapt2")

        project.build("assembleDebug") {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("app/build/generated/source/kapt/debug/org/example/kotlin/butterknife/SimpleActivity\$\$ViewBinder.java")

            val butterknifeJavaClassesDir =
                if (androidGradlePluginVersion >= AGPVersion.v3_2_0)
                    "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/org/example/kotlin/butterknife/"
                else
                    "app/build/intermediates/classes/debug/org/example/kotlin/butterknife/"

            assertFileExists(butterknifeJavaClassesDir + "SimpleActivity\$\$ViewBinder.class")

            assertFileExists("app/build/tmp/kotlin-classes/debug/org/example/kotlin/butterknife/SimpleAdapter\$ViewHolder.class")
        }

        project.build("assembleDebug") {
            assertSuccessful()
            assertTasksUpToDate(":compileDebugKotlin", ":compileDebugJavaWithJavac")
        }
    }

    @Test
    fun testDagger() {
        val project = Project("android-dagger", directoryPrefix = "kapt2")

        project.build("assembleDebug") {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("app/build/generated/source/kapt/debug/com/example/dagger/kotlin/DaggerApplicationComponent.java")
            assertFileExists("app/build/generated/source/kapt/debug/com/example/dagger/kotlin/ui/HomeActivity_MembersInjector.java")

            val daggerJavaClassesDir =
                if (androidGradlePluginVersion >= AGPVersion.v3_2_0)
                    "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes/com/example/dagger/kotlin/"
                else
                    "app/build/intermediates/classes/debug/com/example/dagger/kotlin/"

            assertFileExists(daggerJavaClassesDir + "DaggerApplicationComponent.class")

            assertFileExists("app/build/tmp/kotlin-classes/debug/com/example/dagger/kotlin/AndroidModule.class")
        }
    }

    @Test
    fun testKt15001() {
        val project = Project("kt15001", directoryPrefix = "kapt2")

        project.build("assembleDebug") {
            assertSuccessful()
            assertKaptSuccessful()
        }
    }

    @Test
    fun testDbFlow() {
        val project = Project("android-dbflow", directoryPrefix = "kapt2")

        project.build("assembleDebug") {
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
        val project = Project("android-realm", directoryPrefix = "kapt2")

        project.build("assembleDebug") {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("build/generated/source/kapt/debug/io/realm/CatRealmProxy.java")
            assertFileExists("build/generated/source/kapt/debug/io/realm/CatRealmProxyInterface.java")
            assertFileExists("build/generated/source/kapt/debug/io/realm/DefaultRealmModule.java")
            assertFileExists("build/generated/source/kapt/debug/io/realm/DefaultRealmModuleMediator.java")
        }
    }

    @Test
    fun testInterProjectIC() = with(Project("android-inter-project-ic", directoryPrefix = "kapt2")) {
        build("assembleDebug") {
            assertSuccessful()
            assertKaptSuccessful()
        }

        fun modifyAndCheck(utilFileName: String, useUtilFileName: String) {
            val utilKt = projectDir.getFileByName(utilFileName)
            utilKt.modify {
                it.checkedReplace("Int", "Number")
            }

            build("assembleDebug") {
                assertSuccessful()
                val affectedFile = projectDir.getFileByName(useUtilFileName)
                assertCompiledKotlinSources(
                    relativize(affectedFile),
                    tasks = listOf("app:kaptGenerateStubsDebugKotlin", "app:compileDebugKotlin")
                )
            }
        }

        modifyAndCheck("libAndroidUtil.kt", "useLibAndroidUtil.kt")
        modifyAndCheck("libJvmUtil.kt", "useLibJvmUtil.kt")
    }

    @Test
    fun testICWithAnonymousClasses() {
        val project = Project("icAnonymousTypes", directoryPrefix = "kapt2")
        setupDataBinding(project)

        project.build("assembleDebug") {
            assertSuccessful()
            assertKaptSuccessful()
        }

        val aKt = project.projectDir.getFileByName("a.kt").also { assert(it.exists()) }
        aKt.modify {
            assert(it.contains("CrashMe2(1000)"))
            it.replace("CrashMe2(1000)", "CrashMe2(2000)")
        }

        project.build("assembleDebug") {
            assertSuccessful()
            assertKaptSuccessful()
        }
    }

    @Test
    fun testDatabinding() {
        val project = Project("android-databinding", directoryPrefix = "kapt2")
        setupDataBinding(project)

        project.build("assembleDebug", "assembleAndroidTest") {
            assertSuccessful()
            assertKaptSuccessful()
            assertFileExists("app/build/generated/source/kapt/debug/com/example/databinding/BR.java")

            when {
                output.contains("-Aandroid.databinding.enableV2=1") -> {
                    // databinding compiler v2 was introduced in AGP 3.1.0, was enabled by default in AGP 3.2.0
                    assertNoSuchFile("library/build/generated/source/kapt/debugAndroidTest/android/databinding/DataBinderMapperImpl.java")
                    assertFileExists("app/build/generated/source/kapt/debug/com/example/databinding/databinding/ActivityTestBindingImpl.java")
                }
                androidGradlePluginVersion == AGPVersion.v3_1_0 -> {
                    assertNoSuchFile("library/build/generated/source/kapt/debugAndroidTest/android/databinding/DataBinderMapperImpl.java")
                    assertFileExists("app/build/generated/source/kapt/debug/com/example/databinding/databinding/ActivityTestBinding.java")
                }
                else -> {
                    assertNoSuchFile("library/build/generated/source/kapt/debugAndroidTest/android/databinding/DataBinderMapper.java")
                    assertFileExists("app/build/generated/source/kapt/debug/com/example/databinding/databinding/ActivityTestBinding.java")
                }
            }

            // KT-23866
            assertNotContains("The following options were not recognized by any processor")
        }
    }

    private fun setupDataBinding(project: Project) {
        project.setupWorkingDir()

        if (androidGradlePluginVersion >= AGPVersion.v3_2_0) {
            project.gradleBuildScript().modify {
                it + "\n\n" + """
                    allprojects {
                        plugins.withId("kotlin-kapt") {
                            println("${'$'}project android.databinding.enableV2=${'$'}{project.findProperty('android.databinding.enableV2')}")

                            // With new AGP, there's no need in the Databinding kapt dependency:
                            configurations.kapt.exclude group: "com.android.databinding", module: "compiler"
                        }
                    }
                """.trimIndent()
            }
        }
    }
}