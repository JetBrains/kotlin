plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

dependencies {
    implementation(gradleKotlinDsl())
    implementation(project(":kotlin-gradle-plugin"))
    implementation(project(":kotlin-gradle-plugin", "externalTargetApiElements"))
    compileOnly("com.android.tools.build:gradle:7.0.0")
}

gradlePlugin {
    plugins {
        create("kotlinAndroidTargetPlugin") {
            id = "kotlin-android-target"
            implementationClass = "org.jetbrains.kotlin.gradle.android.multiplatform.KotlinExternalAndroidTargetPlugin"
        }

        create("kotlinAndroidKpmPlugin") {
            id ="kotlin-android-kpm"
            implementationClass = "org.jetbrains.kotlin.gradle.android.kpm.KotlinAndroidKpmPlugin"
        }
    }
}

publish()
