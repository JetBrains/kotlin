plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    implementation(gradleKotlinDsl())
    compileOnly("com.android.tools.build:gradle:7.0.0")
    compileOnly(project(":kotlin-gradle-plugin"))
    compileOnly(project(":kotlin-gradle-plugin-idea"))
}

gradlePlugin {
    plugins {
        create("kotlinAndroidKpmPlugin") {
            id = "kotlin-kpm-android"
            implementationClass = "org.jetbrains.kotlin.gradle.android.KotlinAndroidKpmPlugin"
        }
    }
}

/* This module is just for local development / prototyping and demos */
if (!kotlinBuildProperties.isTeamcityBuild) {
    tasks.register("install") {
        dependsOn(tasks.named("publishToMavenLocal"))
    }
}
