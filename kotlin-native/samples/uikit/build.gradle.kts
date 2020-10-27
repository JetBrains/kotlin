import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    kotlin("multiplatform")
}

allprojects {
    repositories {
        jcenter()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://dl.bintray.com/kotlin/kotlin-dev")
    }
}

val sdkName: String? = System.getenv("SDK_NAME")

enum class Target(val simulator: Boolean, val key: String) {
    WATCHOS_X86(true, "watchos"), WATCHOS_ARM64(false, "watchos"),
    IOS_X64(true, "ios"), IOS_ARM64(false, "ios")
}

val target = sdkName.orEmpty().let {
    when {
        it.startsWith("iphoneos") -> Target.IOS_ARM64
        it.startsWith("iphonesimulator") -> Target.IOS_X64
        it.startsWith("watchos") -> Target.WATCHOS_ARM64
        it.startsWith("watchsimulator") -> Target.WATCHOS_X86
        else -> Target.IOS_X64
    }
}

val buildType = System.getenv("CONFIGURATION")?.let {
    NativeBuildType.valueOf(it.toUpperCase())
} ?: NativeBuildType.DEBUG

kotlin {
    // Declare a target.
    // We declare only one target (either arm64 or x64)
    // to workaround lack of common platform libraries
    // for both device and simulator.
    val ios = if (!target.simulator) {
        // Device.
        iosArm64("ios")
    } else {
        // Simulator.
        iosX64("ios")
    }

    val watchos = if (!target.simulator) {
        // Device.
        watchosArm64("watchos")
    } else {
        // Simulator.
        watchosX86("watchos")
    }

    // Declare the output program.
    ios.binaries.executable(listOf(buildType)) {
        baseName = "app"
        entryPoint = "sample.uikit.main"
    }

    watchos.binaries.executable(listOf(buildType)) {
        baseName = "watchapp"
    }

    // Configure dependencies.
    val appleMain by sourceSets.creating {
        dependsOn(sourceSets["commonMain"])
    }
    sourceSets["iosMain"].dependsOn(appleMain)
    sourceSets["watchosMain"].dependsOn(appleMain)
}

// Create Xcode integration tasks.
val targetBuildDir: String? = System.getenv("TARGET_BUILD_DIR")
val executablePath: String? = System.getenv("EXECUTABLE_PATH")

val currentTarget = kotlin.targets[target.key] as KotlinNativeTarget
val kotlinBinary = currentTarget.binaries.getExecutable(buildType)
val xcodeIntegrationGroup = "Xcode integration"

val packForXCode = if (sdkName == null || targetBuildDir == null || executablePath == null) {
    // The build is launched not by Xcode ->
    // We cannot create a copy task and just show a meaningful error message.
    tasks.create("packForXCode").doLast {
        throw IllegalStateException("Please run the task from Xcode")
    }
} else {
    // Otherwise copy the executable into the Xcode output directory.
    tasks.create("packForXCode", Copy::class.java) {
        dependsOn(kotlinBinary.linkTask)

        destinationDir = file(targetBuildDir)

        val dsymSource = kotlinBinary.outputFile.absolutePath + ".dSYM"
        val dsymDestination = File(executablePath).parentFile.name + ".dSYM"
        val oldExecName = kotlinBinary.outputFile.name
        val newExecName = File(executablePath).name

        from(dsymSource) {
            into(dsymDestination)
            rename(oldExecName, newExecName)
        }

        from(kotlinBinary.outputFile) {
            rename { executablePath }
        }
    }
}
