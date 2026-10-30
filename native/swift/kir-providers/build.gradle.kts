plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
}

description = "KIR Providers - family of classes, that transforms SIR nodes into corresponding KIR nodes"

kotlin {
    explicitApi()
}

dependencies {
    compileOnly(kotlinStdlib())

    api(project(":native:swift:kir"))
    api(project(":native:swift:sir"))
}

sourceSets {
    "main" { projectDefault() }
}

publish()

runtimeJar()
sourcesJar()
javadocJar()
