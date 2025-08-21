plugins {
    id("gradle-plugin-common-configuration")
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))

    testImplementation(libs.junit4)
}

gradlePlugin {
    plugins {
        create("assignment") {
            id = "org.jetbrains.kotlin.plugin.assignment"
            displayName = "Kotlin Assignment compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.assignment.plugin.gradle.AssignmentSubplugin"
        }
    }
}
