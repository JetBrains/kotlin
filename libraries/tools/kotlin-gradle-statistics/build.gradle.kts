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
    "test" {
        if (kotlinBuildProperties.isJpsBuildEnabled) {
            none()
        } else {
            projectDefault()
        }
    }
}

projectTest {
    workingDir = rootDir
}
