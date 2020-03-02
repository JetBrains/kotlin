description = "kotlin-gradle-statistics"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(kotlinStdlib())

    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation("junit:junit:4.12")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

publish()