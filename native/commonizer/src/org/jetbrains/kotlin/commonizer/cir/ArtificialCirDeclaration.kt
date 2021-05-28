/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this delegate code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cir

/**
 * Marker for classifiers that are made up to support commonization.
 * E.g. TypeAliases might create artificial classes for the types they are pointing to.
 */
internal interface ArtificialCirDeclaration : CirDeclaration

internal class ArtificialCirModule(val delegate: CirModule) : CirModule by delegate, ArtificialCirDeclaration

internal fun CirModule.markedArtificial() = ArtificialCirModule(this)

internal class ArtificialCirClass(val delegate: CirClass) : CirClass by delegate, ArtificialCirDeclaration

internal fun CirClass.markedArtificial(): ArtificialCirClass = ArtificialCirClass(this)

internal class ArtificialCirClassConstructor(val delegate: CirClassConstructor) : CirClassConstructor by delegate, ArtificialCirDeclaration

internal fun CirClassConstructor.markedArtificial() = ArtificialCirClassConstructor(this)

internal class ArtificialCirFunction(val delegate: CirFunction) : CirFunction by delegate, ArtificialCirDeclaration

internal fun CirFunction.markedArtificial() = ArtificialCirFunction(this)

internal class ArtificialCirProperty(val delegate: CirProperty) : CirProperty by delegate, ArtificialCirDeclaration

internal fun CirProperty.markedArtificial() = ArtificialCirProperty(this)
