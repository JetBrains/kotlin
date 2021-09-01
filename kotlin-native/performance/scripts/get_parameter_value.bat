@echo off

set "ALL_PARAMS=%konanCompilerArgs%"
set "MEMORY_MODEL=%1"

if "%MEMORY_MODEL%" == "experimental" (
  if "%ALL_PARAMS%" == "" (
    set "ALL_PARAMS=-memory-model experimental"
  ) ELSE (
    set "ALL_PARAMS=-memory-model experimental %ALL_PARAMS%"
  )
)
if not "%ALL_PARAMS%" == "" (
  SET "ALL_PARAMS=-PcompilerArgs=%ALL_PARAMS%"
)

echo "##teamcity[setParameter name='env.konanCompilerArgs' value='%ALL_PARAMS%']"