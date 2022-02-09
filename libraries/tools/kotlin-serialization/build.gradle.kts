import org.jetbrains.kotlin.pill.PillExtension

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

dependencies {
    compileOnly(project(":kotlin-gradle-plugin"))
    compileOnly(project(":kotlin-compiler-embeddable"))

    embedded(project(":kotlinx-serialization-compiler-plugin")) { isTransitive = false }
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
