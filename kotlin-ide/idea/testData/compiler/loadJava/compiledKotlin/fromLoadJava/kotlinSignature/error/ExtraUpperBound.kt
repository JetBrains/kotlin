package test

import java.util.*

public open class ExtraUpperBound {
    public open fun <A : Runnable?> foo() : String? {
        throw UnsupportedOperationException()
    }
}
