plugins {
    `kotlin-dsl`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(11)

    compilerOptions {
        allWarningsAsErrors.set(true)
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

repositories {
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-dependencies")
    mavenCentral()
}
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-build-gradle-plugin:${kotlinBuildProperties.buildGradlePluginVersion}")

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.junit.jupiter.engine)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.register("checkBuild") {
    dependsOn("test")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.freeCompilerArgs.set(
        compilerOptions.freeCompilerArgs.get().filter { it != "-XXLanguage:-TypeEnhancementImprovementsInStrictMode" }
    )
}
