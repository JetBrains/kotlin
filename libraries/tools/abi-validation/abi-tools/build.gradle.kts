plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

kotlin {
    explicitApi()
}

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

dependencies {
    api(project(":libraries:tools:abi-validation:abi-tools-api"))

    implementation(project(":kotlin-metadata-jvm"))
    implementation(project(":kotlin-compiler-embeddable"))

    implementation(libs.diff.utils)

    testImplementation(kotlinTest("junit"))
    testImplementation(libs.junit4)
}

