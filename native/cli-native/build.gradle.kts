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
    implementation(project(":kotlin-native:backend.native"))
    implementation(project(":native:frontend.native"))
    implementation(project(":compiler:cli:cli-native-klib"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

sourcesJar()
javadocJar()

optInToK1Deprecation()
