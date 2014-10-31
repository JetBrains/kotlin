# Default objects

## Goals

- make class objects stand out a little less
- allow to write extensions for class objects
- let every addressable object have a name

## TODO

- [ ] Extensions for "class objects" of classes that don't declare them?
- [ ] Allow names on object expressions?
- [ ] Decide on default name for default objects: `Default`, maybe?
- [ ] Replace all occurences of "class object" in user-facing strings, e.g. error messages

## Syntactic changes

- `default` modifier is allowed on objects nested into classes and traits
- names of objects marked as `default` may be omitted, such objects get a name by default (e.g. `Default`, to be decided later)

External cliens may access default objects by their name (givem explicitly or by default) as well as by the name of the containing class.

Only one default object is allowed per container.

## PSI Changes

`JetClassObject` element goes away, default objects are simply named objects with a modifier.

## Semantics

Default objects work the same way class objects used to work, the changes are purely syntactic.

## Compatibility

- `class object` syntax is deprecated (works the same as `default object` + issues a warning)
