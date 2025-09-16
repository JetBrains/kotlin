plugins {
    java
    kotlin("jvm")
    id("jps-compatible")
}

publish()
sourcesJar()
javadocJar()
configureKotlinCompileTasksGradleCompatibility()

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    implementation(kotlin("stdlib", coreDepsVersion))
    implementation(commonDependency("com.google.code.gson:gson"))
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(kotlin("test-junit", coreDepsVersion))
}
