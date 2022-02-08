val a = mutableListOf<Int>()
val b = 1.also { a += 2 }
val c = a.single()