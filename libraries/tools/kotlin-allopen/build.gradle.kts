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

    compileOnly(project(":kotlin-compiler-embeddable"))
    compileOnly(project(":kotlin-allopen-compiler-plugin"))

    embedded(project(":kotlin-allopen-compiler-plugin")) { isTransitive = false }
}

gradlePlugin {
    plugins {
        create("kotlinAllopenPlugin") {
            id = "org.jetbrains.kotlin.plugin.allopen"
            displayName = "Kotlin All Open compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.allopen.gradle.AllOpenGradleSubplugin"
        }
        create("kotlinSpringPlugin") {
            id = "org.jetbrains.kotlin.plugin.spring"
            displayName = "Kotlin Spring compiler plugin"
            description = displayName
            implementationClass = "org.jetbrains.kotlin.allopen.gradle.SpringGradleSubplugin"
        }
    }
}
