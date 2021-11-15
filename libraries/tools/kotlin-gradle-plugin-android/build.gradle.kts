plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation(project(":kotlin-gradle-plugin"))
    compileOnly("com.android.tools.build:gradle:3.4.0")
}

gradlePlugin {
    plugins {
        create("kotlinAndroidTargetPlugin") {
            id = "kotlin-android-target"
            implementationClass = "org.jetbrains.kotlin.gradle.android.KotlinExternalAndroidTargetPlugin"
        }
    }
}

publish()
