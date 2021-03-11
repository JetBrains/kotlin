import X.combine

class IndentationIssue {
    fun method() {
        X.Y.combine(
                1,
                1
        )
                .let {
                    "zzz"
                }

        X.Y.combine(
                1,
                1
        ).let {
            "zzz"
        }

        X.Y./*
        comment
        */combine(
                1,
                1
        )
                .let {
                    "zzz"
                }

        X.Y./*
        comment
        */combine(
                1,
                1
        ).let {
            "zzz"
        }

        (X.Y.combine(
                1,
                1
        )
                ).let {
                    "zzz"
                }

        X.Y
                .combine(
                        1,
                        1
                )
                .let { "zzz" }

        X.Y
                .combine(
                        1,
                        1
                ).let {
                    "zzz"
                }

        X
                .Y
                .combine(
                        1,
                        1
                )
                .let { "zzz" }

        X
                .Y.combine(
                        1,
                        1
                )
                .let { "zzz" }

        combine(
                1,
                1
        )
                .let { "zzz" }

        combine(
                1,
                2
        ).let {
            "zzz"
        }.let {
            "zzz"
        }

        (combine(
                1,
                2
        ).let {
            "zzz"
        }).let {
            "zzz"
        }

        (combine(
                1,
                2
        )
                .let {
                    "zzz"
                }).let {
                    "zzz"
                }

        combine(
                1,
                2
        ).let {
            "zzz"
        }!!.let {
            "zzz"
        }

        combine(
                1,
                2
        )
                .let {
                    "zzz"
                }!!.let {
                    "zzz"
                }

        (combine(
                1,
                2
        )
                .let {
                    "zzz"
                })!!.let {
                    "zzz"
                }

        ((combine(
                1,
                2
        )
                ).let {
                    "zzz"
                }!!)!!.let {
                    "zzz"
                }

        ((combine(
                1,
                2
        )).let {
            "zzz"
        }!!)!!.let {
            "zzz"
        }

        ((combine(
                1,
                2
        )).let {
            "zzz"
        }!!
                )!!.let {
                    "zzz"
                }
    }
}

object X {
    fun <L, R, T> combine(x: L, y: R): T = TODO()

    val Y get() = this
}