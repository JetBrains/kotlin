// Do not run K1 KAPT
// LANGUAGE_VERSION: LATEST_STABLE
// ALLOW_DANGEROUS_LANGUAGE_VERSION_TESTING

// FILE: Rock.kt
package kaptk2.b451982470

// The build would crash without the following import statement.
// import kaptk2.b451982470.lib.Piedra

class Rock(piedra: Piedra): Paper(piedra), Scissors

abstract class Paper(final override val piedra: Piedra): Scissors

interface Scissors {
    val piedra: Piedra
}

// FILE: lib/Piedra.kt

package kaptk2.b451982470.lib

interface Piedra
