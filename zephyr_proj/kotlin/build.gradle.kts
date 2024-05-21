plugins {
    kotlin("multiplatform") version "2.0.0"
}

repositories {
    mavenCentral()
}

kotlin {
  linuxX64("native") { // on Linux
    binaries {
      sharedLib {
        baseName = "kn" // on Linux and macOS
      }
    }
  }
}

tasks.wrapper {
  gradleVersion = "8.5"
  distributionType = Wrapper.DistributionType.ALL
}
