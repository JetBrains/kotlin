/**
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef HOTRELOAD_PLUGINS_HPP
#define HOTRELOAD_PLUGINS_HPP

#ifdef KONAN_HOT_RELOAD

#include "WeakSymbolFallbackGenerator.hpp"
#include "ObjCSelectorFixupPlugin.hpp"
#include "KotlinSymbolExternalizerPlugin.hpp"
#include "CompactUnwindStripperPlugin.hpp"

using kotlin::hot::orc::plugins::WeakSymbolFallbackGenerator;
using kotlin::hot::orc::plugins::KotlinSymbolExternalizerPlugin;
using kotlin::hot::orc::plugins::CompactUnwindStripperPlugin;

#if defined(__APPLE__)
using kotlin::hot::orc::plugins::ObjCSelectorFixupPlugin;
#endif

#endif // KONAN_HOT_RELOAD
#endif // HOTRELOAD_PLUGINS_HPP
