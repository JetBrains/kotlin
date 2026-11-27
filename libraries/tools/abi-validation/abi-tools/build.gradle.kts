plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
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

    // The runtime provider must be the self-contained fat artifact (as it was before the prepare/
    // split): abi-tools' runtime classpath is packed into abi-tools-embeddable, and a thin closure
    // there makes the linkage depend on duplicate-class exclusion order (KTI-3695)
    compileOnly(project(":kotlin-metadata-jvm"))
    runtimeOnly(project(":prepare:kotlin-metadata-jvm"))

    implementation(project(":kotlin-klib-abi-reader"))

    compileOnly(libs.intellij.asm)
    embedded(libs.intellij.asm)

    implementation(libs.diff.utils)

    testImplementation(kotlinTest("junit5"))
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
