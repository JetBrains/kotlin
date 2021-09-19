@file:Import("script-file-location-helper-imported-file.main.kts")

arrayOf(__FILE__.absolutePath, getDependentScriptFile().absolutePath)

