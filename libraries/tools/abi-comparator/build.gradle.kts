plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    implementation(commonDependency("org.apache.commons:commons-text"))

    implementation(project(":tools:kotlinp-jvm"))
    implementation(project(":kotlinx-metadata-jvm"))
    implementation(project(":kotlinx-metadata"))

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
