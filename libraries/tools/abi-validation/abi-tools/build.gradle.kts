plugins {
    kotlin("jvm")
}

kotlin {
    explicitApi()
}

sourceSets.named("test") {
    java.srcDir("src/test/kotlin")
}

configureKotlinCompileTasksGradleCompatibility()

projectTest {
    useJUnit()
    systemProperty("overwrite.output", System.getProperty("overwrite.output", "false"))
    systemProperty("testCasesClassesDirs", sourceSets.test.get().output.classesDirs.asPath)
    jvmArgs("-ea")
}

dependencies {
    api(project(":libraries:tools:abi-validation:abi-tools-api"))

    implementation(project(":kotlin-metadata-jvm"))
    implementation(project(":kotlin-compiler-embeddable"))

    implementation(libs.intellij.asm)
    implementation(libs.diff.utils)

    testImplementation(kotlinTest("junit"))
    testImplementation(libs.junit4)
}

