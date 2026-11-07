internal inline fun g(x: Any) = probe(x)

private inline fun probe(x: Any) = x is Hidden

private class Hidden
