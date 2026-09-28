fun square(x: Int) = x * x

val values = listOf(1, 2, 3).map(::square)

if (values.joinToString(",") != "1,4,9") error("Something went wrong")

println("OK")
