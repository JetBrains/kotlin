// FIR_COMPARISON
class ResolvedCall<out D>(val candidateDescriptor: D)

fun test(myResolvedCall: ResolvedCall<String>) {
    bar(my<caret>)
}

fun bar(foo: ResolvedCall<*>) {}

// EXIST: myResolvedCall