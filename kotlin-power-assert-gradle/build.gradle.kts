import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jmailen.gradle.kotlinter.tasks.FormatTask
import org.jmailen.gradle.kotlinter.tasks.LintTask

plugins {
  id("java-gradle-plugin")
  kotlin("jvm")
  id("com.gradle.plugin-publish")
  id("com.github.gmazzo.buildconfig")
  id("org.jmailen.kotlinter")
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(kotlin("gradle-plugin-api"))
}

buildConfig {
  val project = project(":kotlin-power-assert-plugin")
  packageName(project.group.toString())
  buildConfigField("String", "PLUGIN_GROUP_ID", "\"${project.group}\"")
  buildConfigField("String", "PLUGIN_ARTIFACT_ID", "\"${project.name}\"")
  buildConfigField("String", "PLUGIN_VERSION", "\"${project.version}\"")
}

gradlePlugin {
  website.set("https://github.com/bnorm/kotlin-power-assert")
  vcsUrl.set("https://github.com/bnorm/kotlin-power-assert.git")
  plugins {
    create("kotlinPowerAssert") {
      id = "com.bnorm.power.kotlin-power-assert"
      displayName = "Kotlin Power Assertion Plugin"
      description = "Kotlin Compiler Plugin to add power to your assertions"
      implementationClass = "com.bnorm.power.PowerAssertGradlePlugin"
      tags.set(listOf("kotlin", "power-assert"))
    }
  }
}
tasks.withType<JavaCompile> {
  sourceCompatibility = "1.8"
  targetCompatibility = "1.8"
}

tasks.withType<KotlinCompile> {
  kotlinOptions.jvmTarget = "1.8"
}

tasks.named("publish") {
  dependsOn("publishPlugins")
}

publishing {
  repositories {
    maven {
      name = "test"
      url = uri(rootProject.layout.buildDirectory.dir("localMaven"))
    }
  }
}
tasks.register<FormatTask>("formatBuildscripts") {
  group = "verification"
  source(layout.projectDirectory.asFileTree.matching { include("**.kts") })
}
tasks.register<LintTask>("lintBuildscripts") {
  group = "verification"
  source(layout.projectDirectory.asFileTree.matching { include("**.kts") })
}

tasks.named("lintKotlin") { dependsOn("lintBuildscripts") }
tasks.named("formatKotlin") { dependsOn("formatBuildscripts") }
