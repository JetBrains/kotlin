plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
}

kotlin {
    explicitApi()
}

publish()

standardPublicJars()

projectTests {
    testTask {
        jvmArgs("-ea")
    }
}

dependencies {
    api(project(":libraries:tools:abi-validation:abi-tools-api"))
    api(kotlinStdlib())

    implementation(project(":kotlin-metadata-jvm"))
    implementation(project(":kotlin-klib-abi-reader"))

    compileOnly(libs.intellij.asm)
    embedded(libs.intellij.asm)

    implementation(libs.diff.utils)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(kotlinStdlib())
    testImplementation(libs.intellij.asm)
    // using `KonanTarget` class
    testImplementation(project(":native:kotlin-native-utils"))
}

runtimeJarWithRelocation {
    from(mainSourceSet.output)
    relocate("org.jetbrains.org.objectweb.asm", "org.jetbrains.kotlin.abi.tools.org.objectweb.asm")
}


// we create ABI dump only for `mainSourceSet.output` because in `libs.intellij.asm` is not a part of ABI, and we will exclude it in any way
