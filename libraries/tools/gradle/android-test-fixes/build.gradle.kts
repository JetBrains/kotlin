import plugins.KotlinBuildPublishingPlugin

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    id("gradle-plugin-common-configuration")
}

dependencies {
    commonCompileOnly(gradleKotlinDsl())
    commonCompileOnly(libs.android.gradle.plugin.gradle.api) {
        overrideTargetJvmVersion(11)
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
