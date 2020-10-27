package org.jetbrains.kotlin

enum class BenchmarkRepeatingType {
    INTERNAL,  // Let the benchmark perform warmups and repeats.
    EXTERNAL,  // Repeat by relaunching benchmark
}