package test

import java.util.*

public open class MissingUpperBound {
    public open fun <A : Runnable?> foo() : String? where A : Cloneable? {
        throw UnsupportedOperationException()
    }
}
