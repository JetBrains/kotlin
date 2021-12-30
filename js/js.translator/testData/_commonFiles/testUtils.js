function isLegacyBackend() {
   var self = this || global
   return Boolean(self.__legacyBackend__)
}