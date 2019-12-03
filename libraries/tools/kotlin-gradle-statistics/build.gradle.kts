description = "kotlin-gradle-statistics"

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(kotlinStdlib())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    workingDir = rootDir
}

runtimeJar(rewriteDefaultJarDepsToShadedCompiler())
