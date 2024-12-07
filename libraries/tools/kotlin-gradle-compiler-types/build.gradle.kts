plugins {
    id("org.jetbrains.kotlin.jvm")
    id("jps-compatible")
}

configureKotlinCompileTasksGradleCompatibility()

sourceSets {
    "main" {
        kotlin.srcDir("src/generated/kotlin")
    }
}

dependencies {
    compileOnly(kotlinStdlib())
}
