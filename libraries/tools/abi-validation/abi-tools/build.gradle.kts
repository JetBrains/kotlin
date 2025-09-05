plugins {
    kotlin("jvm")
    id("project-tests-convention")
}

kotlin {
    explicitApi()
}

publish()

standardPublicJars()

tasks.compileTestKotlin {
    // override default mode from common `common-configuration`
    // we can't use `jvmDefault` because it has lower priority than `freeCompilerArgs`
    compilerOptions {
        freeCompilerArgs.add("-jvm-default=enable")
    }
}

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

dependencies {
    api(project(":libraries:tools:abi-validation:abi-tools-api"))
    api(kotlinStdlib())

    implementation(project(":kotlin-metadata-jvm"))
    implementation(project(":kotlin-klib-abi-reader"))

    compileOnly(libs.intellij.asm)
    embedded(libs.intellij.asm)

    implementation(libs.diff.utils)

    testImplementation(kotlinTest("junit"))
    testImplementation(libs.junit4)
    testImplementation(kotlinStdlib())
    // using `KonanTarget` class
    testImplementation(project(":native:kotlin-native-utils"))
}

runtimeJarWithRelocation {
    from(mainSourceSet.output)
    relocate("org.jetbrains.org.objectweb.asm", "org.jetbrains.kotlin.abi.tools.org.objectweb.asm")
}

tasks.compileTestKotlin {
    compilerOptions.freeCompilerArgs.add("-jvm-default=enable")
}

// we create ABI dump only for `mainSourceSet.output` because in `libs.intellij.asm` is not a part of ABI, and we will exclude it in any way
