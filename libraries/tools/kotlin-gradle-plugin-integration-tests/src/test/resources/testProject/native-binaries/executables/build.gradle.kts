plugins {
    id("org.jetbrains.kotlin.multiplatform").version("<pluginMarkerVersion>")
}

repositories {
    mavenLocal()
    jcenter()
}

kotlin {
    sourceSets["commonMain"].apply {
        dependencies {
            api("org.jetbrains.kotlin:kotlin-stdlib-common")
        }
    }

    <SingleNativeTarget>("host") {
        binaries {
            executable()                       // Executable with default name.
            executable("foo")                  // Custom binary name.
            executable("bar", listOf(RELEASE)) // Custom build types.

            // Configure a binary.
            executable("baz") {
                // Rename an output binary: baz.kexe -> my-baz.kexe.
                baseName = "my-baz"
                // Use a custom entry point.
                entryPoint = "foo.main"
            }
        }
    }
}
