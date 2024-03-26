plugins {
    id("com.android.application")
}

android {
    namespace = "org.jetbrains.kotlin.android.tests"

    defaultConfig {
        applicationId = "org.jetbrains.kotlin.android.tests"
        minSdk = 14
        compileSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testApplicationId = "org.jetbrains.kotlin.android.tests.gradle"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    flavorDimensions += "box"

    productFlavors {
        listOf(
            "common0",
            "common1",
            "common2",
            "common3",
            "reflect0",
            "common_ir0",
            "common_ir1",
            "common_ir2",
            "common_ir3",
            "reflect_ir0"
        ).forEach {
            create(it) {
                dimension = "box"
            }
        }
    }

    testOptions.managedDevices.localDevices {
        create("nexus") {
            // A lower resolution device is used here for better emulator performance
            device = "Nexus One"

            if (project.hasProperty("forceArmEmulator")) {
                apiLevel = 27
                systemImageSource = "aosp"
            } else {
                apiLevel = 30
                systemImageSource = "aosp-atd"
            }
        }
    }
}


val jarTestFolders by tasks.registering {
    inputs.dir("test-classes")
        .withPropertyName("classDirectories")
        .withPathSensitivity(PathSensitivity.RELATIVE)

    val testClassesDirectories = file("test-classes").listFiles().filter { it.isDirectory }
    val libsJars = testClassesDirectories.map { file("libs").resolve("${it.name}.jar") }
    outputs.files(libsJars).withPropertyName("libsJars")

    doLast {
        testClassesDirectories.forEach { dir ->
            ant.withGroovyBuilder {
                "jar"("basedir" to dir.path, "destfile" to "libs/${dir.name}.jar")
            }
        }
    }
}

tasks.preBuild {
    dependsOn(jarTestFolders)
}

val kotlin_version: String by project

dependencies {
    implementation(kotlin("stdlib", kotlin_version))
    implementation(kotlin("test-junit", kotlin_version))
    implementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")

    android.applicationVariants.configureEach {
        productFlavors.forEach { flavor ->
            val configuration = "${flavor.name}Implementation"
            configuration(project.fileTree("libs") { include("${flavor.name}.jar") })
            if (flavor.name.startsWith("reflect")) {
                configuration(kotlin("reflect", kotlin_version))
            }
        }
    }
}

if (project.hasProperty("forceArmEmulator")) {
    tasks.withType<com.android.build.gradle.internal.tasks.ManagedDeviceInstrumentationTestSetupTask> {
        doFirst {
            org.gradle.api.internal.provider.AbstractProperty::class.java.getDeclaredField("value").also {
                it.isAccessible = true
                it.set(abi, org.gradle.api.internal.provider.Providers.of("arm64-v8a"))
            }
        }
    }
}
