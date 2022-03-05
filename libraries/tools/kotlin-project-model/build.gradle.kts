plugins {
    kotlin("jvm")
    id("jps-compatible")
}

publish()

standardPublicJars()

dependencies {
    implementation(kotlinStdlib())
    implementation(project(":kotlin-tooling-core"))
    testImplementation(kotlin("test-junit"))
}

kotlin.target.compilations.all {
    kotlinOptions.languageVersion = "1.4"
    kotlinOptions.apiVersion = "1.4"
    kotlinOptions.freeCompilerArgs += listOf("-Xskip-prerelease-check", "-Xsuppress-version-warnings")
}

tasks.named<Jar>("jar") {
    callGroovy("manifestAttributes", manifest, project)
}
