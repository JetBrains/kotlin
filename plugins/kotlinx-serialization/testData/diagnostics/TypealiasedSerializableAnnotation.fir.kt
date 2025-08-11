@file:Suppress("UNUSED_PARAMETER", "UNUSED_VARIABLE")

import kotlinx.serialization.*

// This should trigger a warning
<!TYPEALIASED_SERIALIZABLE_ANNOTATION!>typealias SerializableAlias = Serializable<!>

// This should not trigger a warning
@MetaSerializable
annotation class MySerializable

// Test case with the typealias
@SerializableAlias
class TestClass1(val prop: String)

// Test case with the proper annotation
@Serializable
class TestClass2(val prop: String)

// Test case with MetaSerializable
@MySerializable
class TestClass3(val prop: String)

// Negative test case - not using the typealias
class TestClass4(val prop: String)