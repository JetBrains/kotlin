plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    implementation("org.apache.commons:commons-text:1.10.0")

    implementation(project(":tools:kotlinp"))
    implementation(project(":kotlinx-metadata-jvm"))
    implementation(project(":kotlinx-metadata"))

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
