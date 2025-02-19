// KT-68775
package foo

@JsExport
open class ExportedOne { open val message = "Fail: the original message was taken" }
open class NotExported1 : ExportedOne() { override val message = "NotExported1: ${super.message}" }
open class NotExported2 : ExportedOne() { override val message get() = "NotExported2: ${super.message}" }
open class NotExported3 : NotExported2() { override val message get() = "NotExported3: ${super.message}" }

open class Sandwich1 : ExportedOne() { override val message get() = "Sandwich1: ${super.message}" }
open class Sandwich2 : Sandwich1() { override val message = "Sandwich2: ${super.message}" }

open class MyCustomError1 : Throwable("Fail: the original message was taken") {
    override val message get() = "MyCustomError1: ${super.message}"
}
open class MyCustomError2 : MyCustomError1() {
    override val message get() = "MyCustomError2: ${super.message}"
}
open class MyCustomError3 : Throwable("Fail: the original message was taken") {
    override var message = "MyCustomError3: ${super.message}"
}

open class MyCustomError4() : Throwable("super message") {
    override val message: String?
        get() = "The overridden message"

    constructor(f: (String) -> Unit): this() {
        f("MyCustomError4: ${super.message}")
    }
}

open class MyCustomError5 : Throwable {
    override val message: String?
        get() = "The overridden message"

    constructor(f: (String) -> Unit) : super("super message") {
        f("MyCustomError5: ${super.message}")
    }
}

open class MyCustomError6 : Throwable(message = null) {
    override val message: String?
        get() = "MyCustomError6: The overridden message"
}

open class SandwichError1 : MyCustomError3() {
    override var message = "Initial: ${super.message}"
        get() = "Sandwich1: ${super.message}"
        set(value) { field = value }
}
open class SandwichError2 : SandwichError1() {
    override var message = "Sandwich2: ${super.message}"
}

@JsName("Array")
external abstract class JsArray {
    open var notExistingProperty: String?
}

class OverrideNotExistingProperty : JsArray() {
    override var notExistingProperty: String? = "Parent value is: ${super.notExistingProperty.toString()}"
}

fun box(): String {
    val firstError = MyCustomError1()
    if (firstError.message != "MyCustomError1: Fail: the original message was taken") return firstError.message
    if (firstError.message != firstError.asDynamic().message) return firstError.asDynamic().message

    val secondError = MyCustomError2()
    if (secondError.message != "MyCustomError2: MyCustomError1: Fail: the original message was taken") return secondError.message
    if (secondError.message != secondError.asDynamic().message) return secondError.asDynamic().message

    val thirdError = MyCustomError3()
    if (thirdError.message != "MyCustomError3: Fail: the original message was taken") return thirdError.message
    if (thirdError.message != thirdError.asDynamic().message) return thirdError.asDynamic().message

    var capturedMessage1 = "Fail: the message was not captured"
    val forthError = MyCustomError4 { capturedMessage1 = it }

    if (forthError.message != "The overridden message") return forthError.message.toString()
    if (forthError.message != forthError.asDynamic().message) return forthError.asDynamic().message
    if (capturedMessage1 != "MyCustomError4: super message") return "Fail: $capturedMessage1"

    var capturedMessage2 = "Fail: the message was not captured"
    val fifthError = MyCustomError5 { capturedMessage2 = it }

    if (fifthError.message != "The overridden message") return fifthError.message.toString()
    if (fifthError.message != fifthError.asDynamic().message) return fifthError.asDynamic().message
    if (capturedMessage2 != "MyCustomError5: super message") return "Fail: $capturedMessage2"

    val sixthError = MyCustomError6()

    if (sixthError.message != "MyCustomError6: The overridden message") return sixthError.message.toString()
    if (sixthError.message != sixthError.asDynamic().message) return sixthError.asDynamic().message

    val notExported1 = NotExported1()
    if (notExported1.message != "NotExported1: Fail: the original message was taken") return notExported1.message
    if (notExported1.message != notExported1.asDynamic().message) return notExported1.asDynamic().message

    val notExported2 = NotExported2()
    if (notExported2.message != "NotExported2: Fail: the original message was taken") return notExported2.message
    if (notExported2.message != notExported2.asDynamic().message) return notExported2.asDynamic().message

    val notExported3 = NotExported3()
    if (notExported3.message != "NotExported3: NotExported2: Fail: the original message was taken") return notExported3.message
    if (notExported3.message != notExported3.asDynamic().message) return notExported3.asDynamic().message

    val sandwich2 = Sandwich2()
    if (sandwich2.message != "Sandwich2: Sandwich1: Fail: the original message was taken") return sandwich2.message
    if (sandwich2.message != sandwich2.asDynamic().message) return sandwich2.asDynamic().message

    val sandwich2Error = SandwichError2()
    if (sandwich2Error.message != "Sandwich2: Sandwich1: MyCustomError3: Fail: the original message was taken") return sandwich2Error.message
    if (sandwich2Error.message != sandwich2Error.asDynamic().message) return sandwich2Error.asDynamic().message

    sandwich2Error.message = "OK"

    if (sandwich2Error.message != "OK") return sandwich2Error.message
    if (sandwich2Error.message != sandwich2Error.asDynamic().message) return sandwich2Error.asDynamic().message

    val nonExistingProperty = OverrideNotExistingProperty()

    if (nonExistingProperty.notExistingProperty != "Parent value is: null") return nonExistingProperty.notExistingProperty.toString()
    if (nonExistingProperty.notExistingProperty != nonExistingProperty.asDynamic().notExistingProperty) return nonExistingProperty.asDynamic().notExistingProperty

    nonExistingProperty.notExistingProperty = "New value"

    if (nonExistingProperty.notExistingProperty != "New value") return nonExistingProperty.notExistingProperty.toString()
    if (nonExistingProperty.notExistingProperty != nonExistingProperty.asDynamic().notExistingProperty) return nonExistingProperty.asDynamic().notExistingProperty

    return "OK"
}