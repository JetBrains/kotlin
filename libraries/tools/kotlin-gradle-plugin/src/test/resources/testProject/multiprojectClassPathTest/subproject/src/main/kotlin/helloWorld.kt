package demo

import sun.nio.cs.SingleByte
import sun.net.spi.nameservice.dns.DNSNameService
import javax.crypto.Cipher
import com.sun.java.browser.plugin2.DOM
import com.sun.crypto.provider.AESCipher

fun box(): String {
    val a = SingleByte() // charsets.jar
    val c = DNSNameService() // dnsns.ajr
    val e : Cipher? = null // jce.jar
    val f : AESCipher? = null // sunjce_provider.jar
    val j : DOM? = null // plugin.jar
    return "OK"
}


