plugins {
    kotlin("jvm")
    id("jps-compatible")
}

configureKotlinCompileTasksGradleCompatibility()

publish()
standardPublicJars()

dependencies {
    compileOnly(kotlinStdlib())
}