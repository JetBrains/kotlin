import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    kotlin("jvm")
    id("project-tests-convention")

    // Can't enable the plugin because Java Security Manager is not supported with JDK 24+.
    // See KTI-3068 and https://openjdk.org/jeps/486.
    // id("test-inputs-check")
}

dependencies {
    api(kotlinStdlib())

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

/*
Note: this source set adds `MemorySegmentMemoryAccess`, which uses FFM API, available from JDK 22.
So, in theory, this can be compiled JDK 22. But the CI doesn't have JDK 22 and has JDK 25 instead.
So it is easier to compile with JDK 25.

It is possible, though, to set `jvmTarget` to 22 and use the result when running on JDK 22+.
But this can't be tested on the CI on JDK 22, so it is more reliable to use JDK 25 consistently everywhere.
*/
val jdk25: SourceSet by sourceSets.creating {
    java.srcDir("srcJdk25")
}

dependencies {
    "jdk25CompileOnly"(sourceSets.main.map { it.output })
    "jdk25CompileOnly"(kotlinStdlib())
}

tasks.named<KotlinJvmCompile>("compileJdk25Kotlin") {
    configureTaskToolchain(JdkMajorVersion.JDK_25_0)
}

tasks.named<JavaCompile>("compileJdk25Java") {
    configureTaskToolchain(JdkMajorVersion.JDK_25_0)
}

/*
Make this JAR multi-release.
Pack the classfiles compiled from the `jdk25` source set into `META-INF/versions/25`,
so that they are available at runtime only when running on JDK 25+.
In particular, the JDK 25 implementation of `UnsafeMemoryAccessProvider` shadows the original one in that case.

See https://openjdk.org/jeps/238.
*/
tasks.named<Jar>("jar") {
    into("META-INF/versions/25") {
        from(jdk25.output)
        exclude("META-INF/**")
    }
    manifest {
        attributes("Multi-Release" to true)
    }
}

// Use the JAR (not class directories) on the test classpath so that the MR-JAR mechanism
// selects the correct `UnsafeMemoryAccessProvider` for the running JDK.
// This way the selection machinery can also be tested.
tasks.withType<Test>().configureEach {
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().output + files(tasks.named<Jar>("jar")) + configurations["testRuntimeClasspath"]
}

tasks.check {
    dependsOn("test") // It does by default, but this line makes that clear and explicit.
    dependsOn("testJdk25")
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        systemProperty("kotlin.unsafe.mem.test.mode", "default")
    }

    testTask(
        taskName = "testJdk25",
        jUnitMode = JUnitMode.JUnit5,
        javaLauncher = JdkMajorVersion.JDK_25_0,
        skipInLocalBuild = false,
    ) {
        systemProperty("kotlin.unsafe.mem.test.mode", "jdk25")

        // `UnsafeBasedMemoryAccess` is also tested in this task, so this flag is needed to suppress warnings.
        // See also https://openjdk.org/jeps/498.
        jvmArgs("--sun-misc-unsafe-memory-access=allow")

        // `MemorySegmentMemoryAccess` uses "restricted" FFM APIs, so this flag is needed to suppress warnings.
        // See also https://openjdk.org/jeps/454.
        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }
}
