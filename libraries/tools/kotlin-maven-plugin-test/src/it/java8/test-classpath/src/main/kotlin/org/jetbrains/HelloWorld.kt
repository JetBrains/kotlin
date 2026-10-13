package org.jetbrains

import sun.nio.cs.ext.ExtendedCharsets
import sun.net.dns.ResolverConfiguration
import javax.crypto.Cipher
import com.sun.crypto.provider.SunJCE
import sun.nio.ByteBuffered

fun box(): String {
    // NB: In modern JDKs, `Big5` lives in `sun.nio.cs.ext` on macOS,
    // and in `sun.nio.cs` on Linux.
    val a = ExtendedCharsets() // charsets.jar / jdk.charsets
    val c = ResolverConfiguration.open() // java.base module
    val e : Cipher? = null // jce.jar
    val f : SunJCE? = null // sunjce_provider.jar
    val j : ByteBuffered? = null // rt.jar
    return "OK"
}
