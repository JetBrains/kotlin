plugins {
    id("common-configuration")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("java-test-fixtures")
}

dependencies {
    api(kotlinStdlib())
    compileOnly(toolsJarApi())

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testCompileOnly(toolsJarApi())
    testFixturesCompileOnly(toolsJarApi())
    testFixturesApi(kotlinStdlib())
    testRuntimeOnly(toolsJar())
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
    "testFixtures" { projectDefault() }
}

testsJar {}

projectTests {
    testTask(
        javaLauncher = JdkMajorVersion.JDK_1_8,
        maxHeapSize = testMaxHeapSizeLarge,
        // Use Parallel GC because this test runs on JDK 8.
        garbageCollector = GarbageCollector.Parallel,
    ) {
        workingDir = rootDir
    }

    withJvmStdlibAndReflect()
}
