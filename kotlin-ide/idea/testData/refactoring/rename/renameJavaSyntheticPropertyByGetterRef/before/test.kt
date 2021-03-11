fun test(bean: Bean) {
    bean.prop = "a"
    println(bean./*rename*/prop)
    bean.prop += "a"
}