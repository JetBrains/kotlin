plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    api(kotlinStdlib())
    implementation(commonDependency("org.jetbrains.intellij.deps:asm-all"))
}