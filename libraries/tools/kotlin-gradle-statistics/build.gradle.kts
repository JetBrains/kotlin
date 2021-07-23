description = "kotlin-gradle-statistics"

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

dependencies {
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
