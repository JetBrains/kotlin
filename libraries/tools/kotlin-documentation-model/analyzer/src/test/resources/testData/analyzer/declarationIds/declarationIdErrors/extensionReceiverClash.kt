// MODULE: main
// FILE: sample/ExtensionReceiverClash.kt
package sample

// Same JVM descriptor f(String); the ID distinguishes them by the extension receiver.
fun k(x: String) {}
fun String.k() {}
