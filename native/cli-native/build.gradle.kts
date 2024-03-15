plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(intellijCore())

    implementation(project(":compiler:cli"))
    implementation(project(":compiler:cli-common"))
    implementation(project(":compiler:util"))
    implementation(project(":kotlin-native:backend.native"))
    implementation(project(":native:frontend.native"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

sourcesJar()
javadocJar()