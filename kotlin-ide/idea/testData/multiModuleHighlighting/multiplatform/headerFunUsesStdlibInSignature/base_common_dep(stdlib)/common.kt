package test

expect fun stringBuilder(sb: StringBuilder)

expect fun kotlinVersion(kv: KotlinVersion)

expect fun regex(r: Regex)

expect fun pair(p: Pair<String, List<Double>>)

expect fun <A, B> genericPair(p: Pair<A, B>)
