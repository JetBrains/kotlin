package org.jetbrains.kotlin.r4a.frames

import org.jetbrains.kotlin.r4a.AbstractR4aDiagnosticsTest

class R4aClassAnalysisHandlerExtensionTests : AbstractR4aDiagnosticsTest() {

    fun testReportOpen() {
        doTest("""
            import com.google.r4a.Component;

            open class <!OPEN_COMPONENT!>MyComponent<!> : Component() {
               override fun compose() { }
            }
        """)
    }

    fun testAllowClosed() {
        doTest("""
            import com.google.r4a.Component;

            class MyComponent: Component() {
               override fun compose() { }
            }
        """)
    }

    fun testReportAbstract() {
        doTest("""
            import com.google.r4a.Component;

            abstract class <!OPEN_COMPONENT!>MyComponent<!>: Component() {
               override fun compose() { }
            }
        """)
    }
}
