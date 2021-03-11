fun bar(arg: Int): String {
    <caret>if (arg == 1) return "One"
    else if (arg == 2) return "Two"
    if (arg == 0) return "Zero"
    if (arg == -1) return "Minus One"
    else if (arg == -2) return "Minus Two"
    return "Something Complex"
}