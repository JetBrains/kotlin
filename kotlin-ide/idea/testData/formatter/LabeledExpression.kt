fun test1() {
    loop@    while(true) {

    }
}

fun test2() {
    listOf(1).forEach lit@    {
        if (it == 0) return@lit
        print(it)
    }
}

fun test3() {
    l1@


    /*comment*/ {

    }

    l2@    /*comment*/ {

    }
}

fun test4() {
    l1@


    //eol comment
    {

    }

    l2@    //eol comment
    {

    }
}

fun test5() {
    L1@   val x: Int = 42
}