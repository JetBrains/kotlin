# hair

## TODO
* ~~Value DCE~~
* Object operations
  * Floating New
  * Floating Reads
    * ~~**WIP:** Memory dependencies~~
      * ~~Memory combing~~
* Value-filtering nodes
  * **WIP**
* Range analysis
* ~~Consider inverting control flow~~
  * ~~Or at least separate from value arguments~~
  * ~~Dedicated control use field~~
* Calls
  * Compilation session
  * Inline
  * Interproc analysis
    * Escape properties
    * Stores graph?
      * What memory locations changed, and how
  * Exceptions
* ~~Build param-less nodes without second `()`~~
  * Special support for singleton forms
* ~~Rework Session and friends~~
* Put Node and generated nodes in the same compilation?
* More normalizations
* Interpreter?
* ~~Rework Node arguments into a single array with accessors (after profiling?)~~
* Refactor generator
* ~~Rework node builder:~~
  * ~~ControlAwareBuilder(NormalizingBuilder(BaseBuilder))~~
    * ~~Initial builder~~
    * Dominators-aware builder
      * Only this one can modify CFG
    * GCM-aware builder
      * incremental GCM
* ~~Re-normalize on arg change~~

* Sophisticated verification of GCM result?