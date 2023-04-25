group = "com.h0tk3y.mpp.demo"
version = "1.0"

plugins {
    kotlin("multiplatform")
}

repositories {
	val thirdPartyRepo: String by project
	maven(thirdPartyRepo)
	mavenLocal()
    mavenCentral()
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.example.thirdparty:third-party-lib:1.0")
                implementation(kotlin("stdlib-common"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
    }

    jvm {
        compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
        compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }

    js {
        nodejs()
        browser()

        compilations["main"].defaultSourceSet {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        compilations["test"].defaultSourceSet {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile> {
    kotlinOptions.freeCompilerArgs += "-Xforce-deprecated-legacy-compiler-usage"
}
