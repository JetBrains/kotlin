// KIND: STANDALONE
// WITH_PLATFORM_LIBS
// MODULE: Autoimport
// FILE: autoimport.kt

// requires `-framework AVFoundation` during linkage
fun useAVFoundation() = platform.AVFoundation.AVPlayer().description()!!