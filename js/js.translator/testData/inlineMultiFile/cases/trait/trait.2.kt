/*
 * Copy of JVM-backend test
 * Found at: compiler/testData/codegen/boxInline/trait/trait.2.kt
 */

package test

trait InlineTrait {

    inline final fun finalInline(s: () -> String): String {
        return s()
    }

    default object {
        inline final fun finalInline(s: () -> String): String {
            return s()
        }
    }
}

class Z: InlineTrait {

}