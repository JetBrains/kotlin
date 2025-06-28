plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(intellijCore())

    implementation(project(":compiler:cli"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:ir.backend.native"))
    implementation(project(":compiler:util"))
    implementation(project(":native:frontend.native"))
    implementation(project(":native:driver-core"))
    implementation(project(":native:driver-frontend"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

sourcesJar()
javadocJar()

optInToK1Deprecation()

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.jetbrains.kotlin.cli.bc.K2NativeLightKt"
    }
}