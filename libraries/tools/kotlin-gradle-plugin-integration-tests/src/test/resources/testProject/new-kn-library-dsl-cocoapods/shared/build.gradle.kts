import org.jetbrains.kotlin.gradle.plugin.cocoapods.withPodspec

plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
}

version = "0.1"

kotlin {
    linuxX64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
}

kotlinArtifacts {
    Native.Library("mylib") {
        target = linuxX64
        isStatic = true

        withPodspec {
            attribute(
                "description", """                   |<<-DESC
                |                                      Computes the meaning of life.
                |                                      Features:
                |                                      1. Is self aware
                |                                      ...
                |                                      42. Likes candies
                |                                    DESC
                """.trimMargin()
            )

            attribute("static_framework", "true")
            attribute("requires_arc", "'true'")
            attribute("authors", "Tony O'Connor")
        }
    }
    Native.Library("myslib") {
        target = linuxX64
        isStatic = false
        modes(DEBUG)
        addModule(project(":lib"))

        withPodspec {
            attribute("version", "111")
        }
    }
    Native.Library("myslibwithoutpodspec") {
        target = linuxX64
        isStatic = false
        modes(DEBUG)
        addModule(project(":lib"))

        withPodspec {} // intentionally empty
    }
    Native.Framework("myframe") {
        modes(DEBUG, RELEASE)
        target = iosArm64
        isStatic = false
        embedBitcode = EmbedBitcodeMode.MARKER

        withPodspec {
            attribute("prefix_header_file", "false")
        }
    }
    Native.Framework("myframewihtoutpodspec") {
        modes(DEBUG, RELEASE)
        target = iosArm64
        isStatic = false
        embedBitcode = EmbedBitcodeMode.MARKER
    }
    Native.FatFramework("myfatframe") {
        targets(iosX64, iosSimulatorArm64)
        embedBitcode = EmbedBitcodeMode.DISABLE
        toolOptions {
            suppressWarnings.set(true)
        }

        withPodspec {
            attribute("name", "custom-podspec-name")
        }
    }
    Native.XCFramework {
        targets(iosX64, iosArm64, iosSimulatorArm64)
        setModules(
            project(":shared"),
            project(":lib")
        )

        withPodspec {
            attribute("version", "5.6.2")
            attribute("license", "MIT")
            attribute("homepage", "https://github.com/Alpaca/Alpaca")
            attribute("source", "{ :git => 'https://github.com/Alpaca/Alpaca.git', :tag => spec.version }")
        }

        withPodspec {
            attribute("ios.deployment_target", "10.0")
            attribute("osx.deployment_target", "10.12")
            attribute("watchos.deployment_target", "3.0")

            attribute("swift_versions", "['4', '5']")
        }

        withPodspec {
            rawStatement("    # This is raw statement that is appended 'as is' to the podspec")
            rawStatement("    spec.frameworks = 'CFNetwork'")
        }
    }
}
