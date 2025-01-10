plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":kotlin-metadata"))
    compileOnly(project(":kotlinx-metadata-klib"))

    api(project(":tools:kotlinp"))

    testApi(intellijCore())

    testCompileOnly(project(":kotlin-metadata"))
    testCompileOnly(project(":kotlinx-metadata-klib"))

    testImplementation(libs.junit4)
    testImplementation(projectTests(":compiler:tests-common"))
    testImplementation(projectTests(":generators:test-generator"))

    testRuntimeOnly(project(":kotlinx-metadata-klib"))
}

sourceSets {
    "main" { projectDefault() }
}
