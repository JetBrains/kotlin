/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.config

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent.Factory
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.lombok.config.LombokConfig
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Accessors
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.AllArgsConstructor
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Builder
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Data
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Getter
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.NoArgsConstructor
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.RequiredArgsConstructor
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Setter
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Singular
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.Value
import org.jetbrains.kotlin.lombok.k2.config.ConeLombokAnnotations.With
import java.io.File

@OptIn(SymbolInternals::class)
class LombokService(session: FirSession, configFile: File?) : FirExtensionSessionComponent(session) {
    companion object {
        fun getFactory(configFile: File?): Factory {
            return Factory { LombokService(it, configFile) }
        }
    }

    val config = configFile?.let(LombokConfig::parse) ?: LombokConfig.Empty
    private val cachesFactory = session.firCachesFactory

    private val accessorsCache: Cache<Accessors> = cachesFactory.createCache { symbol ->
        Accessors.get(symbol.fir, config, session)
    }

    private val accessorsIfAnnotatedCache: Cache<Accessors?> = cachesFactory.createCache { symbol ->
        Accessors.getIfAnnotated(symbol.fir, config, session)
    }

    private val getterCache: Cache<Getter?> = cachesFactory.createCache { symbol ->
        Getter.getOrNull(symbol.fir, session)
    }

    private val setterCache: Cache<Setter?> = cachesFactory.createCache { symbol ->
        Setter.getOrNull(symbol.fir, session)
    }

    private val withCache: Cache<With?> = cachesFactory.createCache { symbol ->
        With.getOrNull(symbol.fir, session)
    }

    private val noArgsConstructorCache: Cache<NoArgsConstructor?> = cachesFactory.createCache { symbol ->
        NoArgsConstructor.getOrNull(symbol.fir, session)
    }

    private val allArgsConstructorCache: Cache<AllArgsConstructor?> = cachesFactory.createCache { symbol ->
        AllArgsConstructor.getOrNull(symbol.fir, session)
    }

    private val requiredArgsConstructorCache: Cache<RequiredArgsConstructor?> = cachesFactory.createCache { symbol ->
        RequiredArgsConstructor.getOrNull(symbol.fir, session)
    }

    private val dataCache: Cache<Data?> = cachesFactory.createCache { symbol ->
        Data.getOrNull(symbol.fir, session)
    }

    private val valueCache: Cache<Value?> = cachesFactory.createCache { symbol ->
        Value.getOrNull(symbol.fir, session)
    }

    private val builderCache: Cache<Builder?> = cachesFactory.createCache { symbol ->
        Builder.getIfAnnotated(symbol.fir, config, session)
    }

    private val singularCache: Cache<Singular?> = cachesFactory.createCache { symbol ->
        Singular.getOrNull(symbol.fir, session)
    }

    fun getAccessors(symbol: FirBasedSymbol<*>): Accessors = accessorsCache.getValue(symbol)
    fun getAccessorsIfAnnotated(symbol: FirBasedSymbol<*>): Accessors? = accessorsIfAnnotatedCache.getValue(symbol)
    fun getGetter(symbol: FirBasedSymbol<*>): Getter? = getterCache.getValue(symbol)
    fun getSetter(symbol: FirBasedSymbol<*>): Setter? = setterCache.getValue(symbol)
    fun getWith(symbol: FirBasedSymbol<*>): With? = withCache.getValue(symbol)
    fun getNoArgsConstructor(symbol: FirBasedSymbol<*>): NoArgsConstructor? = noArgsConstructorCache.getValue(symbol)
    fun getAllArgsConstructor(symbol: FirBasedSymbol<*>): AllArgsConstructor? = allArgsConstructorCache.getValue(symbol)
    fun getRequiredArgsConstructor(symbol: FirBasedSymbol<*>): RequiredArgsConstructor? = requiredArgsConstructorCache.getValue(symbol)
    fun getData(symbol: FirBasedSymbol<*>): Data? = dataCache.getValue(symbol)
    fun getValue(symbol: FirBasedSymbol<*>): Value? = valueCache.getValue(symbol)
    fun getBuilder(symbol: FirBasedSymbol<*>): Builder? = builderCache.getValue(symbol)
    fun getSingular(symbol: FirBasedSymbol<*>): Singular? = singularCache.getValue(symbol)
}

private typealias Cache<T> = FirCache<FirBasedSymbol<*>, T, Nothing?>

val FirSession.lombokService: LombokService by FirSession.sessionComponentAccessor()
