plugins {
    kotlin("jvm")
}

val composeVersion = "1.7.0-alpha07"
repositories {
    google {
        content {
            includeGroup("androidx.collection")
            includeVersion("androidx.compose.runtime", "runtime", composeVersion)
            includeVersion("androidx.compose.runtime", "runtime-desktop", composeVersion)
        }
    }
}

description = "Contains the Kotlin compiler plugin for Compose used in Android Studio and IDEA"

dependencies {
    implementation(project(":kotlin-stdlib"))
    compileOnly(project(":compiler:frontend"))
    compileOnly(project(":compiler:backend.jvm"))
    compileOnly(project(":compiler:cli-base"))
    compileOnly(project(":compiler:ir.serialization.js"))
    compileOnly(project(":compiler:backend.jvm.codegen"))
    compileOnly(project(":compiler:fir:entrypoint"))

    compileOnly(intellijCore())

    testCompileOnly(project(":compiler:ir.tree"))
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
    testImplementation(projectTests(":analysis:analysis-api-fe10"))
    testImplementation(projectTests(":analysis:analysis-api-fir"))
    testImplementation(projectTests(":analysis:analysis-api-standalone"))
    testImplementation(projectTests(":analysis:analysis-api-impl-base"))
    testImplementation(projectTests(":analysis:analysis-test-framework"))
    testImplementation(projectTests(":analysis:low-level-api-fir"))
    testImplementation(projectTests(":compiler:test-infrastructure"))
    testImplementation(projectTests(":generators:analysis-api-generator"))
    testApi(project(":compiler:plugin-api"))
    testImplementation(projectTests(":compiler:tests-common-new"))

    testImplementation("androidx.compose.runtime:runtime:$composeVersion")

    testCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())

    testApi(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

optInToUnsafeDuringIrConstructionAPI()
optInToObsoleteDescriptorBasedAPI()

kotlin {
    jvmToolchain(8)
}

val generationRoot = projectDir.resolve("tests-gen")
sourceSets {
    "test" {
        this.java.srcDir(generationRoot.name)
    }
}

publish {
    artifactId = "kotlin-compose-compiler-plugin"
    pom {
        name.set("AndroidX Compose Hosted Compiler Plugin")
        developers {
            developer {
                name.set("The Android Open Source Project")
            }
        }
    }
}

projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
    dependsOn(":dist", "jar")
    workingDir = rootDir
    useJUnitPlatform()
}

val generateTests by generator("androidx.compose.compiler.plugins.kotlin.TestGeneratorKt")

standardPublicJars()
testsJar()
