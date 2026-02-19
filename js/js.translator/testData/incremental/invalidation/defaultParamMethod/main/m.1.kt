fun box(): String {
//    if (usage(A()) != 42) return "FAIL1" COMMENTING THIS LINE WAS CAUSED A BUG KT-72232 BEFORE FIX
    if (crossUsage2() != 44) return "FAIL2"
    return "OK"
}