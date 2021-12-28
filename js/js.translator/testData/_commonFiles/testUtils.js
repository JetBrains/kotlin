function isLegacyBackend() {
   return typeof Kotlin !== "undefined" && typeof Kotlin.kotlin !== "undefined"
}