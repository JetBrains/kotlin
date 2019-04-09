package org.jetbrains.kotlin.r4a.frames

import org.jetbrains.kotlin.r4a.AbstractR4aDiagnosticsTest

class FrameDiagnosticTests : AbstractR4aDiagnosticsTest() {

    // Ensure the simple case does not report an error
    fun testModel_Accept_Simple() = doTest(
        """
        import com.google.r4a.Model

        @Model
        class MyModel {
          var strValue = "default"
        }
        """
    )

    // Ensure @Model is not used on an open class
    fun testModel_Report_Open() = doTest(
        """
        import com.google.r4a.Model

        @Model
        open class <!OPEN_MODEL!>MyModel<!> {
          var strValue = "default"
        }
        """
    )

    // Ensure @Model is not used on an abstract class
    fun testModel_Report_Abstract() = doTest(
        """
        import com.google.r4a.Model

        @Model
        abstract class <!OPEN_MODEL!>MyModel<!> {
          var strValue = "default"
        }
        """
    )

    // Ensure @Model is not used on a class that specifies a base class
    fun testModel_Report_Inheritance() = doTest(
        """
        import com.google.r4a.Model

        open class NonModel { }

        @Model
        class <!UNSUPPORTED_MODEL_INHERITANCE!>MyModel<!> : NonModel() {
          var strValue = "default"
        }
        """
    )
}