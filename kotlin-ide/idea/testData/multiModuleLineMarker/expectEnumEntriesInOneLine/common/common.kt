package test

expect enum class <lineMarker descr="Has actuals in JVM">Enum</lineMarker> { A, B, C, D }

/*
LINEMARKER: Has actuals in JVM
TARGETS:
jvm.kt
actual enum class <5>Enum { <1>A, <2>B, <3>C, <4>D }
*/
