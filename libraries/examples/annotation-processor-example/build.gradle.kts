import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

description = "Simple Annotation Processor for testing kapt"

plugins {
    kotlin("jvm")
    `maven-publish` // only used for installing to mavenLocal()
}

dependencies {
    api(kotlinStdlib())
}

sourceSets {
    "test" {}
}

publishing {
    publications {
        create<MavenPublication>("main") {
            from(components["java"])
        }
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
    }
}

tasks.register("install") {
    dependsOn(tasks.named("publishToMavenLocal"))
}