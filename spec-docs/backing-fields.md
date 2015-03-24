# Backing-Fields

Goals:
* Drop special backing-field syntax (`$name`)
* allow annotations/visbility on backing fields

## Examples

``` kotlin
var prop: Type = init
  field _prop
  get
  set
```

## Syntax

A soft-keyword `field` is used is a prefix for a backing-field name of a property.
The special syntax for backing field access (`$propertyName`) is removed (deprecated, to be dropped later).
