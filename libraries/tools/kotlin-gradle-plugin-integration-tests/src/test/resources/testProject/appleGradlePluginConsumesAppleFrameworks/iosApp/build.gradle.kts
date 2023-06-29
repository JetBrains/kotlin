import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("org.jetbrains.gradle.apple.applePlugin") version <applePluginTestVersion>
}

apple {
    iosApp {
        productName = "test"
        productModuleName = "MobileApp"
        launchStoryboard = "LaunchScreen"

        dependencies {
            implementation(project(":iosLib")) {
                if (properties.containsKey("multipleFrameworks")) {
                    attributes.attribute(KotlinNativeTarget.kotlinNativeFrameworkNameAttribute, "mainStaticDebugFramework")
                }
            }
        }
    }
}

println(configurations.names.joinToString("\n"))