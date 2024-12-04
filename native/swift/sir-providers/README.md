# SIR Providers

The core of Swift export is a [`SirSession`](src/org/jetbrains/kotlin/sir/providers/SirSession.kt) interface which is decomposed into 
several components: providers. Each provider contains a small piece of `Analysis API` -> `SIR` logic which in turn might call other providers.

Such decomposition allows Swift export to be adapted to different use cases: standalone tools, IDE, etc. 

This architecture is inspired by `FirSession` from FIR and `KtAnalysisSession` from Analysis API.