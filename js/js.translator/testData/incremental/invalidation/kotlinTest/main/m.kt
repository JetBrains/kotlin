fun box(stepId: Int, isWasm: Boolean) = when (stepId) {
        0 -> "OK"
        1 -> checkLog {
            suite("Test1") {
                test("foo") {
                    call("before")
                    call("foo")
                    call("after")
                }
            }
        }
        2 -> checkLog {
            suite("Test1") {
                test("foo") {
                    call("before")
                    call("foo")
                    call("after")
                }
                test("withException") {
                    call("before")
                    call("withException")
                    raised("some exception")
                    call("after")
                    caught("some exception")
                }
            }
        }
        3 -> checkLog {
            suite("Test1") {
                test("foo") {
                    call("before")
                    call("foo")
                    call("after")
                }
                test("withException") {
                    call("before")
                    call("withException")
                    raised("some exception")
                    call("after")
                    caught("some exception")
                }
            }
            suite("Test2") {
                test("foo") {
                    call("before")
                    call("foo")
                }
            }
        }
        else -> "Fail: unexpected step $stepId"
    }
