import plugins.KotlinBuildPublishingPlugin

plugins {
    id("gradle-plugin-common-configuration")
}

gradlePlugin {
    plugins {
        create("gradle-warnings-detector") {
            id = "org.jetbrains.kotlin.test.gradle-warnings-detector"
            displayName = "GradleWarningsDetectorPlugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.gradle.test.GradleWarningsDetectorPlugin"
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