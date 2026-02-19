plugins {
    kotlin("jvm")
}

publishJarsForIde(
    listOf(
      ":compiler:ir.serialization.native",
      ":libraries:tools:analysis-api-based-klib-reader",
      ":native:base",
      ":native:binary-options",
      ":native:objcexport-header-generator",
      ":native:objcexport-header-generator-analysis-api",
      ":native:objcexport-header-generator-k1",
      ":native:analysis-api-based-export-common"
    )
)
