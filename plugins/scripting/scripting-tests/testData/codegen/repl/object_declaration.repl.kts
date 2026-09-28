// SNIPPET

object Empty

// SNIPPET

val res1 = "Empty" in Empty.toString()

// EXPECTED: res1 == true

// SNIPPET

object Life {
    fun meaning() = 42
}

// SNIPPET

val res2 = Life.meaning()

// EXPECTED: res2 == 42
