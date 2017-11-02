// EXPECTED_ERROR 'WHI-TE' is an invalid Java enum value name
// EXPECTED_ERROR an enum annotation value must be an enum constant
// EXPECTED_ERROR cannot find symbol

enum class Color {
    BLACK, `WHI-TE`
}

@Anno(Color.`WHI-TE`)
annotation class Anno(val color: Color)