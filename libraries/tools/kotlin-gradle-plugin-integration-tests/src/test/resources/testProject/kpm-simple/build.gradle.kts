import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPm20PluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.project.model.*

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${property("kotlin_version")}")
    }
}

apply<KotlinPm20PluginWrapper>()

repositories {
    mavenLocal()
    mavenCentral()
}

configure<KotlinPm20ProjectExtension> {
    main {
        jvm
    }
}
