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
        binaries.executable()
        generateTypeScriptDefinitions()
        compilerOptions {
            // Avoid having to use JvmSerializableLambda in build script injections
            freeCompilerArgs.add("-Xexport-kdoc")
        }
    }
}
