@echo off

set "ALL_PARAMS=%konanCompilerArgs%"
set "MEMORY_MODEL=%1"

if "%MEMORY_MODEL%" == "legacy" (
  if "%ALL_PARAMS%" == "" (
    set "ALL_PARAMS=-memory-model strict"
  ) ELSE (
    set "ALL_PARAMS=-memory-model strict %ALL_PARAMS%"
  )
)
if not "%ALL_PARAMS%" == "" (
  SET "ALL_PARAMS=-PcompilerArgs=%ALL_PARAMS%"
)

echo "##teamcity[setParameter name='env.konanCompilerArgs' value='%ALL_PARAMS%']"