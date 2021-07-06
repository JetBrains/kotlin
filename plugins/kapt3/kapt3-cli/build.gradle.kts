plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:cli"))

    compileOnly(intellijCoreDep()) { includeJars("intellij-core", rootProject = rootProject) }

    testApi(projectTests(":compiler:tests-common"))
    testApi(projectTests(":compiler"))
    testApi(commonDep("junit:junit"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar()

projectTest {
    workingDir = rootDir
    dependsOn(":dist")
}
