package test

import java.util.*

public open class MethodWithFunctionTypes {
    public open fun foo(f : (String?) -> String) : (String.() -> String?)? {
        throw UnsupportedOperationException()
    }
}
