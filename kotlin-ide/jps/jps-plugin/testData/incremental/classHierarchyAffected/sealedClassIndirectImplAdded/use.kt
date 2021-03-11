fun use(x: Base): Int =
        when (x) {
            is Impl1 -> 1
        }