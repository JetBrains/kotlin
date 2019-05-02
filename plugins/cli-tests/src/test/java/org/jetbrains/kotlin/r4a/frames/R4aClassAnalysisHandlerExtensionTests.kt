package org.jetbrains.kotlin.r4a.frames

import org.jetbrains.kotlin.r4a.AbstractR4aDiagnosticsTest

class R4aClassAnalysisHandlerExtensionTests : AbstractR4aDiagnosticsTest() {

    fun testReportOpen() {
        doTest("""
            import androidx.compose.Component;

            open class <!OPEN_COMPONENT!>MyComponent<!> : Component() {
               override fun compose() { }
            }
        """)
    }

    fun testAllowClosed() {
        doTest("""
            import androidx.compose.Component;

            class MyComponent: Component() {
               override fun compose() { }
            }
        """)
    }

    fun testReportAbstract() {
        doTest("""
            import androidx.compose.Component;

            abstract class <!OPEN_COMPONENT!>MyComponent<!>: Component() {
               override fun compose() { }
            }
        """)
    }
}
