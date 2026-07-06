import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check-v2")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

optInToK1Deprecation()

kotlin {
    compilerOptions {
        explicitApi()
    }
}

projectTests {
    testData(isolated, "testData")

    testTask {
        val testProjectKlib = configurations.create("testProjectKlib") {
            attributes {
                attribute(Usage.USAGE_ATTRIBUTE, objects.named(KotlinUsages.KOTLIN_API))
                attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
                attribute(KotlinPlatformType.attribute, KotlinPlatformType.native)
                attribute(KotlinNativeTarget.konanTargetAttribute, HostManager.host.name)
            }
        }

        val testProjectKlibFiles = testProjectKlib.incoming.files

        dependencies {
            testProjectKlib(project("testProject"))
        }

        inputs.files(testProjectKlibFiles)
            .withPathSensitivity(PathSensitivity.RELATIVE)

        doFirst {
            systemProperty("testKlibs", testProjectKlibFiles.joinToString(File.pathSeparator))
        }
    }
}

dependencies {
    api(project(":analysis:analysis-api"))

    implementation(project(":core:compiler.common"))
    implementation(project(":kotlin-tooling-core"))

    compileOnly(project(":analysis:analysis-api-standalone"))
    compileOnly(project(":core:metadata"))
    compileOnly(project(":kotlin-metadata"))
    compileOnly(project(":kotlin-util-klib-metadata"))
    compileOnly(protobufLite())

    testImplementation(kotlinTest("junit5"))
    testImplementation(testFixtures(project(":compiler:tests-common")))
    testImplementation(project(":analysis:analysis-api-standalone"))
    testImplementation(project(":native:native.config"))
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
}
