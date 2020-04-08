// "Remove 'val' from parameter" "true"
class Pair<A, B>
{
    operator fun component1(): A = null!!
    operator fun component2(): B = null!!
}

fun f(list: List<Pair<String, String>>) {
    for (val<caret> (x,y) in list) {

    }
}