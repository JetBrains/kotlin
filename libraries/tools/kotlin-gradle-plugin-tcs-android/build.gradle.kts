plugins {
    kotlin("jvm")
    `maven-publish`
}

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly("com.android.tools.build:gradle:7.0.0")
    compileOnly(project(":kotlin-gradle-plugin"))
}

/* This module is just for local development / prototyping and demos */
if (!kotlinBuildProperties.isTeamcityBuild) {
    tasks.register("install") {
        dependsOn(tasks.named("publishToMavenLocal"))
    }
}
