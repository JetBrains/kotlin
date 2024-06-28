import plugins.KotlinBuildPublishingPlugin

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

// Disable releasing for this plugin
// It is not intended to be released publicly
tasks.withType<PublishToMavenRepository>()
    .configureEach {
        if (name.endsWith("PublicationTo${KotlinBuildPublishingPlugin.REPOSITORY_NAME}Repository")) {
            enabled = false
        }
    }

tasks.named("publishPlugins") {
    enabled = false
}