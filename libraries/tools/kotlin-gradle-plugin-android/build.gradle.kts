plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation(project(":kotlin-gradle-plugin"))
    compileOnly("com.android.tools.build:gradle:7.0.0")
}

gradlePlugin {
    plugins {
        create("kotlinAndroidKpmPlugin") {
            id ="kotlin-android-kpm"
            implementationClass = "org.jetbrains.kotlin.gradle.android.KotlinAndroidKpmPlugin"
        }
    }
}

publish()
