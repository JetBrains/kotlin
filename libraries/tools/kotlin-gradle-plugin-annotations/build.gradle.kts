plugins {
    kotlin("jvm")
}

configureKotlinCompileTasksGradleCompatibility()

publish()
standardPublicJars()

dependencies {
    compileOnly(kotlinStdlib())
}