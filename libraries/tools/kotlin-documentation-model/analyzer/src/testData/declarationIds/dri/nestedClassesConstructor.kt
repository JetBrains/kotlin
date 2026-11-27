// MODULE: main
// FILE: sample/NestedClassesConstructor.kt
package sample

// From DRITest.nested classes constructor should have callable name equal to class name:
// constructors of nested classes are owned by the nested class ID with an empty callable name.
class Parent { class Nested { class DoubleNested } }
