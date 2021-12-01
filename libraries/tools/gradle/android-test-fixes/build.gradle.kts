import plugins.KotlinBuildPublishingPlugin

plugins {
    id("java-gradle-plugin")
    id("gradle-plugin-common-configuration")
    id("com.gradle.plugin-publish")
}

repositories {
    google()
}

dependencies {
    compileOnly(gradleKotlinDsl())
    compileOnly("com.android.tools.build:gradle:3.4.0")
    compileOnly("com.android.tools.build:gradle-api:3.4.0")
    compileOnly("com.android.tools.build:builder:3.4.0")
    compileOnly("com.android.tools.build:builder-model:3.4.0")
}

configure<GradlePluginDevelopmentExtension> {
    isAutomatedPublishing = false
}

gradlePlugin {
    (plugins) {
        create("android-test-fixes") {
            id = "org.jetbrains.kotlin.test.fixes.android"
            implementationClass = "org.jetbrains.kotlin.gradle.test.fixes.android.AndroidTestFixesPlugin"
        }
    }
}

pluginBundle {
    (plugins) {
        named("android-test-fixes") {
            id = "org.jetbrains.kotlin.test.fixes.android"
            displayName = "AndroidTestFixes"
        }
    }
}

publishPluginMarkers()

// Disable releasing for this plugin
// It is not intended to be released publicly
tasks.withType<PublishToMavenRepository>()
    .configureEach {
        if (name.endsWith("PublicationTo${KotlinBuildPublishingPlugin.REPOSITORY_NAME}Repository")) {
            enabled = false
        }
    }