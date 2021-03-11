fun test(bean: Bean) {
    bean.prop = "a"
    println(bean.prop)
    bean./*rename*/prop += "a"
}