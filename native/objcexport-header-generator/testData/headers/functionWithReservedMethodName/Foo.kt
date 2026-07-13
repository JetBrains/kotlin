class Foo {
    /**
     * Documented [superclass] method.
     */
    fun superclass() {
    }

    /**
     * Documented [class] method.
     */
    fun `class`() {
    }

    /**
     * Documented [autorelease] method.
     */
    fun autorelease() {
    }

    /**
     * Documented [release] method.
     */
    fun release() {
    }

    /**
     * Documented [retain] method.
     */
    fun retain() {
    }

    /**
     * Documented [NULL] method.
     */
    fun NULL() {
    }

    /**
     * Documented [DEBUG] method.
     */
    fun DEBUG() {
    }

    /**
     * Documented [YES] method.
     */
    fun YES() {
    }

    /**
     * Documented [NO] method.
      */
    fun NO() {
    }

    /**
     * Documented [NO] method.
     */
    fun NO(param1: Int) {
    }

    /**
     * Documented [N] method.
     */
    fun N(ULL: Int, DEBUG: Long) {
    }
}

class Bar {

    /**
     * Documented [YES] method.
     */
    fun NULL<NO>.YES() {}
}

class NULL<T>

interface NO
