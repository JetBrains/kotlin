package kotlin

/*
 * Returns true if this character is defined in Unicode
 */
public inline fun Char.isDefined(): Boolean = Character.isDefined(this)

/*
 * Returns true if this character is a digit
 */
public inline fun Char.isDigit(): Boolean = Character.isDigit(this)

public inline fun Char.isHighSurrogate(): Boolean = Character.isHighSurrogate(this)

public inline fun Char.isIdentifierIgnorable(): Boolean = Character.isIdentifierIgnorable(this)

public inline fun Char.isISOControl(): Boolean = Character.isISOControl(this)

public inline fun Char.isJavaIdentifierPart(): Boolean = Character.isJavaIdentifierPart(this)

public inline fun Char.isJavaIdentifierStart(): Boolean = Character.isJavaIdentifierStart(this)

public inline fun Char.isJavaLetter(): Boolean = Character.isJavaLetter(this)

public inline fun Char.isJavaLetterOrDigit(): Boolean = Character.isJavaLetterOrDigit(this)

/**
 * Returns true if the character is whitespace
 *
 * @includeFunctionBody ../../test/StringTest.kt count
 */
public inline fun Char.isWhitespace(): Boolean = Character.isWhitespace(this)

/**
 * Returns true if this character is upper case
 */
public inline fun Char.isUpperCase(): Boolean = Character.isUpperCase(this)

/**
 * Returns true if this character is lower case
 */
public inline fun Char.isLowerCase(): Boolean = Character.isLowerCase(this)

/**
 * Returns true if this character is a letter
 */
public inline fun Char.isLetter(): Boolean = Character.isLetter(this)

/**
 * Returns true if this character is a letter or digit
 */
public inline fun Char.isLetterOrDigit(): Boolean = Character.isLetterOrDigit(this)

public inline fun Char.isLowSurrogate(): Boolean = Character.isLowSurrogate(this)

public inline fun Char.isMirrored(): Boolean = Character.isMirrored(this)

public inline fun Char.isSpaceChar(): Boolean = Character.isSpaceChar(this)

public inline fun Char.isTitleCase(): Boolean = Character.isTitleCase(this)

public inline fun Char.isUnicodeIdentifierPart(): Boolean = Character.isUnicodeIdentifierPart(this)

public inline fun Char.isUnicodeIdentifierStart(): Boolean = Character.isUnicodeIdentifierStart(this)
