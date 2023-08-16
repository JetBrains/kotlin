import org.jetbrains.kotlin.pill.PillExtension

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

dependencies {
    commonApi(platform(project(":kotlin-gradle-plugins-bom")))
    commonApi(project(":kotlin-gradle-plugin-model"))

    commonCompileOnly(project(":compiler"))
    commonCompileOnly(project(":kotlin-sam-with-receiver-compiler-plugin"))

    testImplementation(libs.junit4)
}

gradlePlugin {
    plugins {
        create("samWithReceiver") {
            id = "org.jetbrains.kotlin.plugin.sam.with.receiver"
            displayName = "Kotlin Sam-with-receiver compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverGradleSubplugin"
        }
    }
}