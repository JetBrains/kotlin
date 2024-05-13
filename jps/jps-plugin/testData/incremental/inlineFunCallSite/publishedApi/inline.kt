package inline

inline fun f(): Int {
    val obj = A()
    return PublishedClass().getValue()
        + obj.publishedVal
        + obj.publishedMethod()
        + obj.publishedVar
        + inline.A.publishedConst
}