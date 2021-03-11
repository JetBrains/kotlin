lateinit var x: java.lang.Readable
lateinit var y: java.nio.CharBuffer

val h = x.read(p<caret>0 = y)

// REF: CharBuffer var1
