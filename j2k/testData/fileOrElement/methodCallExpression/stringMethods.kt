// ERROR: Too many arguments for public constructor String() defined in kotlin.String
// ERROR: Too many arguments for public constructor String() defined in kotlin.String
// ERROR: Too many arguments for public constructor String() defined in kotlin.String
// ERROR: Too many arguments for public constructor String() defined in kotlin.String
// ERROR: Too many arguments for public constructor String() defined in kotlin.String
// ERROR: Too many arguments for public constructor String() defined in kotlin.String
import java.nio.charset.Charset
import java.util.*

internal class A {
    @Throws(Exception::class)
    fun constructors() {
        String()
        // TODO: new String("original");
        String(charArrayOf('a', 'b', 'c'))
        String(charArrayOf('b', 'd'), 1, 1)
        String(intArrayOf(32, 65, 127), 0, 3)

        val bytes = byteArrayOf(32, 65, 100, 81)
        val charset = Charset.forName("utf-8")
        String(bytes)
        String(bytes, charset)
        String(bytes, 0, 2)
        String(bytes, "utf-8")
        String(bytes, 0, 2, "utf-8")
        String(bytes, 0, 2, charset)

        String(StringBuilder("content"))
        String(StringBuffer("content"))
    }

    fun normalMethods() {
        val s = "test string"
        s.length
        s.isEmpty()
        s[1]
        s.codePointAt(2)
        s.codePointBefore(2)
        s.codePointCount(0, s.length)
        s.offsetByCodePoints(0, 4)
        s.compareTo("test 2")
        s.contains("seq")
        s.contentEquals(StringBuilder(s))
        s.contentEquals(StringBuffer(s))
        s.endsWith("ng")
        s.startsWith("te")
        s.startsWith("st", 2)
        s.indexOf("st")
        s.indexOf("st", 5)
        s.lastIndexOf("st")
        s.lastIndexOf("st", 4)
        s.indexOf('t')
        s.indexOf('t', 5)
        s.lastIndexOf('t')
        s.lastIndexOf('t', 5)
        s.substring(1)
        s.substring(0, 4)
        s.subSequence(0, 4)
        s.replace('e', 'i')
        s.replace("est", "oast")
        s.intern()
        s.toLowerCase()
        s.toLowerCase(Locale.FRENCH)
        s.toUpperCase()
        s.toUpperCase(Locale.FRENCH)

        s.toString()
        s.toCharArray()
    }

    @Throws(Exception::class)
    fun specialMethods() {
        val s = "test string"
        s == "test"
        s.equals(
                "tesT", ignoreCase = true
        )
        s.compareTo("Test", ignoreCase = true)
        s.regionMatches(
                0,
                "TE",
                0,
                2, ignoreCase = true
        )
        s.regionMatches(0, "st", 1, 2)
        s.matches("\\w+".toRegex())
        s.replace("\\w+".toRegex(), "---")
                .replaceFirst("([s-t])".toRegex(), "A$1")
        useSplit(s.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        useSplit(s.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
        useSplit(s.split("\\s+".toRegex()).toTypedArray())
        useSplit(s.split("\\s+".toRegex(), 2).toTypedArray())
        val limit = 5
        useSplit(s.split("\\s+".toRegex(), limit.coerceAtLeast(0)).toTypedArray())
        s.trim { it <= ' ' }
        s + " another"

        s.toByteArray()
        s.toByteArray(Charset.forName("utf-8"))
        s.toByteArray(charset("utf-8"))

        val chars = CharArray(10)
        s.toCharArray(chars, 0, 1, 11)
    }

    fun staticMethods() {
        1.toString()
        1L.toString()
        'a'.toString()
        true.toString()
        1.11f.toString()
        3.14.toString()
        Any().toString()

        String.format(
                Locale.FRENCH,
                "Je ne mange pas %d jours",
                6
        )
        String.format("Operation completed with %s", "success")

        val chars = charArrayOf('a', 'b', 'c')
        String(chars)
        String(chars, 1, 2)
        String(chars)
        String(chars, 1, 2)

        val order = String.CASE_INSENSITIVE_ORDER
    }

    fun unsupportedMethods() {
        val s = "test string"
        /* TODO:
        s.indexOf(32);
        s.indexOf(32, 2);
        s.lastIndexOf(32);
        s.lastIndexOf(32, 2);
        */
    }

    fun useSplit(result: Array<String>) {
    }
}