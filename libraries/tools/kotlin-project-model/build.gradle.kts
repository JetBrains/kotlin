plugins {
    kotlin("jvm")
    id("jps-compatible")
}

publish()

standardPublicJars()

dependencies {
    implementation(kotlinStdlib())

    testImplementation(kotlin("test-junit"))
}

pill {
    variant = org.jetbrains.kotlin.pill.PillExtension.Variant.FULL
}

kotlin.target.compilations.all {
    kotlinOptions.languageVersion = "1.4"
    kotlinOptions.apiVersion = "1.4"
    kotlinOptions.freeCompilerArgs += listOf("-Xskip-prerelease-check")
}

tasks.named<Jar>("jar") {
    callGroovy("manifestAttributes", manifest, project)
}
