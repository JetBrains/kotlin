# Kotlin Ultimate

This project is a part of **Kotlin IntelliJ IDEA Plugin** 
which provides support for Ultimate IDEA features from Kotlin side.

If you want to work on this project you should open it in IDEA as separate project.

## Build

To build this project:
 
1. Run `ant -f update_dependencies.xml` in **kotlin-ultimate** folder.
2. Build main Kotlin project itself.
3. Build **kotlin-ultimate** project.

## Build a plugin

If you want to build a **Kotlin IntelliJ IDEA Plugin** locally 
with ultimate features support you should:

1. Build kotlin plugin in main project: **Build -> Build Artifacts -> Kotlin Plugin -> Build**.
2. Run `ant -f build.xml` in **kotlin-ultimate** folder.

Then you will get kotlin-plugin with ultimate features in `ultimate/out/artifacts/Kotlin` folder.
And then you can move/copy/symlink `ultimate/out/artifacts/Kotlin` to the IDEA config: `config/plugins/Kotlin`.  