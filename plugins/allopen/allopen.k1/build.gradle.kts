description = "Kotlin AllOpen Compiler Plugin (K1)"

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":core:descriptors"))

    compileOnly(project(":compiler:plugin-api"))
    compileOnly(project(":compiler:frontend"))

    compileOnly(intellijCore())
    runtimeOnly(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

optInToK1Deprecation()

runtimeJar()
sourcesJar()
javadocJar()
