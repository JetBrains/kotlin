plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("jps-compatible")
    application
}

application {
    mainClass.set("org.jetbrains.kotlin.project.modelx.cli.MainKt")
    applicationName = "kpm"
}

publish()

standardPublicJars()

repositories {
    mavenLocal()
}

dependencies {
    implementation(kotlinStdlib("jdk8"))
    // implementation(project(":kotlin-compiler"))
    implementation(kotlin("compiler", "1.5.255-SNAPSHOT"))
    implementation(project(":kotlin-compiler-runner"))
    implementation(project(":kotlin-project-model"))
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.+")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.4")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
    // implementation(project(":compiler:cli"))
    // api(project(":kotlin-project-model"))

    implementation(kotlin("gradle-plugin")) // needed for PSM
    testImplementation(kotlin("test-junit"))
}

pill {
    variant = org.jetbrains.kotlin.pill.PillExtension.Variant.FULL
}

kotlin.target.compilations.all {
    kotlinOptions.languageVersion = "1.5"
    kotlinOptions.apiVersion = "1.5"
    kotlinOptions.freeCompilerArgs += listOf("-Xskip-prerelease-check")
}

tasks.named<Jar>("jar") {
    callGroovy("manifestAttributes", manifest, project)
}
