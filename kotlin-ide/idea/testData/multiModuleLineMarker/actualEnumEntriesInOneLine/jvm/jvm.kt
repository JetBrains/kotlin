package test

actual enum class <!LINE_MARKER("descr='Has declaration in common module'")!>Enum<!> { A, B, C }

/*
LINEMARKER: Has declaration in common module
TARGETS:
common.kt
expect enum class <4>Enum { <1>A, <2>B, <3>C }
*/
