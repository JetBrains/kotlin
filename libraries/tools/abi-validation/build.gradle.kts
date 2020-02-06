import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    kotlin("jvm")
    `maven-publish`
}

repositories {
    mavenCentral()
    jcenter()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.1.0")
    implementation("org.ow2.asm:asm:6.0")
    implementation("org.ow2.asm:asm-tree:6.0")
    implementation("com.googlecode.java-diff-utils:diffutils:1.3.0")
    compileOnly("org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin:1.3.61")
    testImplementation(kotlin("test-junit"))
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.apply {
        languageVersion = "1.3"
        jvmTarget = "1.8"
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
