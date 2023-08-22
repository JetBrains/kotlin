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

    commonCompileOnly(project(":kotlin-compiler-embeddable"))
    commonCompileOnly(project(":kotlin-formver-compiler-plugin"))
}

gradlePlugin {
    plugins {
        create("kotlinFormVerPlugin") {
            id = "org.jetbrains.kotlin.plugin.formver"
            displayName = "Kotlin Formal Verification compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.formver.gradle.FormVerGradleSubplugin"
        }
    }
}
