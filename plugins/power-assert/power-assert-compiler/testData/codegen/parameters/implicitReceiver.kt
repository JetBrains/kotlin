fun box(): String {
    return "test1: " + test1() +
            "test2: " + test2() +
            "test3: " + test3() +
            "test4: " + test4() +
            "test5: " + "test".test5() +
            "test6: " + "test".test6() +
            "test7: " + "test".test7() +
            "test8: " + "test".test8() +
            Test.box()
}

fun test1() = expectThrowableMessage {
    with("test") {
        assert(length == 5)
    }
}

fun test2() = expectThrowableMessage {
    with("test") {
        assert(this.length == 5)
    }
}

fun test3() = expectThrowableMessage {
    with("test") {
        assert(substring(1, 3) == "TEST")
    }
}

fun test4() = expectThrowableMessage {
    with("test") {
        assert(this.substring(1, 3) == "TEST")
    }
}

fun String.test5() = expectThrowableMessage {
    assert(length == 5)
}

fun String.test6() = expectThrowableMessage {
    assert(this.length == 5)
}

fun String.test7() = expectThrowableMessage {
    assert(substring(1, 3) == "TEST")
}

fun String.test8() = expectThrowableMessage {
    assert(this.substring(1, 3) == "TEST")
}

data object Test {
    fun box(): String {
        return "Test.test9(): " + test9() +
                "Test.test10(): " + test10() +
                "Test.test11(): " + test11() +
                "Test.test12(): " + test12() +
                "Test.test13(): " + test13() +
                "Test.test14(): " + test14() +
                "Test.test15(): " + "test".test15() +
                "Test.test16(): " + "test".test16() +
                "Test.test17(): " + "test".test17() +
                "Test.test18(): " + "test".test18() +
                "Test.test19(): " + "test".test19() +
                "Test.test20(): " + "test".test20()
    }

    fun String.mutate(): String {
        return reversed()
    }

    fun test9() = expectThrowableMessage {
        with("test") {
            assert(length == 5)
        }
    }

    fun test10() = expectThrowableMessage {
        with("test") {
            assert(this.length == 5)
        }
    }

    fun test11() = expectThrowableMessage {
        with("test") {
            assert(substring(1, 3) == "TEST")
        }
    }

    fun test12() = expectThrowableMessage {
        with("test") {
            assert(this.substring(1, 3) == "TEST")
        }
    }

    fun test13() = expectThrowableMessage {
        with("test") {
            assert(mutate() == "TEST")
        }
    }

    fun test14() = expectThrowableMessage {
        with("test") {
            assert(this.mutate() == "TEST")
        }
    }

    fun String.test15() = expectThrowableMessage {
        assert(length == 5)
    }

    fun String.test16() = expectThrowableMessage {
        assert(this.length == 5)
    }

    fun String.test17() = expectThrowableMessage {
        assert(substring(1, 3) == "TEST")
    }

    fun String.test18() = expectThrowableMessage {
        assert(this.substring(1, 3) == "TEST")
    }

    fun String.test19() = expectThrowableMessage {
        assert(mutate() == "TEST")
    }

    fun String.test20() = expectThrowableMessage {
        assert(this.mutate() == "TEST")
    }
}
