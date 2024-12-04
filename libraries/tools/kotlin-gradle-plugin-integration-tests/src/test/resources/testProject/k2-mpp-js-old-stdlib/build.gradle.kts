import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    kotlin("multiplatform")
}

repositories {
	mavenLocal()
    mavenCentral()
}

kotlin {
    js {
        nodejs()
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common", "1.6.21"))
            }
        }

        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js", "1.6.21"))
            }
        }
    }
}
