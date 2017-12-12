@file:JvmVersion
package kotlin.text

import kotlin.*

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
    UNDEFINED(Character.DIRECTIONALITY_UNDEFINED.toInt()),

    /**
     * Strong bidirectional character type "L" in the Unicode specification.
     */
    LEFT_TO_RIGHT(Character.DIRECTIONALITY_LEFT_TO_RIGHT.toInt()),

    /**
     * Strong bidirectional character type "R" in the Unicode specification.
     */
    RIGHT_TO_LEFT(Character.DIRECTIONALITY_RIGHT_TO_LEFT.toInt()),

    /**
     * Strong bidirectional character type "AL" in the Unicode specification.
     */
    RIGHT_TO_LEFT_ARABIC(Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC.toInt()),

    /**
     * Weak bidirectional character type "EN" in the Unicode specification.
     */
    EUROPEAN_NUMBER(Character.DIRECTIONALITY_EUROPEAN_NUMBER.toInt()),

    /**
     * Weak bidirectional character type "ES" in the Unicode specification.
     */
    EUROPEAN_NUMBER_SEPARATOR(Character.DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR.toInt()),

    /**
     * Weak bidirectional character type "ET" in the Unicode specification.
     */
    EUROPEAN_NUMBER_TERMINATOR(Character.DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR.toInt()),

    /**
     * Weak bidirectional character type "AN" in the Unicode specification.
     */
    ARABIC_NUMBER(Character.DIRECTIONALITY_ARABIC_NUMBER.toInt()),

    /**
     * Weak bidirectional character type "CS" in the Unicode specification.
     */
    COMMON_NUMBER_SEPARATOR(Character.DIRECTIONALITY_COMMON_NUMBER_SEPARATOR.toInt()),

    /**
     * Weak bidirectional character type "NSM" in the Unicode specification.
     */
    NONSPACING_MARK(Character.DIRECTIONALITY_NONSPACING_MARK.toInt()),

    /**
     * Weak bidirectional character type "BN" in the Unicode specification.
     */
    BOUNDARY_NEUTRAL(Character.DIRECTIONALITY_BOUNDARY_NEUTRAL.toInt()),

    /**
     * Neutral bidirectional character type "B" in the Unicode specification.
     */
    PARAGRAPH_SEPARATOR(Character.DIRECTIONALITY_PARAGRAPH_SEPARATOR.toInt()),

    /**
     * Neutral bidirectional character type "S" in the Unicode specification.
     */
    SEGMENT_SEPARATOR(Character.DIRECTIONALITY_SEGMENT_SEPARATOR.toInt()),

    /**
     * Neutral bidirectional character type "WS" in the Unicode specification.
     */
    WHITESPACE(Character.DIRECTIONALITY_WHITESPACE.toInt()),

    /**
     * Neutral bidirectional character type "ON" in the Unicode specification.
     */
    OTHER_NEUTRALS(Character.DIRECTIONALITY_OTHER_NEUTRALS.toInt()),

    /**
     * Strong bidirectional character type "LRE" in the Unicode specification.
     */
    LEFT_TO_RIGHT_EMBEDDING(Character.DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING.toInt()),

    /**
     * Strong bidirectional character type "LRO" in the Unicode specification.
     */
    LEFT_TO_RIGHT_OVERRIDE(Character.DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE.toInt()),

    /**
     * Strong bidirectional character type "RLE" in the Unicode specification.
     */
    RIGHT_TO_LEFT_EMBEDDING(Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING.toInt()),

    /**
     * Strong bidirectional character type "RLO" in the Unicode specification.
     */
    RIGHT_TO_LEFT_OVERRIDE(Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE.toInt()),

    /**
     * Weak bidirectional character type "PDF" in the Unicode specification.
     */
    POP_DIRECTIONAL_FORMAT(Character.DIRECTIONALITY_POP_DIRECTIONAL_FORMAT.toInt());


    public companion object {
        private val directionalityMap by lazy { CharDirectionality.values().associateBy { it.value } }

        public fun valueOf(directionality: Int): CharDirectionality = directionalityMap[directionality] ?: throw IllegalArgumentException("Directionality #$directionality is not defined.")
    }
}
