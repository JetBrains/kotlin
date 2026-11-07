internal inline fun h() = h2()

private inline fun h2() = C().x

private class C(val x: Int = 1)
