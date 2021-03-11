package aliases

fun test_1_1(a: A) {
    a.commonFun()
    a.platformFun()
}

fun test_1_2(a: A1) {
    a.commonFun()
    a.platformFun()
}

fun test_1_3(a: A2) {
    a.commonFun()
    a.platformFun()
}

fun test_1_4(a: A3) {
    a.commonFun()
    a.platformFun()
}

fun test_1_5(x: CommonInv<A>) {
    x.value.commonFun()
    x.value.platformFun()
}

fun test_1_6(x: CommonInv<A1>) {
    x.value.commonFun()
    x.value.platformFun()
}

fun test_1_7(x: CommonInv<A2>) {
    x.value.commonFun()
    x.value.platformFun()
}

fun test_1_8(x: CommonInv<A3>) {
    x.value.commonFun()
    x.value.platformFun()
}

fun test_1_9(x: PlatformInv<A>) {
    x.value.commonFun()
    x.value.platformFun()
}

fun test_1_10(x: PlatformInv<A1>) {
    x.value.commonFun()
    x.value.platformFun()
}

fun test_1_11(x: PlatformInv<A2>) {
    x.value.commonFun()
    x.value.platformFun()
}

fun test_1_12(x: PlatformInv<A3>) {
    x.value.commonFun()
    x.value.platformFun()
}

///////////////////////////////////////////////////////////////////////

fun test_2_1(a: B) {
    a.commonFun()
    a.platformFun()
}

fun test_2_2(a: B1) {
    a.commonFun()
    a.platformFun()
}

fun test_2_3(a: B2) {
    a.commonFun()
    a.platformFun()
}

fun test_2_4(a: B3) {
    a.commonFun()
    a.platformFun()
}

fun test_2_5(x: CommonInv<B>) {
    x.value.commonFun()
    x.value.platformFun()
}

fun test_2_6(x: CommonInv<B1>) {
    x.value.commonFun()
    x.value.platformFun()
}

fun test_2_7(x: CommonInv<B2>) {
    x.value.commonFun()
    x.value.platformFun()
}

fun test_2_8(x: CommonInv<B3>) {
    x.value.commonFun()
    x.value.platformFun()
}

fun test_2_9(x: PlatformInv<B>) {
    x.value.commonFun()
    x.value.platformFun()
}

fun test_2_10(x: PlatformInv<B1>) {
    x.value.commonFun()
    x.value.platformFun()
}

fun test_2_11(x: PlatformInv<B2>) {
    x.value.commonFun()
    x.value.platformFun()
}

fun test_2_12(x: PlatformInv<B3>) {
    x.value.commonFun()
    x.value.platformFun()
}