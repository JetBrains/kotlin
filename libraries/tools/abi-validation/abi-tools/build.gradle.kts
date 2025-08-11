plugins {
    kotlin("jvm")
}

kotlin {
    explicitApi()
}

publish()

standardPublicJars()

sourceSets.named("test") {
    java.srcDir("src/test/kotlin")
}

projectTest {
    useJUnit()
    systemProperty("overwrite.output", System.getProperty("overwrite.output", "false"))
    systemProperty("testCasesClassesDirs", sourceSets.test.get().output.classesDirs.asPath)
    jvmArgs("-ea")
}

tasks.compileTestKotlin {
    compilerOptions {
        // fix signatures and binary declarations
        freeCompilerArgs.add("-Xjvm-default=all-compatibility")
    }
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        enabled = true
    }
}
tasks.check {
    dependsOn(tasks.checkLegacyAbi)
}

dependencies {
    api(project(":libraries:tools:abi-validation:abi-tools-api"))

    implementation(project(":kotlin-metadata-jvm"))
    implementation(project(":kotlin-klib-abi-reader"))

    implementation(libs.intellij.asm)
    implementation(libs.diff.utils)

    testImplementation(kotlinTest("junit"))
    testImplementation(libs.junit4)
    // using `KonanTarget` class
    testImplementation(project(":native:kotlin-native-utils"))
}

