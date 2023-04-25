import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("org.jetbrains.gradle.apple.applePlugin") version "222.4550-0.21-dev-0"
}

apple {
    iosApp {
        productName = "test"
        productModuleName = "MobileApp"
        launchStoryboard = "LaunchScreen"

        dependencies {
            implementation(project(":iosLib")) {
                if (properties.containsKey("multipleFrameworks")) {
                    attributes.attribute(KotlinNativeTarget.kotlinNativeFrameworkNameAttribute, "mainStatic")
                }
            }
        }
    }
}

println(configurations.names.joinToString("\n"))