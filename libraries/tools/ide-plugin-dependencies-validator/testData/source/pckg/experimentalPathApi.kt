@OptIn(ExperimentalPathApi::class)
class Usage {

}

@OptIn(ExperimentalPathApi::class, OtherOptIn::class)
class WithOtherArgument {

}

fun x() {
    @OptIn(ExperimentalPathApi::class)
    call()
}

@OptIn()
fun y() {

}

@ExperimentalPathApi
fun direct() {

}


@kotlin.io.path.ExperimentalPathApi
fun fqName() {

}