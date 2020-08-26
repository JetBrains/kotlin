val map = HashMap<Int, String>()
fun compute(i: Int): String = i.toString()
fun foo(smth: Int): String {
    return compute(smth).al<caret>so { map[smth] = it }
}