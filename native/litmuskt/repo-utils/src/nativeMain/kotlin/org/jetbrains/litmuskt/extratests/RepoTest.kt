package org.jetbrains.litmuskt.extratests

import org.jetbrains.litmuskt.*
import org.jetbrains.litmuskt.autooutcomes.*

// This is a sample test written outside of litmuskt-testsuite.
val sampleTest = litmusTest({
    object : LitmusIOutcome() {
    }
}) {
    thread {
        r1++
    }
    thread {
        r1++
    }
    spec {
        accept(2)
        interesting(1)
    }
    reset { }
}
