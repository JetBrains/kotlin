package test

enum class My { ALPHA, BETA, OMEGA }

annotation class ann(vararg val m: My)

@ann(My.ALPHA, My.BETA) annotation class annotated
