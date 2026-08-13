import org.gradle.api.tasks.compile.JavaCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    id("common-configuration")
    id("test-federation-convention")
    id("com.autonomousapps.dependency-analysis")
    kotlin("jvm")
    id("project-tests-convention")
    id("test-inputs-check")
}

// `UnsafeBasedMemoryAccess.kt` uses `sun.misc.Unsafe`, which is not visible when cross-compiling
// with `--release`. The `jdk25` source set below overrides this per task.
configureJvmToolchain(JdkMajorVersion.JDK_1_8)

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
val jdk25: SourceSet = sourceSets.create("jdk25") {
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
    // The point of this task is the pre-JDK-25 path, where the multi-release JAR resolves
    // `UnsafeMemoryAccessProvider` to the `sun.misc.Unsafe` implementation. It has to name its JDK, because the
    // default test launcher is JDK 21.
    testTask(javaLauncher = JdkMajorVersion.JDK_21_0) {
        systemProperty("kotlin.unsafe.mem.test.mode", "default")
    }

    testTask(
        taskName = "testJdk25",
        javaLauncher = JdkMajorVersion.JDK_25_0,
        skipInLocalBuild = false,
    ) {
        systemProperty("kotlin.unsafe.mem.test.mode", "jdk25")
    }
}
