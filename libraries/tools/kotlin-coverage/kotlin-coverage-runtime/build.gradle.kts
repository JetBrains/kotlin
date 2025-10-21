plugins {
    kotlin("jvm")
}

kotlin {
    explicitApi()
}

configureKotlinCompileTasksGradleCompatibility()

publish()

standardPublicJars()

dependencies {
    // remove stdlib dependency from api artifact in order not to affect the dependencies of the user project
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly(kotlin("stdlib", coreDepsVersion))
}
