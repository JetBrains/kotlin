plugins {
    kotlin("jvm")
}

kotlin.jvmToolchain(11)

dependencies {
    embedded(project(":plugins:compose-compiler-plugin:compiler-hosted")) { isTransitive = false }
}

description = "Compiler plugin that enables Compose"

publish {
    artifactId = "kotlin-compose-compiler-plugin-embeddable"
    pom {
        name.set("Compose Compiler")
        developers {
            developer {
                name.set("The Android Open Source Project")
            }
        }
    }
}

runtimeJarWithRelocation {
    relocate("com.intellij", "org.jetbrains.kotlin.com.intellij")
}

sourcesJar()
javadocJar()