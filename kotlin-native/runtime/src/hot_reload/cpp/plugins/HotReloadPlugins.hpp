/**
 * Copyright 2010-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef HOTRELOAD_PLUGINS_HPP
#define HOTRELOAD_PLUGINS_HPP

#include "PluginsCommon.hpp"
#include "WeakSymbolFallbackGenerator.hpp"
#include "ObjCSelectorFixupPlugin.hpp"
#include "KotlinSymbolExternalizerPlugin.hpp"
#include "MachOHostDataSymbolGenerator.hpp"

using orc::plugins::WeakSymbolFallbackGenerator;
using orc::plugins::KotlinSymbolExternalizerPlugin;

#if defined(__APPLE__)
using orc::plugins::ObjCSelectorFixupPlugin;
using orc::plugins::MachOHostDataSymbolGenerator;
#endif

#endif // HOTRELOAD_PLUGINS_HPP
