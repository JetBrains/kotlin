@echo off
rem Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
rem Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.

setlocal

set _KOTLIN_HOME=%~dp0..
set _NODE_EXECUTABLE=%KOTLIN_WEB_IMAGE_NODE%
if "%_NODE_EXECUTABLE%"=="" set _NODE_EXECUTABLE=node

rem '--experimental-wasm-exnref' enables the WebAssembly exception handling proposal,
rem which is required by the GraalVM Web Image runtime on Node.js versions prior to 25.
"%_NODE_EXECUTABLE%" --experimental-wasm-exnref "%_KOTLIN_HOME%\bin\kotlinc-wasm.js" %*

endlocal
