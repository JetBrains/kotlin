fun main() {
    val x: (
            y: Comparable<Comparable<Number>>,
            z: Iterable<Iterable<Number>> // trailing comma
    ) -> Int = {
        10
    }

    val x: (
            y: Comparable<Comparable<Number>>,
            z: Iterable<Iterable<Number>>
    ) -> Int = {
        10
    }

    val x: (y: Comparable<Comparable<Number>>, z: Iterable<Iterable<Number>>) -> Int = {
        10
    }

    val x: (y: Comparable<Comparable<Number>>, z: Iterable<Iterable<Number>>) -> Int = {
        10
    }

    val x: (
            y: Comparable<Comparable<Number>>, z: Iterable<Iterable<Number>>,
    ) -> Int = {
        10
    }

    val x: (y: Comparable<Comparable<Number>>) -> Int = {
        10
    }

    val x: (y: Comparable<Comparable<Number>>) -> Int = {
        10
    }

    val x: (y: Comparable<Comparable<Number>>
    ) -> Int = {
        10
    }

    val x: (
            y: Comparable<Comparable<Number>>) -> Int = {
        10
    }

    val x: (
            y: Comparable<Comparable<Number>>, //
            z: Iterable<Iterable<Number>> // /**/
    ) -> Int = {
        10
    }

    val x: (y: Comparable<Comparable<Number>>, z: Iterable<Iterable<Number>>
            // wd
    ) -> Int = {
        10
    }

    val x: (
            y: Comparable<Comparable<Number>>,/*
    */
            z: Iterable<Iterable<Number>>, /* //
    */
    ) -> Int = {
        10
    }

    val x: (/**/y: Comparable<Comparable<Number>>/**/) -> Int = {
        10
    }

    val x: (y: Comparable<Comparable<Number>>/**/) -> Int = {
        10
    }

    val x: (y: Comparable<Comparable<Number>>
    ) -> Int = {
        10
    }

    val x: ( /*
        */y: Comparable<Comparable<Number>>) -> Int = {
        10
    }
}

// SET_TRUE: ALLOW_TRAILING_COMMA
