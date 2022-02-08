description = "kotlin-gradle-statistics"

plugins {
    id("gradle-plugin-common-configuration")
    id("jps-compatible")
}

dependencies {
    testImplementation(project(":kotlin-test:kotlin-test-junit"))
    testImplementation(commonDependency("junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}
