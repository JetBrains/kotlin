
description = "Kotlin scripting support utilities"

apply { plugin("kotlin") }

dependencies {
    compile(project(":kotlin-stdlib"))
    compile(project(":kotlin-script-runtime"))
    compile(projectRuntimeJar(":kotlin-compiler"))
    compile(projectRuntimeJar(":kotlin-daemon-client"))
    compileOnly("com.jcabi:jcabi-aether:0.10.1")
    compileOnly("org.sonatype.aether:aether-api:1.13.1")
    compileOnly("org.apache.maven:maven-core:3.0.3")
    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testRuntime(project(":kotlin-reflect"))
    testRuntime("com.jcabi:jcabi-aether:0.10.1")
    testRuntime("org.sonatype.aether:aether-api:1.13.1")
    testRuntime("org.apache.maven:maven-core:3.0.3")
}

tasks.withType<Test> {
    jvmArgs("-ea", "-XX:+HeapDumpOnOutOfMemoryError", "-Xmx1200m", "-XX:+UseCodeCacheFlushing", "-XX:ReservedCodeCacheSize=128m", "-Djna.nosys=true")
    maxHeapSize = "1200m"
    systemProperty("idea.is.unit.test", "true")
    environment("NO_FS_ROOTS_ACCESS_CHECK", "true")
    ignoreFailures = true
}

runtimeJar()
sourcesJar()
javadocJar()

publish()
