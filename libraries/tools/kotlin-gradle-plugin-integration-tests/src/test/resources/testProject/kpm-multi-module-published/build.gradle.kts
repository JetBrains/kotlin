import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*

plugins {
    kotlin("multiplatform.pm20").version("<pluginMarkerVersion>")
    `maven-publish`
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "com.example.bar"
version = "1.0"

kotlin {
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
