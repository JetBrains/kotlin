// !RENDER_DIAGNOSTICS_MESSAGES

package sample

expect class <!AMBIGUOUS_ACTUALS("Class 'A'; bottom, left"), LINE_MARKER("descr='Has actuals in common'")!>A<!> {
    fun <!AMBIGUOUS_ACTUALS("Function 'foo'; bottom, left"), LINE_MARKER("descr='Has actuals in common'")!>foo<!>(): Int
}