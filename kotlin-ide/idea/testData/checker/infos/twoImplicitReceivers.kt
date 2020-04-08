interface IA
interface IB

object A : IA {
    fun B.foo() {  }
}

object B : IB

<info descr="null">public</info> <info descr="null">inline</info> fun <T, R> with(receiver: T, block: T.() -> R): R = receiver.block()

fun test(a: IA, b: IB) {
    with(a) lambda1@{
        with(b) lambda2@{
            if (this@lambda1 is A && this@lambda2 is B) {
                <info descr="Extension implicit receiver smart cast to A"><info descr="Extension implicit receiver smart cast to B">foo</info></info>()
            }
        }
    }
}
