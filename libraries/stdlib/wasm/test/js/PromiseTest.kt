package test.js

import kotlin.js.Promise
import kotlin.test.Test

@JsFun("async () => { console.log('TADA'); }")
internal external fun jsAsync(): Promise

class PromiseTest {
    @Test
    fun test1(): Promise {
        val p = jsAsync().then {
            println("T1 1")
            null
        }.then {
            println("T1 2")
            null
        }
        return p
    }

    @Test
    fun test2(): Promise {
        val p = jsAsync().then {
            println("T2 1")
            null
        }.then {
            println("T2 2")
            null
        }
        return p
    }


    @Test
    fun test3(): Promise {
        val p = jsAsync().then {
            println("T3 1")
            null
        }.then {
            println("T3 2")
            null
        }
        return p
    }

    @Test
    fun test4() {
        val p = jsAsync().then {
            println("T4 1")
            null
        }.then {
            println("T4 2")
            null
        }
    }

    @Test
    fun test5(): Promise {
        val p = jsAsync().then {
            println("T5 1")
            null
        }.then {
            println("T5 2")
            null
        }
        return p
    }
}

class PromiseTest2 {
    @Test
    fun test1(): Promise {
        val p = jsAsync().then {
            println("T1 1")
            null
        }.then {
            println("T1 2")
            null
        }
        return p
    }

    @Test
    fun test2(): Promise {
        val p = jsAsync().then {
            println("T2 1")
            null
        }.then {
            println("T2 2")
            null
        }
        return p
    }


    @Test
    fun test3(): Promise {
        val p = jsAsync().then {
            println("T3 1")
            null
        }.then {
            println("T3 2")
            null
        }
        return p
    }

    @Test
    fun test4() {
        val p = jsAsync().then {
            println("T4 1")
            null
        }.then {
            println("T4 2")
            null
        }
    }

    @Test
    fun test5(): Promise {
        val p = jsAsync().then {
            println("T5 1")
            null
        }.then {
            println("T5 2")
            null
        }
        return p
    }
}

class PromiseTest3 {
    @Test
    fun test1(): Promise {
        val p = jsAsync().then {
            println("T1 1")
            null
        }.then {
            println("T1 2")
            null
        }
        return p
    }

    @Test
    fun test2(): Promise {
        val p = jsAsync().then {
            println("T2 1")
            null
        }.then {
            println("T2 2")
            null
        }
        return p
    }


    @Test
    fun test3(): Promise {
        val p = jsAsync().then {
            println("T3 1")
            null
        }.then {
            println("T3 2")
            null
        }
        return p
    }

    @Test
    fun test4() {
        val p = jsAsync().then {
            println("T4 1")
            null
        }.then {
            println("T4 2")
            null
        }
    }

    @Test
    fun test5(): Promise {
        val p = jsAsync().then {
            println("T5 1")
            null
        }.then {
            println("T5 2")
            null
        }
        return p
    }
}