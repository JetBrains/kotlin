plugins {
    kotlin("jvm")
    id("jps-compatible")
}

description = "Kotlin/Native utils"

dependencies {
    compileOnly(kotlinStdlib())
    api(project(":kotlin-util-io"))
    api(project(":kotlin-util-klib"))
    api(platform(project(":kotlin-gradle-plugins-bom")))

    testImplementation(commonDependency("junit:junit"))
    testImplementation(kotlinStdlib())
    testImplementation(commonDependency("org.jetbrains.kotlin:kotlin-reflect")) { isTransitive = false }
    testApiJUnit5()
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

configureKotlinCompileTasksGradleCompatibility()

tasks {
    withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

publish()

standardPublicJars()
