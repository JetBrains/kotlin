plugins {
    kotlin("jvm")
}

kotlin {
    compilerOptions {
        /* Required to use Analysis Api */
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

dependencies {
    api(project(":native:objcexport-header-generator"))
    api(project(":analysis:analysis-api"))

    testImplementation(projectTests(":native:objcexport-header-generator"))
    testApi(project(":analysis:analysis-api-standalone"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

nativeTest("test", tag = null) {
    useJUnitPlatform()
    enableJunit5ExtensionsAutodetection()
}
