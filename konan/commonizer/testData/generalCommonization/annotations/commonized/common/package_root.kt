expect var propertyWithoutBackingField: Double
expect val propertyWithBackingField: Double
expect val propertyWithDelegateField: Int
expect val <T : CharSequence> T.propertyWithExtensionReceiver: Int

expect fun function1(text: String): String
expect fun <Q : Number> Q.function2(): Q
