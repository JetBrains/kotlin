buildscript {
  dependencies {
    classpath("gradle.plugin.com.bnorm.power:kotlin-power-assert-gradle:0.3.0")
  }
}

plugins {
  kotlin("multiplatform") version "1.3.70"
}
apply(plugin = "com.bnorm.power.kotlin-power-assert")

repositories {
  mavenCentral()
}


kotlin {
  jvm {
    compilations.all {
      kotlinOptions {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.useIR = true
      }
    }
  }
  js {
    browser()
    nodejs()

    compilations.all {
      kotlinOptions {
        kotlinOptions.freeCompilerArgs += listOf("-Xir-produce-klib-dir", "-Xir-produce-js")
      }
    }
  }

  val osName = System.getProperty("os.name")
  when {
    "Windows" in osName -> mingwX64("native")
    "Mac OS" in osName -> macosX64("native")
    else -> linuxX64("native")
  }

  sourceSets {
    val commonMain by getting {
      dependencies {
        implementation(kotlin("stdlib"))
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }
    val jvmMain by getting {
      dependencies {
        implementation(kotlin("stdlib-jdk8"))
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test-junit"))
      }
    }
    val jsMain by getting {
      dependencies {
        implementation(kotlin("stdlib-js"))
      }
    }
    val jsTest by getting {
      dependencies {
        implementation(kotlin("test-js"))
      }
    }
    val nativeMain by getting {
      dependsOn(commonMain)
    }
    val nativeTest by getting {
      dependsOn(commonTest)
    }
  }
}

configure<com.bnorm.power.PowerAssertGradleExtension> {
  functions = listOf("kotlin.test.assertTrue", "kotlin.require")
}
