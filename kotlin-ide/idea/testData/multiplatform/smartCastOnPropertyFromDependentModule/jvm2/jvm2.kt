fun test(c1: CommonDataClass1, c2: CommonDataClass2, c3: CommonDataClass3, c4: CommonDataClass4, jvm: JvmDataClass) {
    if (c1.property != null) {
        <!DEBUG_INFO_SMARTCAST!>c1.property<!>.doSomething()
    }

    if (c2.property != null) {
        <!DEBUG_INFO_SMARTCAST!>c2.property<!>.doSomething()
    }

    if (c3.property != null) {
        <!DEBUG_INFO_SMARTCAST!>c3.property<!>.doSomething()
    }

    if (c4.property != null) {
        <!DEBUG_INFO_SMARTCAST!>c4.property<!>.doSomething()
    }

    if (jvm.property != null) {
        <!SMARTCAST_IMPOSSIBLE!>jvm.property<!>.doSomething()
    }
}