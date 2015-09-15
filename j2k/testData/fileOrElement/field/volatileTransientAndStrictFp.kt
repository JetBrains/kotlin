// ERROR: This annotation is not applicable to target 'member property'
internal class A {
    Deprecated("")
    Volatile internal var field1 = 0

    Transient internal var field2 = 1

    // Should work even for bad modifiers
    Strictfp internal var field3 = 2.0
}