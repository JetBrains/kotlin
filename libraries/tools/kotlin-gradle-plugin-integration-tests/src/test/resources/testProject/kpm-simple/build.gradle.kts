import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.project.model.*

plugins {
    kotlin("multiplatform.pm20")
}

repositories {
    mavenLocal()
    mavenCentral()
}

configure<KotlinPm20ProjectExtension> {
    main {
        jvm
    }
}
