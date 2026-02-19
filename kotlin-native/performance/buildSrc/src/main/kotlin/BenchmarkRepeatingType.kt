package org.jetbrains.kotlin

enum class BenchmarkRepeatingType {
    /**
     * Let the benchmark executable perform warmups and repeats itself
     */
    INTERNAL,

    /**
     * Repeat and warmup the benchmark by relaunching its executable.
     *
     * Useful, when measuring the startup time, or for anything initialized only once.
     */
    EXTERNAL,
}