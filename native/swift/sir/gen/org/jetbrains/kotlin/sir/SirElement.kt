/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// This file was generated automatically. See native/swift/sir/tree-generator/Readme.md.
// DO NOT MODIFY IT MANUALLY.

package org.jetbrains.kotlin.sir

import org.jetbrains.kotlin.sir.visitors.SirTransformer
import org.jetbrains.kotlin.sir.visitors.SirTransformerVoid
import org.jetbrains.kotlin.sir.visitors.SirVisitor
import org.jetbrains.kotlin.sir.visitors.SirVisitorVoid

/**
 * The root interface of the Swift IR tree.
 *
 * Generated from: [org.jetbrains.kotlin.sir.tree.generator.SwiftIrTree.rootElement]
 */
sealed interface SirElement {
    /**
     * Runs the provided [visitor] on the Swift IR subtree with the root at this node.
     *
     * @param visitor The visitor to accept.
     * @param data An arbitrary context to pass to each invocation of [visitor]'s methods.
     * @return The value returned by the topmost `visit*` invocation.
     */
    fun <R, D> accept(visitor: SirVisitor<R, D>, data: D): R

    /**
     * Runs the provided [transformer] on the Swift IR subtree with the root at this node.
     *
     * @param transformer The transformer to use.
     * @param data An arbitrary context to pass to each invocation of [transformer]'s methods.
     * @return The transformed node.
     */
    @Suppress("UNCHECKED_CAST")
    fun <E : SirElement, D> transform(transformer: SirTransformer<D>, data: D): E =
        transformer.transformElement(this, data) as E

    /**
     * Runs the provided [visitor] on the Swift IR subtree with the root at this node.
     *
     * @param visitor The visitor to accept.
     */
    fun accept(visitor: SirVisitorVoid) = accept(visitor, null)

    /**
     * Runs the provided [visitor] on subtrees with roots in this node's children.
     *
     * Basically, calls `accept(visitor, data)` on each child of this node.
     *
     * Does **not** run [visitor] on this node itself.
     *
     * @param visitor The visitor for children to accept.
     * @param data An arbitrary context to pass to each invocation of [visitor]'s methods.
     */
    fun <R, D> acceptChildren(visitor: SirVisitor<R, D>, data: D)

    /**
     * Runs the provided [visitor] on subtrees with roots in this node's children.
     *
     * Basically, calls `accept(visitor)` on each child of this node.
     *
     * Does **not** run [visitor] on this node itself.
     *
     * @param visitor The visitor for children to accept.
     */
    fun acceptChildren(visitor: SirVisitorVoid) = acceptChildren(visitor, null)

    /**
     * Runs the provided [transformer] on the Swift IR subtree with the root at this node.
     *
     * @param transformer The transformer to use.
     * @return The transformed node.
     */
    fun <E : SirElement> transform(transformer: SirTransformerVoid): E =
        transform(transformer, null)

    /**
     * Recursively transforms this node's children *in place* using [transformer].
     *
     * Basically, executes `this.child = this.child.transform(transformer, data)` for each child of this node.
     *
     * Does **not** run [transformer] on this node itself.
     *
     * @param transformer The transformer to use for transforming the children.
     * @param data An arbitrary context to pass to each invocation of [transformer]'s methods.
     */
    fun <D> transformChildren(transformer: SirTransformer<D>, data: D)

    /**
     * Recursively transforms this node's children *in place* using [transformer].
     *
     * Basically, executes `this.child = this.child.transform(transformer)` for each child of this node.
     *
     * Does **not** run [transformer] on this node itself.
     *
     * @param transformer The transformer to use for transforming the children.
     */
    fun transformChildren(transformer: SirTransformerVoid) =
        transformChildren(transformer, null)
}
