package kotlin

import kotlin.properties.Delegates

/**
 * Represents the Unicode directionality of a character.
 * Character directionality is used to calculate the
 * visual ordering of text.
 */
public enum class CharDirectionality(public val value: Int) {

    /**
     * Undefined bidirectional character type. Undefined `char`
     * values have undefined directionality in the Unicode specification.
     */
    UNDEFINED: CharDirectionality(Character.DIRECTIONALITY_UNDEFINED.toInt())

    /**
     * Strong bidirectional character type "L" in the Unicode specification.
     */
    LEFT_TO_RIGHT: CharDirectionality(Character.DIRECTIONALITY_LEFT_TO_RIGHT.toInt())

    /**
     * Strong bidirectional character type "R" in the Unicode specification.
     */
    RIGHT_TO_LEFT: CharDirectionality(Character.DIRECTIONALITY_RIGHT_TO_LEFT.toInt())

    /**
     * Strong bidirectional character type "AL" in the Unicode specification.
     */
    RIGHT_TO_LEFT_ARABIC: CharDirectionality(Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC.toInt())

    /**
     * Weak bidirectional character type "EN" in the Unicode specification.
     */
    EUROPEAN_NUMBER: CharDirectionality(Character.DIRECTIONALITY_EUROPEAN_NUMBER.toInt())

    /**
     * Weak bidirectional character type "ES" in the Unicode specification.
     */
    EUROPEAN_NUMBER_SEPARATOR: CharDirectionality(Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR.toInt())

    /**
     * Weak bidirectional character type "ET" in the Unicode specification.
     */
    EUROPEAN_NUMBER_TERMINATOR: CharDirectionality(Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR.toInt())

    /**
     * Weak bidirectional character type "AN" in the Unicode specification.
     */
    ARABIC_NUMBER: CharDirectionality(Character.DIRECTIONALITY_ARABIC_NUMBER.toInt())

    /**
     * Weak bidirectional character type "CS" in the Unicode specification.
     */
    COMMON_NUMBER_SEPARATOR: CharDirectionality(Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR.toInt())

    /**
     * Weak bidirectional character type "NSM" in the Unicode specification.
     */
    NONSPACING_MARK: CharDirectionality(Character.DIRECTIONALITY_NONSPACING_MARK.toInt())

    /**
     * Weak bidirectional character type "BN" in the Unicode specification.
     */
    BOUNDARY_NEUTRAL: CharDirectionality(Character.DIRECTIONALITY_BOUNDARY_NEUTRAL.toInt())

    /**
     * Neutral bidirectional character type "B" in the Unicode specification.
     */
    PARAGRAPH_SEPARATOR: CharDirectionality(Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR.toInt())

    /**
     * Neutral bidirectional character type "S" in the Unicode specification.
     */
    SEGMENT_SEPARATOR: CharDirectionality(Character.DIRECTIONALITY_SEGMENT_SEPARATOR.toInt())

    /**
     * Neutral bidirectional character type "WS" in the Unicode specification.
     */
    WHITESPACE: CharDirectionality(Character.DIRECTIONALITY_WHITESPACE.toInt())

    /**
     * Neutral bidirectional character type "ON" in the Unicode specification.
     */
    OTHER_NEUTRALS: CharDirectionality(Character.DIRECTIONALITY_OTHER_NEUTRALS.toInt())

    /**
     * Strong bidirectional character type "LRE" in the Unicode specification.
     */
    LEFT_TO_RIGHT_EMBEDDING: CharDirectionality(Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING.toInt())

    /**
     * Strong bidirectional character type "LRO" in the Unicode specification.
     */
    LEFT_TO_RIGHT_OVERRIDE: CharDirectionality(Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE.toInt())

    /**
     * Strong bidirectional character type "RLE" in the Unicode specification.
     */
    RIGHT_TO_LEFT_EMBEDDING: CharDirectionality(Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING.toInt())

    /**
     * Strong bidirectional character type "RLO" in the Unicode specification.
     */
    RIGHT_TO_LEFT_OVERRIDE: CharDirectionality(Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE.toInt())

    /**
     * Weak bidirectional character type "PDF" in the Unicode specification.
     */
    POP_DIRECTIONAL_FORMAT: CharDirectionality(Character.DIRECTIONALITY_POP_DIRECTIONAL_FORMAT.toInt())


    public companion object {
        private val directionalityMap by Delegates.lazy { CharDirectionality.values().toMap { it.value } }

        public fun valueOf(directionality: Int): CharDirectionality = directionalityMap[directionality] ?: throw IllegalArgumentException("Directionality #$directionality is not defined.")
    }
}
