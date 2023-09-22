plugins {
    id("gradle-plugin-common-configuration")
}


dependencies {
    commonApi(project(":kotlin-gradle-plugin-api"))
    commonApi(project(":kotlin-gradle-plugin"))
}


gradlePlugin {
    plugins {
        create("fus-statistics-gradle-plugin") {
            id = "org.jetbrains.kotlin.fus-statistics-gradle-plugin"
            displayName = "FusStatisticsPlugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.gradle.fus.FusStatisticsPlugin"
        }
    }
}