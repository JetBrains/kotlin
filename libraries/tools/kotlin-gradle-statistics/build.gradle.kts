description = "kotlin-gradle-statistics"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(kotlinStdlib())

    testCompile(project(":kotlin-test::kotlin-test-junit"))
    testCompile("junit:junit:4.12")
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
