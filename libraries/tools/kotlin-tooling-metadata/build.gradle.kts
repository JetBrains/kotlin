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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
    implementation(commonDependency("com.google.code.gson:gson"))
    testImplementation(kotlinTest("junit"))
}
