// PLATFORM_DEPENDANT_METADATA
// DUMP_KT_IR
// LANGUAGE: +MultiPlatformProjects
// MODULE: common
package test

class VerySpecificNameInCommon(val x: Int) {
  fun foo() {}

  class Derived
}

// MODULE: main()()(common)
package test

class VerySpecificNameInPlatform(val x: Int) {
  fun foo() {}

  class Derived
}
