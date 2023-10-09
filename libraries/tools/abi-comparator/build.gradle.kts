plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    implementation(commonDependency("org.jetbrains.intellij.deps:asm-all"))
    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.7.0")
    implementation("io.github.java-diff-utils:java-diff-utils:4.5")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    implementation(project(":kotlin-stdlib-jdk8"))
}

