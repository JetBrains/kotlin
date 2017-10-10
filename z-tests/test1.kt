fun main(args: Array<String>) {
    val a: Int = 1
    val k: Int = 2
    val e = 3

    when (Pair(g + k + d, a + b + c + e)) {
        match (5, 7) -> {

        }
        match (m, #a) -> {

        }
        match (n, #(k + e)) -> {

        }
        match p @ Pair(:Int, :Int) -> {

        }
        match (a, b) if (a > b) -> {

        }
        match ("some string ${e} with parameter", _) -> {

        }
        match x -> {

        }
    }
}
