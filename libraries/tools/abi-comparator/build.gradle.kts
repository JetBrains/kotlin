plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    implementation(commonDependency("org.apache.commons:commons-text"))

    implementation(project(":tools:kotlinp-jvm"))
    implementation(project(":kotlin-metadata-jvm"))
    implementation(project(":kotlin-metadata"))

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
}
