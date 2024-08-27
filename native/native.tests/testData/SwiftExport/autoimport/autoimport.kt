// KIND: STANDALONE
// MODULE: Autoimport
// FILE: autoimport.kt

// requires `-framework AVFoundation` during linkage
fun useAVFoundation() = platform.AVFoundation.AVCaptureMetadataOutput().description()!!