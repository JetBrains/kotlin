import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.MACOS_ARM64
import java.io.File

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

const val NATIVE_TEST_DEPENDENCY_KLIBS_CONFIGURATION_NAME = "testDependencyLibraryKlibs"

/**
 * Wrapper for [nativeTest] which provides access to external libraries via `testDependencyKlibs` system property.
 * Use this one when you need to write tests against external libraries like kotlinx.* ones.
 */
fun Project.nativeTestWithExternalDependencies(
    taskName: String,
    requirePlatformLibs: Boolean = false,
    configure: Test.() -> Unit = {}
) : TaskProvider<Test> {
    /* Configuration to resolve klibs for the current host */
    val testDependencyProjectKlibs = configurations.maybeCreate("testDependencyProjectKlibs").also { testDependencyProjectKlibs ->
        testDependencyProjectKlibs.attributes {
            attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
            attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
            attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
            /* Project dependencies shall resolve the platform from the current host */
            attribute(KotlinNativeTarget.konanTargetAttribute, HostManager.host.name)
        }

        dependencies {
            testDependencyProjectKlibs(project(":native:external-projects-test-utils:testLibraryA"))
            testDependencyProjectKlibs(project(":native:external-projects-test-utils:testLibraryB"))
            testDependencyProjectKlibs(project(":native:external-projects-test-utils:testLibraryC"))
            testDependencyProjectKlibs(project(":native:external-projects-test-utils:testInternalLibrary"))
            testDependencyProjectKlibs(project(":native:external-projects-test-utils:testExtensionsLibrary"))
        }
    }

    /* Configuration to resolve klibs for macosArm64 (used to resolve remote libraries consistently on CI and locally) */
    val testDependencyLibraryKlibs =
        configurations.maybeCreate(NATIVE_TEST_DEPENDENCY_KLIBS_CONFIGURATION_NAME).also { testDependencyLibraryKlibs ->
            testDependencyLibraryKlibs.attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
                /* Library dependencies shall resolve the macosArm64 version */
                attribute(KotlinNativeTarget.konanTargetAttribute, MACOS_ARM64.name)
            }

            dependencies {
                testDependencyLibraryKlibs("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                testDependencyLibraryKlibs("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")
                testDependencyLibraryKlibs("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
                testDependencyLibraryKlibs("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implicitDependencies("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3") {
                    because("workaround for KTIJ-30065, remove after its resolution")
                }
                implicitDependencies("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0") {
                    because("workaround for KTIJ-30065, remove after its resolution")
                }
                implicitDependencies("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1") {
                    because("workaround for KTIJ-30065, remove after its resolution")
                }
                implicitDependencies("org.jetbrains.kotlinx:kotlinx-coroutines-debug:1.8.1") {
                    because("workaround for KTIJ-30065, remove after its resolution")
                }
            }
        }

    return nativeTest(
        taskName = taskName,
        requirePlatformLibs = requirePlatformLibs,
    ) {
        /**
         * Setup klib dependencies that can be used in tests:
         * The resolved klibs will be available as classpath under the `testDependencyKlibs` System property.
         */
        run {
            /* Create a classpath (list of file paths) that will be exposed as System property */
            val testDependencyKlibsClasspath = project.files(
                testDependencyProjectKlibs.incoming.files,
                testDependencyLibraryKlibs.incoming.files
            ).elements.map { elements ->
                elements.joinToString(File.pathSeparator) { location -> location.asFile.absolutePath }
            }

            doFirst {
                systemProperty("testDependencyKlibs", testDependencyKlibsClasspath.get())
            }

            /* Add dependency files as inputs to this test task */
            inputs.files(testDependencyProjectKlibs).withPathSensitivity(PathSensitivity.RELATIVE)
        }
        configure()
    }
}

/**
 * Wrapper for [nativeTest] which helps to apply defaults expected by
 * projects under ':native:objcexport-header-generator:*'
 */
fun Project.objCExportHeaderGeneratorTest(
    taskName: String,
    testDisplayNameTag: String? = null,
    configure: Test.() -> Unit = {},
): TaskProvider<Test> {
    return nativeTestWithExternalDependencies(taskName = taskName) {
        useJUnitPlatform()
        enableJunit5ExtensionsAutodetection()

        /* Special 'Kotlin in Fleet' flag that can switch test mode to 'local development' */
        systemProperty("kif.local", project.providers.gradleProperty("kif.local").isPresent)

        /* Tests will show this displayName as an additional tag (e.g., to differentiate between K1 and AA tests) */
        if (testDisplayNameTag != null) {
            systemProperty("testDisplayName.tag", testDisplayNameTag)
        }
        configure()
    }
}
