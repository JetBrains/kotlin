fun box(): String {
    if (usage(A()) != 42) return "FAIL1"
    if (crossUsage2() != 44) return "FAIL2"
    return "OK"
}