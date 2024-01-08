plugins {
    kotlin("multiplatform")
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvm().compilations.all {
        compilerOptions.configure {
            // log level isn't properly used to set `verbose` in the default configuration, fix is WIP in KT-64698
            verbose.convention(true)
        }
    }
    js(IR)
}
