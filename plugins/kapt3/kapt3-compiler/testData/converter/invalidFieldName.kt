// EXPECTED_ERROR(-1;-1) 'WHI-TE' is an invalid Java enum value name
// EXPECTED_ERROR(6;20) cannot find symbol

enum class Color {
    BLACK, `WHI-TE`
}

@Anno(Color.`WHI-TE`)
annotation class Anno(val color: Color)