// KT-34027
expect interface <!LINE_MARKER("descr='Has actuals in JS'")!>A<!><T> {
    fun <!LINE_MARKER("descr='Has actuals in JS'")!>foo<!>(x: T)
}

fun bar(): A<String> = null!!
