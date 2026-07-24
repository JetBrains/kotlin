pluginManagement {
    includeBuild("../gradle-settings-conventions")

    repositories {
        // duplicated from repositories.kt because pluginManagement block annot access to it.
        exclusiveContent {
            forRepository {
                maven {
                    name = "kotlin-dependencies"
                    setUrl("https://redirector.kotlinlang.org/maven/kotlin-dependencies")
                }
            }
            filter {
                includeModule("org.jetbrains.dukat", "dukat")
                includeModule("org.jetbrains.kotlin", "android-dx")
                includeModule("org.jetbrains.kotlin", "jcabi-aether")
                includeModule("org.jetbrains.kotlin", "protobuf-lite")
                includeModule("org.jetbrains.kotlin", "protobuf-relocated")
                includeModule("org.jetbrains.kotlinx", "kotlinx-metadata-klib")
            }
        }
        exclusiveContent {
            forRepository {
                google()
            }
            filter {
                includeGroupByRegex("""com\.android(\..*)?""")
                includeGroupByRegex("""androidx(\..*)?""")
                includeGroup("com.google.testing.platform")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("kotlin-bootstrap")
    id("develocity")
    id("jvm-toolchain-provisioning")
    id("kotlin-daemon-config")
    id("cache-redirector")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
    repositories {
        intellijDependencies()
        googleAndroidRepository()
        mavenCentral()
        gradlePluginPortal()
    }
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
}

include(":buildsrc-compat")
include(":analysis-api-artifact")
include(":generators")
include(":project-tests-convention")
include(":android-sdk-provisioner")
include(":asm-deprecating-transformer")
include(":binary-compatibility-extended")
include(":gradle-plugins-documentation")
include(":gradle-plugins-common")
include(":kgp-npm-tooling-helper")
include(":d8-configuration")
// TODO: uncomment after bootstrap
// include(":swc-configuration")
include(":foreign-class-usage-checker")
include(":binaryen-configuration")
include(":nodejs-configuration")
include(":test-data-manager-convention")
include(":utilities")
include(":test-federation-convention")
include(":repo-test-fixtures")
include(":java-flight-recorder")
include(":test-inputs-check-v2")

