plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
}

group = "org.jetbrains.kotlin.sample.native"
version = "1.0"

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    iosX64()

    cocoapods {
        summary = "Some description for the Shared Module"
        homepage = "Link to the Shared Module homepage"
        version = "1.0"
        ios.deploymentTarget = "14.1"
        framework {
            baseName = "shared"
        }

        pod("pod1", path = project.file("pod1"))

        pod("pod2") {
            source = path(project.file("pod2"))

            useInteropBindingFrom("pod1")

            extraOpts = listOf("-compiler-option", "-fmodules")
        }

        pod("pod3") {
            source = path(project.file("pod3"))

            interopBindingDependencies.add("pod1")
        }

        pod("pod4") {
            source = path(project.file("pod4"))

            useInteropBindingFrom("pod2")
            useInteropBindingFrom("pod3")

            extraOpts = listOf("-compiler-option", "-fmodules")
        }
    }
}
