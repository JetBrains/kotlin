plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compileOnly(project(":core:util.runtime"))
    compileOnly(project(":core:descriptors"))

    compile(kotlinStdlib())

    testCompile(commonDep("junit:junit"))
    testCompile(projectTests(":compiler:tests-common"))

    testCompile(intellijCoreDep()) { includeJars("intellij-core") }
    testCompile(intellijDep()) {
        includeJars(
            "openapi",
            "jps-model",
            "extensions",
            "util",
            "platform-api",
            "platform-impl",
            "idea",
            "idea_rt",
            "guava",
            "trove4j",
            "picocontainer",
            "asm-all",
            "log4j",
            "jdom",
            "streamex",
            "bootstrap",
            rootProject = rootProject
        )
        isTransitive = false
    }

    Platform[192].orHigher {
        testCompile(intellijDep()) { includeJars("platform-util-ui", "platform-concurrency", "platform-objectSerializer") }
    }
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest(parallel = true) {
    dependsOn(":dist")
    workingDir = rootDir
}
