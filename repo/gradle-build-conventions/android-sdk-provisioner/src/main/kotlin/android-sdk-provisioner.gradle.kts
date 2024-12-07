import org.gradle.kotlin.dsl.project
import org.jetbrains.kotlin.build.androidsdkprovisioner.AndroidSdkProvisionerExtension

configurations {
    val sdkDependencyScope = dependencyScope("androidSdkDepScope")
    register("androidSdk") {
        extendsFrom(sdkDependencyScope.get())
    }
    val jarDependencyScope = dependencyScope("androidJarDepScope")
    register("androidJar") {
        extendsFrom(jarDependencyScope.get())
    }
    val emulatorDependencyScope = dependencyScope("androidEmulatorDepScope")
    register("androidEmulator") {
        extendsFrom(emulatorDependencyScope.get())
    }
}

dependencies {
    add("androidSdkDepScope", project(":dependencies:android-sdk", configuration = "androidSdk"))
    add("androidEmulatorDepScope", project(":dependencies:android-sdk", configuration = "androidEmulator"))
    add("androidJar", project(":dependencies:android-sdk", configuration = "androidJar"))
}

extensions.create<AndroidSdkProvisionerExtension>("androidSdkProvisioner")