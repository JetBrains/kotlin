import plugins.KotlinBuildPublishingPlugin

plugins {
    id("gradle-plugin-common-configuration")
}

repositories {
    google()
}

dependencies {
    commonCompileOnly(gradleKotlinDsl())
    commonCompileOnly("com.android.tools.build:gradle:3.6.4") {
        isTransitive = false
    }
    commonCompileOnly("com.android.tools.build:builder:3.6.4") {
        isTransitive = false
    }
    commonCompileOnly("com.android.tools.build:builder-model:3.6.4") {
        isTransitive = false
    }
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