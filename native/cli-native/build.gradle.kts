plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":kotlin-native:backend.native", "compilerApiElements"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}

sourcesJar()
javadocJar()