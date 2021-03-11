package sample

actual interface <!LINE_MARKER("descr='Has declaration in common module'")!>Input<!>

class JSInput : AbstractInput()

// ------------------------------------

expect class <!LINE_MARKER("descr='Has actuals in JS'")!>ExpectInJsActualInJs<!>
actual class <!LINE_MARKER!>ExpectInJsActualInJs<!>