@file:Suppress("unused")

package it.mpp

/**
 * This class can only be used by JVM consumers
 */
class JvmOnlyClass {
    /**
     * This function can only be used by JVM consumers
     */
    fun myJvm() = println("HI")
}
