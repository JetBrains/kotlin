enum class Color {
    BLACK, `WHI-TE`
}

@Anno(Color.`WHI-TE`)
annotation class Anno(val color: Color)

// EXPECTED_ERROR: (other:-1:-1) 'WHI-TE' is an invalid Java enum value name
