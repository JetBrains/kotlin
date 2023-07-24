import org.jetbrains.kotlin.gradle.plugin.KotlinPm20PluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*

plugins {
    `maven-publish`
}

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:<pluginMarkerVersion>")
    }
}

apply<KotlinPm20PluginWrapper>()

repositories {
    mavenLocal()
    mavenCentral()
}

group = "com.example.bar"
version = "1.0"

configure<KotlinPm20ProjectExtension> {
    // feel free to add more modules, variants and fragments
    mainAndTest {
        jvm
        val linuxX64 by fragments.creating(GradleKpmLinuxX64Variant::class)
    }

    val secondaryModule by modules.creating {
        jvm
        val linuxArm64 by fragments.creating(GradleKpmLinuxArm64Variant::class)

        makePublic()
    }

    test {
        dependencies { implementation(kotlin("test")) }
        jvm.dependencies { implementation(kotlin("test-junit")) }
    }

    val integrationTest by modules.creating {
        jvm
        val linuxX64 by fragments.creating(GradleKpmLinuxX64Variant::class)
    }
}

publishing {
    repositories {
        maven("../repo")
    }
}
