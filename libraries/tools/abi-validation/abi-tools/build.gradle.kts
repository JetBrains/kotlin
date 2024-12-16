plugins {
    kotlin("jvm")
}

kotlin {
    explicitApi()
}

sourceSets.named("test") {
    java.srcDir("src/test/kotlin")
}

tasks.withType<Test>().configureEach {
    useJUnit()
    systemProperty("overwrite.output", System.getProperty("overwrite.output", "false"))
    systemProperty("testCasesClassesDirs", sourceSets.test.get().output.classesDirs.asPath)
    jvmArgs("-ea")
}

tasks.compileTestKotlin {
    compilerOptions {
        // fix signatures and binary declarations
        freeCompilerArgs.add("-Xjvm-default=all-compatibility")

        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }
}

dependencies {
    api(project(":tools:abi-tools-api"))

    implementation(project(":kotlin-metadata-jvm"))
    implementation(project(":kotlin-compiler-embeddable"))

    implementation("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-tree:9.6")
    implementation("io.github.java-diff-utils:java-diff-utils:4.12")

    testImplementation(kotlinTest("junit"))
    testImplementation(libs.junit4)
}

