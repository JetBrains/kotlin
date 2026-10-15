// LANGUAGE: +IntrinsicConstEvaluation
// WITH_STDLIB

// FILE: J.java
public class J {
    public static final String PADDED = "  OK  ";
    public static final int N = 41;
}

// FILE: test.kt
annotation class Anno(val s: String, val i: Int, val c: Char)

object KConsts {
    const val TRIMMED: String = "  padded  ".trim()
    const val UPPERCASED: String = "mixed".uppercase()
    const val BUMPED: Int = 41.inc()
    const val CODE: Char = Char(65)
    const val SECONDS_PER_DAY: UInt = 60u * 60u * 24u
}

object KFromJava {
    const val TRIMMED: String = J.PADDED.trim()
    const val BUMPED: Int = J.N.inc()
}

@Anno(s = KConsts.TRIMMED, i = KConsts.BUMPED, c = KConsts.CODE)
class UsesNamedConsts

@Anno(s = "  inline  ".trim(), i = 41.inc(), c = Char(65))
class UsesInlineCalls
