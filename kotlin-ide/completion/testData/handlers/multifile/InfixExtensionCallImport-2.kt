package other

infix fun <A, B> A.makePair(that: B): Pair<A, B> = Pair(this, that)
