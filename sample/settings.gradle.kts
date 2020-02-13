rootProject.name = "sample"

includeBuild("..") {
  dependencySubstitution {
    substitute(module("com.bnorm.power:kotlin-power-assert")).with(project(":kotlin-power-assert"))
    substitute(module("com.bnorm.power:kotlin-power-assert-gradle")).with(project(":kotlin-power-assert-gradle"))
    substitute(module("gradle.plugin.com.bnorm.power:kotlin-power-assert-gradle")).with(project(":kotlin-power-assert-gradle"))
  }
}
