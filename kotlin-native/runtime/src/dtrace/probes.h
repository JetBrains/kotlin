/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

 // File with empty DTrace defines when dtrace isn't available.

#ifndef	_PROBES_H
#define	_PROBES_H

#define	KOTLIN_NATIVE_RUNTIME_GCSTART(arg0, arg1)
#define	KOTLIN_NATIVE_RUNTIME_GCSTART_ENABLED() (0)

#endif	/* _PROBES_H */