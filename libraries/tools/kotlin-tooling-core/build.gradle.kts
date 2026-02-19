plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlinx.binary-compatibility-validator")
}

publish()
sourcesJar()
javadocJar()
configureKotlinCompileTasksGradleCompatibility()

dependencies {
    val coreDepsVersion = libs.versions.kotlin.`for`.gradle.plugins.compilation.get()
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$coreDepsVersion")
    testImplementation(kotlin("stdlib", coreDepsVersion))
    testImplementation(kotlin("test-junit", coreDepsVersion))
}

tasks {
    apiBuild {
        inputJar.value(jar.flatMap { it.archiveFile })
    }
}
