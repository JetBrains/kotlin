// SET_TRUE: ALLOW_TRAILING_COMMA

@Anno([1])
fun a() = Unit

@Anno([1])
fun a() = Unit

@Anno(
        [
            1,
        ],
)
fun a() = Unit

@Anno(
        [
            1,
        ],
)
fun a() = Unit

@Anno(
        [
            1,
        ],
)
fun a() = Unit

@Anno([1, 2])
fun a() = Unit

@Anno([1, 2])
fun a() = Unit

@Anno(
        [
            1, 2,
        ],
)
fun a() = Unit

@Anno(
        [
            1, 2,
        ],
)
fun a() = Unit

@Anno(
        [
            1, 2,
        ],
)
fun a() = Unit

@Anno([1, 2, 2])
fun a() = Unit

@Anno([1, 2, 2])
fun a() = Unit

@Anno(
        [
            "1",
        ],
)
fun a() = Unit

@Anno(
        [
            1,
        ],
)
fun a() = Unit

@Anno(
        [
            1, 2, 2,
        ],
)
fun a() = Unit

@Anno(
        [
            1,/*
    */
        ],
)
fun a() = Unit

@Anno(
        [
            1, //dw
        ],
)
fun a() = Unit

@Anno(
        [
            1, // ds
        ],
)
fun a() = Unit

@Anno(
        [
/*
    */
            // d
            1,/*
    *//*
    */
        ],
)
fun a() = Unit

@Anno(
        [
            /*
       */
            1,
        ],
)
fun a() = Unit

@Anno(
        [
            1,/*
    */
            2,
        ],
)
fun a() = Unit

@Anno(
        [
            1,
            2,/*
    *//*
    */
        ],
)
fun a() = Unit

@Anno(
        [
/*
    */
            1, 2,
        ],
)
fun a() = Unit

@Anno(
        [
            "1",
        ],
)
fun a() = Unit

@Anno(
        [
            1,
        ],
)
fun a() = Unit

@Anno(
        [
            1,
            2,  /*
    */
        ],
)
fun a() = Unit

@Component(
        modules = [
            AppModule::class, DataModule::class,
            DomainModule::class,
        ],
)
fun b() = Unit