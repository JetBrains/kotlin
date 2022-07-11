import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.cinterop.*

plugins {
    kotlin("multiplatform.pm20")
}

repositories {
    mavenLocal()
    mavenCentral()
}

group = "com.example.foo"
version = "1.0"

kotlin {
    main {
        jvm
        val macosX64 by fragments.creating(GradleKpmMacosX64Variant::class)
        val linuxArm64 by fragments.creating(GradleKpmLinuxArm64Variant::class)
        val linuxX64 by fragments.creating(GradleKpmLinuxX64Variant::class)
        val linuxArm32 by fragments.creating(GradleKpmLinuxArm32HfpVariant::class)

        val linux by fragments.creating {
            dependencies {
                cinterop("sampleInterop")
            }
        }
        val linux32 by fragments.creating
        val desktop by fragments.creating {
            linux.refines(this)
            macosX64.refines(this)
        }

        linuxArm64.refines(linux)
        linuxX64.refines(linux)
        linux32.refines(linux)
        linuxArm32.refines(linux32)
    }
}

tasks.withType<CinteropTask>().all {
    if (name.contains("cinteropSampleInterop")) {
        headers.from(file("src/nativeInterop/cinterop/sampleInterop.h"))
    }
}
