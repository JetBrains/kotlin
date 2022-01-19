plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

dependencies {
    implementation(gradleKotlinDsl())
    compileOnly(project(":kotlin-gradle-plugin"))
    compileOnly("com.android.tools.build:gradle:7.0.0")
}

gradlePlugin {
    plugins {
        create("kotlinAndroidKpmPlugin") {
            id = "kotlin-kpm-android"
            implementationClass = "org.jetbrains.kotlin.gradle.android.KotlinAndroidKpmPlugin"
        }
    }
}

/* This project is for local prototyping */
if (!project.kotlinBuildProperties.isTeamcityBuild) {
    publish()
}
