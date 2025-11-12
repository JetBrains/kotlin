package hair.logic

public enum class Trilean {
    YES, NO, MAYBE;

    fun isDefinite(): Boolean = this != MAYBE
    fun isPossible(): Boolean = this != NO

    companion object {
        fun definite(value: Boolean) = if (value) YES else NO
    }
}

public object BooleanLogic {
    fun <T> reflexive(cond: (T, T) -> Boolean): (T, T) -> Boolean = { l, r ->
        if (l == r) true
        else cond(l, r)
    }

    fun <T> simmetrical(cond: (T, T) -> Boolean): (T, T) -> Boolean = { l, r ->
        if (cond(l, r)) true
        else cond(r, l)
    }
}

public object TrileanLogic {
    fun <T> reflexive(cond: (T, T) -> Trilean): (T, T) -> Trilean = { l, r ->
        if (l == r) Trilean.YES
        else cond(l, r)
    }

    fun <T> simmetrical(cond: (T, T) -> Trilean): (T, T) -> Trilean = { l, r ->
        val lr = cond(l, r)
        // FIXME wrong
        if (lr.isDefinite()) lr
        else cond(r, l)
    }
}


