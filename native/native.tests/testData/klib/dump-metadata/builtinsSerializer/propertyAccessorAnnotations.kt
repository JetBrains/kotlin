// FIR_IDENTICAL
package test

annotation class Anno(val value: String)

@Anno("property") val v1 = ""

var v2: String
    @Anno("getter") get() = ""
    @Anno("setter") set(@Anno("setparam") value) {
    }
