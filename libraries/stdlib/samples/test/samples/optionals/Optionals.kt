/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package samples.optionals

import samples.*
import java.util.Optional
import kotlin.jvm.optionals.*

class Optionals {

    @Sample
    fun getOrNull() {
        val optional = Optional.of("Hello there")
        assertPrints(optional.getOrNull(), "Hello there")

        val absent = Optional.empty<String>()
        assertPrints(absent.getOrNull(), "null")
    }

    @Sample
    fun getOrDefault() {
        fun parsePortFromConfig(value: String): Optional<Int> = Optional.ofNullable(value.toIntOrNull())

        val port = parsePortFromConfig("8081")
        assertPrints(port.getOrDefault(8080), "8081")

        val otherPort = parsePortFromConfig("invalid")
        assertPrints(otherPort.getOrDefault(8080), "8080")
    }

    @Sample
    fun getOrElse() {
        val user = Optional.of("Vlad Tepes")
        val stranger = Optional.empty<String>()
        val username = user.getOrElse {
            println("This block is never called, optional has some value")
            "Anonymous"
        }
        assertPrints(username, "Vlad Tepes")
        val anonymousUsername = stranger.getOrElse {
            println("Computing anonymous username")
            "Anonymous"
        }
        assertPrints(anonymousUsername, "Anonymous")
    }

    @Sample
    fun toCollection() {
        val maybeItsAnAnimal = Optional.of("Anonymous Capybara")
        val maybeItsAnAnimalAsWell = Optional.empty<String>()
        val animals = arrayListOf("Hilarious Honey Badger")
        // Add to the collection if present
        maybeItsAnAnimal.toCollection(animals)
        maybeItsAnAnimalAsWell.toCollection(animals)

        assertPrints(animals, "[Hilarious Honey Badger, Anonymous Capybara]")
    }

    @Sample
    fun toList() {
        val maybeItsAnAnimal = Optional.of("Anonymous Capybara")
        val animals = maybeItsAnAnimal.toList()
        assertPrints(animals, "[Anonymous Capybara]")

        val maybeItIsNot = Optional.empty<String>()
        assertPrints(maybeItIsNot.toList(), "[]")
    }

    @Sample
    fun toSet() {
        val maybeItsAnAnimal = Optional.of("Anonymous Capybara")
        val animals = maybeItsAnAnimal.toSet()
        assertPrints(animals, "[Anonymous Capybara]")

        val maybeItIsNot = Optional.empty<String>()
        assertPrints(maybeItIsNot.toSet(), "[]")
    }

    @Sample
    fun asSequence() {
        val maybeItsAnAnimal = Optional.of("Anonymous Capybara")
        val animals = maybeItsAnAnimal.asSequence()
        assertPrints(animals.joinToString(), "Anonymous Capybara")

        val maybeItIsNot = Optional.empty<String>()
        assertPrints(maybeItIsNot.asSequence().joinToString(), "")
    }
}
