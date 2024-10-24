import plugins.KotlinBuildPublishingPlugin

plugins {
    id("gradle-plugin-common-configuration")
}

repositories {
    google()
}

dependencies {
    commonCompileOnly(gradleKotlinDsl())
    commonCompileOnly(libs.android.gradle.plugin.gradle) { isTransitive = false }
    commonCompileOnly(libs.android.gradle.plugin.gradle.api) { isTransitive = false }
    commonCompileOnly(libs.android.gradle.plugin.builder) { isTransitive = false }
    commonCompileOnly(libs.android.gradle.plugin.builder.model) { isTransitive = false }
}

gradlePlugin {
    plugins {
        create("android-test-fixes") {
            id = "org.jetbrains.kotlin.test.fixes.android"
            displayName = "AndroidTestFixes"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.gradle.test.fixes.android.AndroidTestFixesPlugin"
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