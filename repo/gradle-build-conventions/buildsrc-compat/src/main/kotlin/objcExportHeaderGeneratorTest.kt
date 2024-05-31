import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.PathSensitivity
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

/**
 * Wrapper for [nativeTest] which helps to apply defaults expected by
 * projects under ':native:objcexport-header-generator:*'
 */
fun Project.objCExportHeaderGeneratorTest(
    taskName: String,
    testDisplayNameTag: String? = null,
    configure: Test.() -> Unit = {},
) = nativeTest(
    taskName = taskName,
    tag = null,
    requirePlatformLibs = false,
) {
    /**
     * Setup klib dependencies that can be used in tests:
     * The resolved klibs will be available as classpath under the `testDependencyKlibs` System property.
     */
    run {

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
                testDependencyProjectKlibs(project(":native:objcexport-header-generator:testLibraryA"))
                testDependencyProjectKlibs(project(":native:objcexport-header-generator:testLibraryB"))
            }
        }

        /* Configuration to resolve klibs for macosArm64 (used to resolve remote libraries consistently on CI and locally) */
        val testDependencyLibraryKlibs = configurations.maybeCreate("testDependencyLibraryKlibs").also { testDependencyLibraryKlibs ->
            testDependencyLibraryKlibs.resolutionStrategy.disableDependencyVerification()
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
            }
        }

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
