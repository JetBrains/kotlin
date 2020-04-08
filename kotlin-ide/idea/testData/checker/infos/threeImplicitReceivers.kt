interface IA
interface IB
interface IC

object Host : IB

object Prop : IA {
    val Host.foo: Callee <info descr="null">get</info>() = Callee
}

object Callee

object Invoke : IC {
    <info descr="null">operator</info> fun Callee.invoke() { }
}

<info descr="null">public</info> <info descr="null">inline</info> fun <T, R> with(receiver: T, block: T.() -> R): R = receiver.block()

fun test(a: IA, b: IB, c: IC) {
    with(a) lambdaA@{
        with(b) lambdaB@{
            with(c) lambdaC@{
                if (this@lambdaA is Prop && this@lambdaB is Host && this@lambdaC is Invoke) {
                    <info descr="Extension implicit receiver smart cast to Host"><info descr="Extension implicit receiver smart cast to Prop">foo</info></info>()
                }
            }
        }
    }
}
