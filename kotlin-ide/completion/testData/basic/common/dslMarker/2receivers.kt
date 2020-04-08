@DslMarker
annotation class SimpleDsl

class DslRoot {

}

interface DslContainer

@SimpleDsl
class DslContainerOne : DslContainer {
    fun DslContainer.one() {
        println(this)
    }
}

@SimpleDsl
class DslContainerTwo : DslContainer {
    fun DslContainer.two() {
        println(this)
    }
}

fun DslRoot.containerOne(body: DslContainerOne.() -> Unit) {
    body(DslContainerOne())
}

fun DslRoot.containerTwo(body: DslContainerTwo.() -> Unit) {
    body(DslContainerTwo())
}

fun dsl(body: DslRoot.() -> Unit) {
    body(DslRoot())
}

fun test() {
    dsl {
        containerOne {
            containerTwo {
                <caret>
            }
        }
    }
}

// EXIST: two
// ABSENT: one
