plugins {
    kotlin("jvm") version "1.8.0-dev-446"
    //id("jps-compatible")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
}

val kotlinVersion: String by project.properties

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.name.contains("compiler-embeddable")) {
            useVersion("1.8.0-dev-446")
            //useTarget("org.slf4j:jcl-over-slf4j:1.7.7")
        }
    }
}

dependencies {
    "org.jetbrains.kotlin:kotlin-compiler:$kotlinVersion".let {
        compileOnly(it)
        testImplementation(it)
    }

    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable") {
        version {
            strictly("1.8.0-dev-446")
        }
    }

    implementation("org.jetbrains.kotlinx:dataframe:0.9.0-dev") /*{
//        isTransitive = false
        exclude("org.jetbrains.kotlin")
    }*/
    implementation("org.jetbrains.kotlinx.dataframe:bridge-generator:0.9.0-dev") /*{
//        isTransitive = false
        exclude("org.jetbrains.kotlin")
    }*/

    testRuntimeOnly("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
    testRuntimeOnly("org.jetbrains.kotlin:kotlin-annotations-jvm:$kotlinVersion")


    testImplementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-internal-test-framework:$kotlinVersion")
    testImplementation("junit:junit:4.12")
    testImplementation("io.kotest:kotest-assertions-core:4.6.3")
    testImplementation(kotlin("test"))

    testImplementation(platform("org.junit:junit-bom:5.8.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-commons")
    testImplementation("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.platform:junit-platform-runner")
    testImplementation("org.junit.platform:junit-platform-suite-api")

//    testRuntimeOnly(toolsJar())
//    testRuntimeOnly(files("/home/nikitak/.m2/repository/org/jetbrains/kotlinx/dataframe-core/0.9.0-dev/dataframe-core-0.9.0-dev.jar"))
//    implementation(files("/home/nikitak/.m2/repository/org/jetbrains/kotlinx/dataframe-core/0.9.0-dev/dataframe-core-0.9.0-dev.jar"))
//    implementation("org.jetbrains.kotlinx:dataframe:0.9.0-dev")
//    testImplementation("io.kotest:kotest-assertions-core:4.6.3")
//    implementation("org.jetbrains.kotlinx.dataframe:bridge-generator:0.9.0-dev")
}

//dependencies {
//    "org.jetbrains.kotlin:kotlin-compiler:1.8.0-dev-446".let {
//        compileOnly(it)
//        testImplementation(it)
//    }
//
//    testRuntimeOnly("org.jetbrains.kotlin:kotlin-test:1.8.0-dev-446")
//    testRuntimeOnly("org.jetbrains.kotlin:kotlin-script-runtime:1.8.0-dev-446")
//    testRuntimeOnly("org.jetbrains.kotlin:kotlin-annotations-jvm:1.8.0-dev-446")
//
//    testImplementation("org.jetbrains.kotlinx:dataframe")
//    testImplementation("org.jetbrains.kotlinx.dataframe:bridge-generator")
//
//    testImplementation("org.jetbrains.kotlin:kotlin-reflect:1.8.0-dev-446")
//    testImplementation("org.jetbrains.kotlin:kotlin-compiler-internal-test-framework:1.8.0-dev-446")
//    //testImplementation("junit:junit:4.12")
//
//    testImplementation(platform("org.junit:junit-bom:5.8.0"))
//    testImplementation("org.junit.jupiter:junit-jupiter")
//    testImplementation("org.junit.platform:junit-platform-commons")
//    testImplementation("org.junit.platform:junit-platform-launcher")
//    testImplementation("org.junit.platform:junit-platform-runner")
//    testImplementation("org.junit.platform:junit-platform-suite-api")
//}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        useK2 = true
        freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi"
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.fir.FirImplementationDetail"
    }
}

//publish {
//    artifactId = "kotlin-dataframe-compiler"
//}
//
//runtimeJar()
//sourcesJar()
//javadocJar()
//
//tasks.create<JavaExec>("generateTests") {
//    classpath = sourceSets.test.get().runtimeClasspath
//    mainClass.set("org.jetbrains.kotlin.fir.dataframe.GenerateTestsKt")
//}

val generationRoot = projectDir.resolve("tests-gen")

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    test {
        java.setSrcDirs(listOf("tests", "tests-gen"))
        resources.setSrcDirs(listOf("testResources"))
    }
}

//projectTest(parallel = true, jUnitMode = JUnitMode.JUnit5) {
//    workingDir = rootDir
//    jvmArgs!!.removeIf { it.contains("-Xmx") }
//    maxHeapSize = "3g"
//    dependsOn(":plugins:kotlin-dataframe:plugin-annotations:jar")
//    useJUnitPlatform()
//}
//
//testsJar()
tasks.test {
    useJUnitPlatform()
    doFirst {
        setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib", "kotlin-stdlib")
        setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib-jdk8", "kotlin-stdlib-jdk8")
        setLibraryProperty("org.jetbrains.kotlin.test.kotlin-reflect", "kotlin-reflect")
        setLibraryProperty("org.jetbrains.kotlin.test.kotlin-test", "kotlin-test")
        setLibraryProperty("org.jetbrains.kotlin.test.kotlin-script-runtime", "kotlin-script-runtime")
        setLibraryProperty("org.jetbrains.kotlin.test.kotlin-annotations-jvm", "kotlin-annotations-jvm")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        useK2 = true
        freeCompilerArgs += "-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi"
    }
}

tasks.withType<JavaCompile> {
    sourceCompatibility = JavaVersion.VERSION_1_8.toString()
    targetCompatibility = JavaVersion.VERSION_1_8.toString()
}

tasks.create<JavaExec>("generateTests") {
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("org.jetbrains.kotlinx.dataframe.GenerateTestsKt")
}

fun Test.setLibraryProperty(propName: String, jarName: String) {
    val path = project.configurations
        .testRuntimeClasspath.get()
        .files
        .find { """$jarName-\d.*jar""".toRegex().matches(it.name) }
        ?.absolutePath
        ?: return
    systemProperty(propName, path)
}

