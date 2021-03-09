import com.gradle.publish.*
import kotlinx.validation.build.*
import org.jetbrains.kotlin.gradle.tasks.*

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") apply false
    `signing`
    `maven-publish`
}

repositories {
    mavenCentral()
    jcenter()
    gradlePluginPortal()
}

sourceSets {
    test {
        java.srcDir("src/test/kotlin")
    }
}

sourceSets {
    create("functionalTest") {
        withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
        }
        resources {
            srcDir(file("src/functionalTest/resources"))
        }
        compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath
        runtimeClasspath += output + compileClasspath
    }
}

tasks.register<Test>("functionalTest") {
    testClassesDirs = sourceSets["functionalTest"].output.classesDirs
    classpath = sourceSets["functionalTest"].runtimeClasspath
}
tasks.check { dependsOn(tasks["functionalTest"]) }

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.2.0")
    implementation("org.ow2.asm:asm:9.0")
    implementation("org.ow2.asm:asm-tree:9.0")
    implementation("com.googlecode.java-diff-utils:diffutils:1.3.0")
    compileOnly("org.jetbrains.kotlin.multiplatform:org.jetbrains.kotlin.multiplatform.gradle.plugin:1.3.61")
    testImplementation(kotlin("test-junit"))

    "functionalTestImplementation"("org.assertj:assertj-core:3.18.1")
    "functionalTestImplementation"(gradleTestKit())
    "functionalTestImplementation"(kotlin("test-junit"))
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.apply {
        languageVersion = "1.3"
        jvmTarget = "1.8"
        allWarningsAsErrors = true
        // Suppress the warning about kotlin-reflect 1.3 and kotlin-stdlib 1.4 in the classpath.
        // It's incorrect in this case because we're limiting API version to 1.3 anyway.
        freeCompilerArgs += "-Xskip-runtime-version-check"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    test {
        systemProperty("overwrite.output", System.getProperty("overwrite.output", "false"))
        systemProperty("testCasesClassesDirs", sourceSets.test.get().output.classesDirs.asPath)
        jvmArgs("-ea")
    }
}

properties["DeployVersion"]?.let { version = it }

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            mavenCentralMetadata()
            mavenCentralArtifacts(project, project.sourceSets.main.get().allSource)
        }

        mavenRepositoryPublishing(project)
        mavenCentralMetadata()
    }

    publications.withType(MavenPublication::class).all {
        signPublicationIfKeyPresent(this)
    }
}

apply(plugin = "org.gradle.java-gradle-plugin")
apply(plugin = "com.gradle.plugin-publish")

extensions.getByType(PluginBundleExtension::class).apply {
    website = "https://github.com/Kotlin/binary-compatibility-validator"
    vcsUrl = "https://github.com/Kotlin/binary-compatibility-validator"
    tags = listOf("kotlin", "api-management", "binary-compatibility")
}

gradlePlugin {
    testSourceSets(sourceSets["functionalTest"])

    plugins {
        create("binary-compatibility-validator") {
            id = "org.jetbrains.kotlinx.binary-compatibility-validator"
            implementationClass = "kotlinx.validation.BinaryCompatibilityValidatorPlugin"
            displayName = "Binary compatibility validator"
            description = "Produces binary API dumps and compares them in order to verify that binary API is preserved"
        }
    }
}
