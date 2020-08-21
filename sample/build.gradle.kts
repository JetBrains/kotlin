buildscript {
  dependencies {
    classpath("gradle.plugin.com.bnorm.power:kotlin-power-assert-gradle:0.4.0")
  }
}

plugins {
  kotlin("multiplatform") version "1.4.0"
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
  js(IR) {
    browser()
    nodejs()
  }

  val osName = System.getProperty("os.name")
  when {
    "Windows" in osName -> mingwX64("native")
    "Mac OS" in osName -> macosX64("native")
    else -> linuxX64("native")
  }

  sourceSets {
    val commonMain by getting {
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
      }
    }
    val jvmTest by getting {
      dependencies {
        implementation(kotlin("test-junit"))
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
