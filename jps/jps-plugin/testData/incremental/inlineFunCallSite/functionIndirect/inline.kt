package inline

private inline fun ps(): () -> String { val z = "Outer"; return { "OK" } }

internal inline fun test(s: () -> () -> String = ::ps) =
    s()

val same = test()

