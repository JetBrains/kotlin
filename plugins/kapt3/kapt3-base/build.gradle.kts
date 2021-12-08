plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(kotlinStdlib())
    compileOnly(toolsJarApi())

    testApi(commonDependency("junit:junit"))
    testCompileOnly(toolsJarApi())
    testRuntimeOnly(toolsJar())

    testCompileOnly(toolsJarApi())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest {
    workingDir = rootDir
    dependsOn(":dist")
}