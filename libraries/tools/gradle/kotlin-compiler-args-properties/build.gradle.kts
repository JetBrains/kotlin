import plugins.KotlinBuildPublishingPlugin

plugins {
    id("gradle-plugin-common-configuration")
}


dependencies {
    commonApi(project(":kotlin-gradle-plugin-api"))
    commonCompileOnly(project(":kotlin-gradle-plugin"))
    commonCompileOnly(gradleKotlinDsl())
}


gradlePlugin {
    plugins {
        create("kotlin-compiler-args-properties") {
            id = "org.jetbrains.kotlin.test.kotlin-compiler-args-properties"
            displayName = "GradleKotlinCompilerArgumentsPlugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.gradle.arguments.GradleKotlinCompilerArgumentsPlugin"
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