/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/trait/trait.2.kt
 */

package test

internal interface InlineTrait {

    internal inline final fun finalInline(s: () -> String): String {
        return s()
    }

    companion object {
        internal inline final fun finalInline(s: () -> String): String {
            return s()
        }
    }
}

class Z: InlineTrait {

}