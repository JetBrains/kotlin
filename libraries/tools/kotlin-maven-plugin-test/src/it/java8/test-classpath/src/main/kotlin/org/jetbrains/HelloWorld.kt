package org.jetbrains

import sun.nio.cs.ext.Big5
import sun.net.dns.ResolverConfiguration
import javax.crypto.Cipher
import com.sun.crypto.provider.SunJCE
import sun.nio.ByteBuffered

fun box(): String {
    val a = Big5() // charsets.jar
    val c = ResolverConfiguration.open() // java.base module
    val e : Cipher? = null // jce.jar
    val f : SunJCE? = null // sunjce_provider.jar
    val j : ByteBuffered? = null // rt.jar
    return "OK"
}