import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

kotlin {
    compilerOptions {
        explicitApi()

        /* Required to use Analysis Api */
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

projectTest(jUnitMode = JUnitMode.JUnit5) {
    workingDir = rootDir

    useJUnitPlatform()
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

dependencies {
    api(project(":analysis:analysis-api"))

    implementation(project(":core:compiler.common"))
    implementation(project(":core:compiler.common.native"))
    implementation(project(":kotlin-tooling-core"))

    compileOnly(project(":analysis:analysis-api-standalone"))
    compileOnly(project(":core:metadata"))
    compileOnly(project(":kotlin-metadata"))
    compileOnly(project(":kotlin-util-klib-metadata"))
    compileOnly(protobufLite())

    testImplementation(kotlinTest("junit5"))
    testImplementation(project(":compiler:tests-common", "tests-jar"))
    testImplementation(project(":analysis:analysis-api-standalone"))
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xconsistent-data-class-copy-visibility")
}
