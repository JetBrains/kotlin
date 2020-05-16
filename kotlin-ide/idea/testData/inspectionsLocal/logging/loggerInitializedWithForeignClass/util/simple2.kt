// RUNTIME_WITH_FULL_JDK
package util

import java.util.logging.Logger.getLogger

class Foo {
    private val logger = getLogger(<caret>Bar::class.qualifiedName)
}

class Bar