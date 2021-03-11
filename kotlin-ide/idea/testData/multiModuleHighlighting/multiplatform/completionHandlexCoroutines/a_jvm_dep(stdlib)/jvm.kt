package foo

private class CancelFutureOnCancel : JobNode()  {
    override fun invoke(cause: Throwable?) {

    }
}

internal actual abstract class CompletionHandlerBase actual constructor() : CompletionHandler {
    actual abstract override fun invoke(cause: Throwable?)
}

fun main() {
    bar(CancelFutureOnCancel())
}
