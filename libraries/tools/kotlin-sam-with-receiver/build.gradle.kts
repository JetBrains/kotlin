plugins {
    id("gradle-plugin-common-configuration")
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