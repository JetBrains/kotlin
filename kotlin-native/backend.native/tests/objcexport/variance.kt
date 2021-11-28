/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package variance

sealed class InvariantSuper<T>
class Invariant<T> : InvariantSuper<T>()

sealed class OutVariantSuper<out T>
class OutVariant<out T> : OutVariantSuper<T>()

sealed class InVariantSuper<in T>
class InVariant<in T> : InVariantSuper<T>()
