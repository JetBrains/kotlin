import org.jetbrains.kotlin.pill.PillExtension

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

dependencies {
    api(project(":kotlin-gradle-plugin-model"))

    compileOnly(project(":compiler"))
    compileOnly(project(":kotlin-sam-with-receiver-compiler-plugin"))

    testApi(commonDependency("junit"))

    embedded(project(":kotlin-sam-with-receiver-compiler-plugin")) { isTransitive = false }
}

gradlePlugin {
    plugins {
        create("samWithReceiver") {
            id = "org.jetbrains.kotlin.plugin.sam.with.receiver"
            displayName = ""
            description = displayName
            implementationClass = "org.jetbrains.kotlin.samWithReceiver.gradle.SamWithReceiverGradleSubplugin"
        }
    }
}