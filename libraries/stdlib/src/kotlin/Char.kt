package kotlin

public inline fun Char.isDefined(): Boolean = Character.isDefined(this)

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

