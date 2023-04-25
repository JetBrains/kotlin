import org.jetbrains.kotlin.pill.PillExtension

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

pill {
    variant = PillExtension.Variant.FULL
}

dependencies {
    api(platform(project(":kotlin-gradle-plugins-bom")))

    compileOnly(project(":kotlin-gradle-plugin"))
    compileOnly(project(":kotlin-compiler-embeddable"))
}

gradlePlugin {
    plugins {
        create("atomicfu") {
            id = "org.jetbrains.kotlin.plugin.atomicfu"
            displayName = "Kotlin compiler plugin for kotlinx.atomicfu library"
            description = displayName
            implementationClass = "org.jetbrains.kotlinx.atomicfu.gradle.AtomicfuKotlinGradleSubplugin"
        }
    }
}