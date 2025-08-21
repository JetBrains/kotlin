plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal()
    mavenCentral()
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    compilerOptions {
        freeCompilerArgs.set(listOf(
            "-Xrender-internal-diagnostic-names",
            "-Xwarning-level=NOTHING_TO_INLINE:warning"
        ))
    }
}
