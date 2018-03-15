@file:JvmName("UsingKt")
@file:JvmVersion
package kotlin.io

import java.io.Closeable
import kotlin.internal.*

/**
 * Executes the given [block] function on the given 1 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 1 [Closeable] resources.
 * @return the result of the [block] function invoked on the 1 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B>
        using( argA: A, block: (A)->B ): B {
    var closed = false
    try {
        return block(argA)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 2 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 2 [Closeable] resources.
 * @return the result of the [block] function invoked on the 2 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C>
        using( argA: A, argB: B, block: (A, B)->C ): C {
    var closed = false
    try {
        return block(argA, argB)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 3 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 3 [Closeable] resources.
 * @return the result of the [block] function invoked on the 3 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D>
        using( argA: A, argB: B, argC: C, block: (A, B, C)->D ): D {
    var closed = false
    try {
        return block(argA, argB, argC)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 4 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 4 [Closeable] resources.
 * @return the result of the [block] function invoked on the 4 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E>
        using( argA: A, argB: B, argC: C, argD: D,
               block: (A, B, C, D)->E ): E {
    var closed = false
    try {
        return block(argA, argB, argC, argD)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 5 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 5 [Closeable] resources.
 * @return the result of the [block] function invoked on the 5 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?, F>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, block: (A, B, C, D, E)->F ): F {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 6 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 6 [Closeable] resources.
 * @return the result of the [block] function invoked on the 6 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?,
        F : Closeable?, G>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, argF: F, block: (A, B, C, D, E, F)->G ): G {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE, argF)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        try {
            argF?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argF?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 7 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 7 [Closeable] resources.
 * @return the result of the [block] function invoked on the 7 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?,
        F : Closeable?, G : Closeable?, H>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, argF: F, argG: G, block: (A, B, C, D, E, F, G)->H ): H {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE,
                argF, argG)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        try {
            argF?.close()
        } catch (closeException: Exception) {}

        try {
            argG?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argF?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argG?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 8 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 8 [Closeable] resources.
 * @return the result of the [block] function invoked on the 8 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?,
        F : Closeable?, G : Closeable?, H : Closeable?, I>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, argF: F, argG: G, argH: H,
               block: (A, B, C, D, E, F, G, H)->I ): I {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE,
                argF, argG, argH)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        try {
            argF?.close()
        } catch (closeException: Exception) {}

        try {
            argG?.close()
        } catch (closeException: Exception) {}

        try {
            argH?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argF?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argG?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argH?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 9 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 9 [Closeable] resources.
 * @return the result of the [block] function invoked on the 9 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?,
        F : Closeable?, G : Closeable?, H : Closeable?, I : Closeable?, J>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, argF: F, argG: G, argH: H,
               argI: I, block: (A, B, C, D, E, F, G, H, I)->J ): J {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE,
                argF, argG, argH, argI)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        try {
            argF?.close()
        } catch (closeException: Exception) {}

        try {
            argG?.close()
        } catch (closeException: Exception) {}

        try {
            argH?.close()
        } catch (closeException: Exception) {}

        try {
            argI?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argF?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argG?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argH?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argI?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 10 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 10 [Closeable] resources.
 * @return the result of the [block] function invoked on the 10 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?,
        F : Closeable?, G : Closeable?, H : Closeable?, I : Closeable?, J : Closeable?, K>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, argF: F, argG: G, argH: H,
               argI: I, argJ: J, block: (A, B, C, D, E, F, G, H, I, J)->K ): K {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE,
                argF, argG, argH, argI, argJ)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        try {
            argF?.close()
        } catch (closeException: Exception) {}

        try {
            argG?.close()
        } catch (closeException: Exception) {}

        try {
            argH?.close()
        } catch (closeException: Exception) {}

        try {
            argI?.close()
        } catch (closeException: Exception) {}

        try {
            argJ?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argF?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argG?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argH?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argI?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argJ?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 11 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 11 [Closeable] resources.
 * @return the result of the [block] function invoked on the 11 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?,
        F : Closeable?, G : Closeable?, H : Closeable?, I : Closeable?, J : Closeable?,
        K : Closeable?, L>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, argF: F, argG: G, argH: H,
               argI: I, argJ: J, argK: K, block: (A, B, C, D, E, F, G, H, I, J, K)->L ): L {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE,
                argF, argG, argH, argI, argJ, argK)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        try {
            argF?.close()
        } catch (closeException: Exception) {}

        try {
            argG?.close()
        } catch (closeException: Exception) {}

        try {
            argH?.close()
        } catch (closeException: Exception) {}

        try {
            argI?.close()
        } catch (closeException: Exception) {}

        try {
            argJ?.close()
        } catch (closeException: Exception) {}

        try {
            argK?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argF?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argG?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argH?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argI?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argJ?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argK?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 12 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 12 [Closeable] resources.
 * @return the result of the [block] function invoked on the 12 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?,
        F : Closeable?, G : Closeable?, H : Closeable?, I : Closeable?, J : Closeable?,
        K : Closeable?, L : Closeable?, M>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, argF: F, argG: G, argH: H,
               argI: I, argJ: J, argK: K, argL: L,
               block: (A, B, C, D, E, F, G, H, I, J, K, L)->M ): M {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE,
                argF, argG, argH, argI, argJ,
                argK, argL)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        try {
            argF?.close()
        } catch (closeException: Exception) {}

        try {
            argG?.close()
        } catch (closeException: Exception) {}

        try {
            argH?.close()
        } catch (closeException: Exception) {}

        try {
            argI?.close()
        } catch (closeException: Exception) {}

        try {
            argJ?.close()
        } catch (closeException: Exception) {}

        try {
            argK?.close()
        } catch (closeException: Exception) {}

        try {
            argL?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argF?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argG?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argH?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argI?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argJ?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argK?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argL?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 13 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 13 [Closeable] resources.
 * @return the result of the [block] function invoked on the 13 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?,
        F : Closeable?, G : Closeable?, H : Closeable?, I : Closeable?, J : Closeable?,
        K : Closeable?, L : Closeable?, M : Closeable?, N>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, argF: F, argG: G, argH: H,
               argI: I, argJ: J, argK: K, argL: L,
               argM: M, block: (A, B, C, D, E, F, G, H, I, J, K, L, M)->N ): N {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE,
                argF, argG, argH, argI, argJ,
                argK, argL, argM)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        try {
            argF?.close()
        } catch (closeException: Exception) {}

        try {
            argG?.close()
        } catch (closeException: Exception) {}

        try {
            argH?.close()
        } catch (closeException: Exception) {}

        try {
            argI?.close()
        } catch (closeException: Exception) {}

        try {
            argJ?.close()
        } catch (closeException: Exception) {}

        try {
            argK?.close()
        } catch (closeException: Exception) {}

        try {
            argL?.close()
        } catch (closeException: Exception) {}

        try {
            argM?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argF?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argG?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argH?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argI?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argJ?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argK?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argL?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argM?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 14 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 14 [Closeable] resources.
 * @return the result of the [block] function invoked on the 14 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?,
        F : Closeable?, G : Closeable?, H : Closeable?, I : Closeable?, J : Closeable?,
        K : Closeable?, L : Closeable?, M : Closeable?, N : Closeable?, O>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, argF: F, argG: G, argH: H,
               argI: I, argJ: J, argK: K, argL: L,
               argM: M, argN: N, block: (A, B, C, D, E, F, G, H, I, J, K, L, M, N)->O ): O {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE,
                argF, argG, argH, argI, argJ,
                argK, argL, argM, argN)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        try {
            argF?.close()
        } catch (closeException: Exception) {}

        try {
            argG?.close()
        } catch (closeException: Exception) {}

        try {
            argH?.close()
        } catch (closeException: Exception) {}

        try {
            argI?.close()
        } catch (closeException: Exception) {}

        try {
            argJ?.close()
        } catch (closeException: Exception) {}

        try {
            argK?.close()
        } catch (closeException: Exception) {}

        try {
            argL?.close()
        } catch (closeException: Exception) {}

        try {
            argM?.close()
        } catch (closeException: Exception) {}

        try {
            argN?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argF?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argG?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argH?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argI?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argJ?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argK?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argL?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argM?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argN?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 15 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 15 [Closeable] resources.
 * @return the result of the [block] function invoked on the 15 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?,
        F : Closeable?, G : Closeable?, H : Closeable?, I : Closeable?, J : Closeable?,
        K : Closeable?, L : Closeable?, M : Closeable?, N : Closeable?, O : Closeable?, P>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, argF: F, argG: G, argH: H,
               argI: I, argJ: J, argK: K, argL: L,
               argM: M, argN: N, argO: O, block: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O)->P ): P {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE,
                argF, argG, argH, argI, argJ,
                argK, argL, argM, argN, argO)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        try {
            argF?.close()
        } catch (closeException: Exception) {}

        try {
            argG?.close()
        } catch (closeException: Exception) {}

        try {
            argH?.close()
        } catch (closeException: Exception) {}

        try {
            argI?.close()
        } catch (closeException: Exception) {}

        try {
            argJ?.close()
        } catch (closeException: Exception) {}

        try {
            argK?.close()
        } catch (closeException: Exception) {}

        try {
            argL?.close()
        } catch (closeException: Exception) {}

        try {
            argM?.close()
        } catch (closeException: Exception) {}

        try {
            argN?.close()
        } catch (closeException: Exception) {}

        try {
            argO?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argF?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argG?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argH?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argI?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argJ?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argK?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argL?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argM?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argN?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argO?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 16 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 16 [Closeable] resources.
 * @return the result of the [block] function invoked on the 16 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?,
        F : Closeable?, G : Closeable?, H : Closeable?, I : Closeable?, J : Closeable?,
        K : Closeable?, L : Closeable?, M : Closeable?, N : Closeable?, O : Closeable?,
        P : Closeable?, Q>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, argF: F, argG: G, argH: H,
               argI: I, argJ: J, argK: K, argL: L,
               argM: M, argN: N, argO: O, argP: P,
               block: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P)->Q ): Q {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE,
                argF, argG, argH, argI, argJ,
                argK, argL, argM, argN, argO, argP)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        try {
            argF?.close()
        } catch (closeException: Exception) {}

        try {
            argG?.close()
        } catch (closeException: Exception) {}

        try {
            argH?.close()
        } catch (closeException: Exception) {}

        try {
            argI?.close()
        } catch (closeException: Exception) {}

        try {
            argJ?.close()
        } catch (closeException: Exception) {}

        try {
            argK?.close()
        } catch (closeException: Exception) {}

        try {
            argL?.close()
        } catch (closeException: Exception) {}

        try {
            argM?.close()
        } catch (closeException: Exception) {}

        try {
            argN?.close()
        } catch (closeException: Exception) {}

        try {
            argO?.close()
        } catch (closeException: Exception) {}

        try {
            argP?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argF?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argG?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argH?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argI?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argJ?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argK?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argL?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argM?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argN?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argO?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argP?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 17 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 17 [Closeable] resources.
 * @return the result of the [block] function invoked on the 17 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?,
        F : Closeable?, G : Closeable?, H : Closeable?, I : Closeable?, J : Closeable?,
        K : Closeable?, L : Closeable?, M : Closeable?, N : Closeable?, O : Closeable?,
        P : Closeable?, Q : Closeable?, R>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, argF: F, argG: G, argH: H,
               argI: I, argJ: J, argK: K, argL: L,
               argM: M, argN: N, argO: O, argP: P,
               argQ: Q, block: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q)->R ): R {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE,
                argF, argG, argH, argI, argJ,
                argK, argL, argM, argN, argO,
                argP, argQ)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        try {
            argF?.close()
        } catch (closeException: Exception) {}

        try {
            argG?.close()
        } catch (closeException: Exception) {}

        try {
            argH?.close()
        } catch (closeException: Exception) {}

        try {
            argI?.close()
        } catch (closeException: Exception) {}

        try {
            argJ?.close()
        } catch (closeException: Exception) {}

        try {
            argK?.close()
        } catch (closeException: Exception) {}

        try {
            argL?.close()
        } catch (closeException: Exception) {}

        try {
            argM?.close()
        } catch (closeException: Exception) {}

        try {
            argN?.close()
        } catch (closeException: Exception) {}

        try {
            argO?.close()
        } catch (closeException: Exception) {}

        try {
            argP?.close()
        } catch (closeException: Exception) {}

        try {
            argQ?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argF?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argG?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argH?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argI?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argJ?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argK?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argL?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argM?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argN?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argO?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argP?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argQ?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 18 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 18 [Closeable] resources.
 * @return the result of the [block] function invoked on the 18 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?,
        F : Closeable?, G : Closeable?, H : Closeable?, I : Closeable?, J : Closeable?,
        K : Closeable?, L : Closeable?, M : Closeable?, N : Closeable?, O : Closeable?,
        P : Closeable?, Q : Closeable?, R : Closeable?, S>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, argF: F, argG: G, argH: H,
               argI: I, argJ: J, argK: K, argL: L,
               argM: M, argN: N, argO: O, argP: P,
               argQ: Q, argR: R, block: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R)->S ): S {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE,
                argF, argG, argH, argI, argJ,
                argK, argL, argM, argN, argO,
                argP, argQ, argR)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        try {
            argF?.close()
        } catch (closeException: Exception) {}

        try {
            argG?.close()
        } catch (closeException: Exception) {}

        try {
            argH?.close()
        } catch (closeException: Exception) {}

        try {
            argI?.close()
        } catch (closeException: Exception) {}

        try {
            argJ?.close()
        } catch (closeException: Exception) {}

        try {
            argK?.close()
        } catch (closeException: Exception) {}

        try {
            argL?.close()
        } catch (closeException: Exception) {}

        try {
            argM?.close()
        } catch (closeException: Exception) {}

        try {
            argN?.close()
        } catch (closeException: Exception) {}

        try {
            argO?.close()
        } catch (closeException: Exception) {}

        try {
            argP?.close()
        } catch (closeException: Exception) {}

        try {
            argQ?.close()
        } catch (closeException: Exception) {}

        try {
            argR?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argF?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argG?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argH?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argI?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argJ?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argK?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argL?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argM?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argN?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argO?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argP?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argQ?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argR?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 19 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 19 [Closeable] resources.
 * @return the result of the [block] function invoked on the 19 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?,
        F : Closeable?, G : Closeable?, H : Closeable?, I : Closeable?, J : Closeable?,
        K : Closeable?, L : Closeable?, M : Closeable?, N : Closeable?, O : Closeable?,
        P : Closeable?, Q : Closeable?, R : Closeable?, S : Closeable?, T>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, argF: F, argG: G, argH: H,
               argI: I, argJ: J, argK: K, argL: L,
               argM: M, argN: N, argO: O, argP: P,
               argQ: Q, argR: R, argS: S, block: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S)->T ): T {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE,
                argF, argG, argH, argI, argJ,
                argK, argL, argM, argN, argO,
                argP, argQ, argR, argS)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        try {
            argF?.close()
        } catch (closeException: Exception) {}

        try {
            argG?.close()
        } catch (closeException: Exception) {}

        try {
            argH?.close()
        } catch (closeException: Exception) {}

        try {
            argI?.close()
        } catch (closeException: Exception) {}

        try {
            argJ?.close()
        } catch (closeException: Exception) {}

        try {
            argK?.close()
        } catch (closeException: Exception) {}

        try {
            argL?.close()
        } catch (closeException: Exception) {}

        try {
            argM?.close()
        } catch (closeException: Exception) {}

        try {
            argN?.close()
        } catch (closeException: Exception) {}

        try {
            argO?.close()
        } catch (closeException: Exception) {}

        try {
            argP?.close()
        } catch (closeException: Exception) {}

        try {
            argQ?.close()
        } catch (closeException: Exception) {}

        try {
            argR?.close()
        } catch (closeException: Exception) {}

        try {
            argS?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argF?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argG?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argH?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argI?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argJ?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argK?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argL?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argM?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argN?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argO?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argP?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argQ?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argR?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argS?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}

/**
 * Executes the given [block] function on the given 20 resources and closes them down correctly afterwards whether an exception
 * is thrown or not.
 *
 * @param block is a function to process the 20 [Closeable] resources.
 * @return the result of the [block] function invoked on the 20 resources.
 */
@InlineOnly
public inline fun <A : Closeable?, B : Closeable?, C : Closeable?, D : Closeable?, E : Closeable?,
        F : Closeable?, G : Closeable?, H : Closeable?, I : Closeable?, J : Closeable?,
        K : Closeable?, L : Closeable?, M : Closeable?, N : Closeable?, O : Closeable?,
        P : Closeable?, Q : Closeable?, R : Closeable?, S : Closeable?, T : Closeable?, U>
        using( argA: A, argB: B, argC: C, argD: D,
               argE: E, argF: F, argG: G, argH: H,
               argI: I, argJ: J, argK: K, argL: L,
               argM: M, argN: N, argO: O, argP: P,
               argQ: Q, argR: R, argS: S, argT: T,
               block: (A, B, C, D, E, F, G, H, I, J, K, L, M, N, O, P, Q, R, S, T)->U ): U {
    var closed = false
    try {
        return block(argA, argB, argC, argD, argE,
                argF, argG, argH, argI, argJ,
                argK, argL, argM, argN, argO,
                argP, argQ, argR, argS, argT)
    } catch (e: Exception) {
        closed = true

        try {
            argA?.close()
        } catch (closeException: Exception) {}

        try {
            argB?.close()
        } catch (closeException: Exception) {}

        try {
            argC?.close()
        } catch (closeException: Exception) {}

        try {
            argD?.close()
        } catch (closeException: Exception) {}

        try {
            argE?.close()
        } catch (closeException: Exception) {}

        try {
            argF?.close()
        } catch (closeException: Exception) {}

        try {
            argG?.close()
        } catch (closeException: Exception) {}

        try {
            argH?.close()
        } catch (closeException: Exception) {}

        try {
            argI?.close()
        } catch (closeException: Exception) {}

        try {
            argJ?.close()
        } catch (closeException: Exception) {}

        try {
            argK?.close()
        } catch (closeException: Exception) {}

        try {
            argL?.close()
        } catch (closeException: Exception) {}

        try {
            argM?.close()
        } catch (closeException: Exception) {}

        try {
            argN?.close()
        } catch (closeException: Exception) {}

        try {
            argO?.close()
        } catch (closeException: Exception) {}

        try {
            argP?.close()
        } catch (closeException: Exception) {}

        try {
            argQ?.close()
        } catch (closeException: Exception) {}

        try {
            argR?.close()
        } catch (closeException: Exception) {}

        try {
            argS?.close()
        } catch (closeException: Exception) {}

        try {
            argT?.close()
        } catch (closeException: Exception) {}

        throw e
    } finally {
        if (!closed) {
            val exceptionCollection = mutableListOf<Exception>()

            try {
                argA?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argB?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argC?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argD?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argE?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argF?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argG?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argH?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argI?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argJ?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argK?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argL?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argM?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argN?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argO?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argP?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argQ?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argR?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argS?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            try {
                argT?.close()
            } catch (closeException: Exception) {
                exceptionCollection.add(closeException)
            }

            if(exceptionCollection.isNotEmpty()){
                throw MultipleCloseExceptionsException(exceptionCollection)
            }
        }
    }
}


/**
 * An exception to collect all suppressed exceptions from the final in using.
 */
@PublishedApi
internal class MultipleCloseExceptionsException(causes: Iterable<Throwable>) : RuntimeException() {
    init {
        causes.forEach {
            addSuppressed(it)
        }
    }
}