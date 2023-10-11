@OptIn(ExperimentalStdlibApi::class)
class Usage {

}

@OptIn(ExperimentalStdlibApi::class, OtherOptIn::class)
class WithOtherArgument {

}

fun x() {
    @OptIn(ExperimentalStdlibApi::class)
    call()
}

@OptIn()
fun empty() {

}

@ExperimentalStdlibApi
fun direct() {

}

@kotlin.io.path.ExperimentalPathApi
fun fqName() {

}
