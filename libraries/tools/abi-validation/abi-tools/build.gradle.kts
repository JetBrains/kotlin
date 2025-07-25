plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
    id("project-tests-convention")
}

kotlin {
    explicitApi()
}

publish()

standardPublicJars()

sourceSets.named("test") {
    java.srcDir("src/test/kotlin")
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit4) {
        useJUnit()
        systemProperty("overwrite.output", System.getProperty("overwrite.output", "false"))
        systemProperty("testCasesClassesDirs", sourceSets.test.get().output.classesDirs.asPath)
        jvmArgs("-ea")
    }
}

tasks.compileTestKotlin {
    compilerOptions {
        // fix signatures and binary declarations
        freeCompilerArgs.add("-Xjvm-default=all-compatibility")
    }
}

dependencies {
    api(project(":libraries:tools:abi-validation:abi-tools-api"))

    implementation(project(":kotlin-metadata-jvm"))
    implementation(project(":kotlin-klib-abi-reader"))

    compileOnly(libs.intellij.asm)
    embedded(libs.intellij.asm)

    implementation(libs.diff.utils)

    testImplementation(kotlinTest("junit"))
    testImplementation(libs.intellij.asm)
    testImplementation(libs.junit4)
    // using `KonanTarget` class
    testImplementation(project(":native:kotlin-native-utils"))
}

runtimeJarWithRelocation {
    from(mainSourceSet.output)
    relocate("org.jetbrains.org.objectweb.asm", "org.jetbrains.kotlin.abi.tools.org.objectweb.asm")
}

// we create ABI dump only for `mainSourceSet.output` because in `libs.intellij.asm` is not a part of ABI, and we will exclude it in any way
