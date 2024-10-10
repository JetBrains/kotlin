@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

@CName("programName")
fun programName() {
    println("Platform.programName is " + Platform.programName + " within library")
}