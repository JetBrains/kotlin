package sample

fun test_1() {
    take_C_common_2(C::common_2_C)
    take_C_alias_common_2(C::common_2_C)
    take_C_jvm(C::common_2_C)
    take_C_alias_jvm(C::common_2_C)

    take_C_common_2(C::jvm_C)
    take_C_alias_common_2(C::jvm_C)
    take_C_jvm(C::jvm_C)
    take_C_alias_jvm(C::jvm_C)
}

fun test_1_alias_1() {
    take_C_common_2(C_Common_2_Alias::common_2_C)
    take_C_alias_common_2(C_Common_2_Alias::common_2_C)
    take_C_jvm(C_Common_2_Alias::common_2_C)
    take_C_alias_jvm(C_Common_2_Alias::common_2_C)

    take_C_common_2(C_Common_2_Alias::jvm_C)
    take_C_alias_common_2(C_Common_2_Alias::jvm_C)
    take_C_jvm(C_Common_2_Alias::jvm_C)
    take_C_alias_jvm(C_Common_2_Alias::jvm_C)
}

fun test_1_alias_2() {
    take_C_common_2(C_jvm_Alias::common_2_C)
    take_C_alias_common_2(C_jvm_Alias::common_2_C)
    take_C_jvm(C_jvm_Alias::common_2_C)
    take_C_alias_jvm(C_jvm_Alias::common_2_C)

    take_C_common_2(C_jvm_Alias::jvm_C)
    take_C_alias_common_2(C_jvm_Alias::jvm_C)
    take_C_jvm(C_jvm_Alias::jvm_C)
    take_C_alias_jvm(C_jvm_Alias::jvm_C)
}

fun test_2() {
    take_A_common_1(C::jvm_C)
    take_A_common_2(C::jvm_C)
    take_A_jvm(C::jvm_C)

    take_A_common_1(C::common_2_C)
    take_A_common_2(C::common_2_C)
    take_A_jvm(C::common_2_C)
}

fun test_2_alias_1() {
    take_A_common_1(C_Common_2_Alias::jvm_C)
    take_A_common_2(C_Common_2_Alias::jvm_C)
    take_A_jvm(C_Common_2_Alias::jvm_C)

    take_A_common_1(C_Common_2_Alias::common_2_C)
    take_A_common_2(C_Common_2_Alias::common_2_C)
    take_A_jvm(C_Common_2_Alias::common_2_C)
}

fun test_2_alias_2() {
    take_A_common_1(C_jvm_Alias::jvm_C)
    take_A_common_2(C_jvm_Alias::jvm_C)
    take_A_jvm(C_jvm_Alias::jvm_C)

    take_A_common_1(C_jvm_Alias::common_2_C)
    take_A_common_2(C_jvm_Alias::common_2_C)
    take_A_jvm(C_jvm_Alias::common_2_C)
}

fun test_3() {
    take_B_common_1(B::common_1_B)
    take_B_common_2(B::common_1_B)
    take_B_jvm(B::common_1_B)
    take_B_common_1(B::jvm_B)
    take_B_common_2(B::jvm_B)
    take_B_jvm(B::jvm_B)

    take_B_alias_common_1(B::common_1_B)
    take_B_alias_common_2(B::common_1_B)
    take_B_alias_jvm(B::common_1_B)
    take_B_alias_common_1(B::jvm_B)
    take_B_alias_common_2(B::jvm_B)
    take_B_alias_jvm(B::jvm_B)
}

fun test_3_alias_1() {
    take_B_common_1(B_Common_1_Alias::common_1_B)
    take_B_common_2(B_Common_1_Alias::common_1_B)
    take_B_jvm(B_Common_1_Alias::common_1_B)
    take_B_common_1(B_Common_1_Alias::jvm_B)
    take_B_common_2(B_Common_1_Alias::jvm_B)
    take_B_jvm(B_Common_1_Alias::jvm_B)

    take_B_alias_common_1(B_Common_1_Alias::common_1_B)
    take_B_alias_common_2(B_Common_1_Alias::common_1_B)
    take_B_alias_jvm(B_Common_1_Alias::common_1_B)
    take_B_alias_common_1(B_Common_1_Alias::jvm_B)
    take_B_alias_common_2(B_Common_1_Alias::jvm_B)
    take_B_alias_jvm(B_Common_1_Alias::jvm_B)
}

fun test_3_alias_2() {
    take_B_common_1(B_Common_2_Alias::common_1_B)
    take_B_common_2(B_Common_2_Alias::common_1_B)
    take_B_jvm(B_Common_2_Alias::common_1_B)
    take_B_common_1(B_Common_2_Alias::jvm_B)
    take_B_common_2(B_Common_2_Alias::jvm_B)
    take_B_jvm(B_Common_2_Alias::jvm_B)

    take_B_alias_common_1(B_Common_2_Alias::common_1_B)
    take_B_alias_common_2(B_Common_2_Alias::common_1_B)
    take_B_alias_jvm(B_Common_2_Alias::common_1_B)
    take_B_alias_common_1(B_Common_2_Alias::jvm_B)
    take_B_alias_common_2(B_Common_2_Alias::jvm_B)
    take_B_alias_jvm(B_Common_2_Alias::jvm_B)
}

fun test_3_alias_3() {
    take_B_common_1(B_jvm_Alias::common_1_B)
    take_B_common_2(B_jvm_Alias::common_1_B)
    take_B_jvm(B_jvm_Alias::common_1_B)
    take_B_common_1(B_jvm_Alias::jvm_B)
    take_B_common_2(B_jvm_Alias::jvm_B)
    take_B_jvm(B_jvm_Alias::jvm_B)

    take_B_alias_common_1(B_jvm_Alias::common_1_B)
    take_B_alias_common_2(B_jvm_Alias::common_1_B)
    take_B_alias_jvm(B_jvm_Alias::common_1_B)
    take_B_alias_common_1(B_jvm_Alias::jvm_B)
    take_B_alias_common_2(B_jvm_Alias::jvm_B)
    take_B_alias_jvm(B_jvm_Alias::jvm_B)
}
