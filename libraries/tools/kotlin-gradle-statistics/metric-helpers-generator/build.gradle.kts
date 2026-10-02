plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":generators"))
    implementation(project(":core:language.version-settings"))
}

application {
    mainClass.set("org.jetbrains.kotlin.statistics.generator.MainKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
    args(layout.projectDirectory.dir("../gen").asFile.absolutePath)
}
