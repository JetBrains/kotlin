plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "SIR Providers - family of classes, that transforms KaSymbol into corresponding SIR nodes"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    implementation(project(":native:swift:sir"))
    implementation(project(":analysis:analysis-api"))
    implementation(project(":compiler:psi"))
    implementation(project(":native:analysis-api-based-export-common"))
}

sourceSets {
    "main" { projectDefault() }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
