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

    commonCompileOnly(project(":kotlin-gradle-plugin"))
    commonCompileOnly(project(":kotlin-compiler-embeddable"))
}

gradlePlugin {
    plugins {
        create("kotlinSerialization") {
            id = "org.jetbrains.kotlin.plugin.serialization"
            displayName = "Kotlin compiler plugin for kotlinx.serialization library"
            description = displayName
            implementationClass = "org.jetbrains.kotlinx.serialization.gradle.SerializationGradleSubplugin"
        }
    }
}
