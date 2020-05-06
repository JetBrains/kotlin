# Kotlin Gradle build scripts support.

## Why custom Scripting support

The scripting support for Gradle becomes tricky since the default one was designed for a little bit another case: each script becomes configured individually on first opening/usage and loading supposes to be instant. 

Unfortunately, this is not working well for the Gradle case: the script configuration gathering is long operation since the Gradle configuration phase required to be executed to get it. For large projects, it may take from 5 seconds for hot daemon and to minutes for initial project loading. It may download dependencies, compile and build buildSrc project and all these things will be done on first script usage, even when we need just to know is such word in reference to our symbol when we are calling "Find usages". Among that, Gradle is not able to gather individual script configuration and always provides configuration for all scripts.

Taking this in mind, it becomes better to have this strategy for Gradle scripts:
- The user performing Gradle project importing/reimporting by executing Gradle configuration phase
- We have all Gradle scripts configured
- We can save this configuration to FS cache which can be persisted between IntelliJ restarts.

## More details about what we already have

[ScriptConfigurationManager] service already existed for providing and updating scripts configuration. It was working by loading scripts configuration on the first opening, storing in-memory cache, and FS cache. 

It also reloads it after each typing (with some throttling) and notifies the user if the configuration is changed with a suggestion to apply a new configuration. We may apply this configuration automatically, but it may introduce indexing which breaks UX (some action becomes unavailable during indexing). To leverage this situation, we are showing notification instead and indexing will occur only after an explicit user action. We can do this automatically if no new source and classes are introduced in dependencies, but this is not implemented yet.

Note: as we don't know in general when some script change causes script configuration change, we always run configuration reloading and comparing new configuration with the old one. If we can know that some particular change will affect script configuration, we can show notification before starting expensive loading, but this requires some knowledge of the script, so should be implemented by the script definition author. [ScriptConfigurationManager] provides ability to implement up-to-date stamp by extension point. It is implemented for the older Gradle versions, see "DefaultScriptingSupport extensions for older Gradle versions" below.

The manager also creates a list of all classes and sources of script dependencies to properly support analyzing of scripts. All these calculations are cached and invalidated on each change in script configurations.

Sometimes this loading may occur in the background, sometimes in sync. We also may apply configuration automatically in some cases (first loading, loading in sync, etc). All that introduces complex states intersection which is tricky to support. Adding another layer of complexity to that implementation is not a way to go.

## Custom scripting support

After some experiments and several iterations, the final idea is to provide the ability to create custom mechanics for managing the scripts configurations without breaking default scripting support.

As we already need a cache of all known scripts, the union of its classpath, and source path which is rebuilt from the scratch on any change, we may just provide the ability to populate this cache with custom content on each invalidation. That the main idea behind custom scripting support.

The other idea is to don't make any blocking work synchronously while getting some configuration. The cache should be always ready for unblocking reading. All updating operation is asynchronous and non-blocking thanks to copy-on-write strategy.

So, it should work in that way:
- Custom scripting support should manage the scripts configurations on its own (with own in-memory/fs cache).
- It should implement a method that will gather all script configuration providers and populate the classpath/sourcepath union that is used to get files for indexing.
- It may implement ScriptChangeListener extension point to do some invalidation on document change events.
- It should call `updater.update { ... updater.invalidate() ...  }` when the new script configurations are required. The manager will schedule an asynchronous update in the background.
- It is not able to load new configurations lazily on the first request (it is still possible to calculate script configuration lazily, but file name should be already registered)

The [CompositeScriptConfigurationManager] will provide redirection of [ScriptConfigurationManager] calls to the custom [ScriptingSupport] or [DefaultScriptingSupport].

## Gradle CustomScriptingSupport

`GradleBuildRootsManager` implementing `ScriptingSupport` described above.

There are many Gradle builds can be linked with a single IntelliJ project (don't confuse with the Gradle project and included build). This complicates implementation a bit, as we should manage this builds separately: each of them may have its own script definitions, Gradle version and java home. Typically, the IntelliJ project has no more than one [GradleBuildRoot].

`GradleBuildRootsManager` actually managing linked Gradle builds. See it's KDoc for more details.

The script configuration is stored in FS by using the IntelliJ VFS file attributes.

Note that we can provide custom scripting support only for projects that using Gradle 6.0 and later, as gathering script models unavailable in older Gradle versions. `GradleBuildRootsManager` falling back to default scripting support for such linked Gradle builds. It is extended through: see "DefaultScriptingSupport extensions for older Gradle versions" for more details.

## Watching files states across IntelliJ restarts

To have consistent sate of scripts, we should also be aware of external script changes. This is achieved by watching files using the IntelliJ VFS events. [GradleScriptInputsWatcher] is responsible for that.

The first tricky part is that scripts are depending on each other: so, when one script is changed, we actually should invalidate all other scripts as we don't know dependencies between them (Gradle will provide this information later, but it is not yet implemented). Actually, we should know the last modified timestamp of all scripts excepting a particular one. This can be achieved by storing timestamps of two last modified files. [LastModifiedFiles] utility is responsible for that.

Another tricky part is that we should track only scripts belong the Gradle project and should ignore all other `*.gradle.kts` files (in `testData` for example). This is achieved by storing Gradle project roots, as scripts can be exactly near Gradle project roots (excepting included and precompiled scripts which are not fully supported yet). This can be gathered from the Gradle project import information or from GradleProjectSettings when the import has not occurred yet. `GradleBuildRootsManager` does it.

## Out of project Gradle scripts

There are scripts that are not linked to any import Gradle project. In `testData` for example. Currently, we are suggesting to import Gradle project for such scripts, but we also may implement standalone Gradle scripts supporting using the `DefaultScriptingSupport` for explicitly listed scripts. We can implement this in the future if it will be valuable for users.

## DefaultScriptingSupport extensions for older Gradle versions

For Gradle versions before 6.0, we are still falling back to the [DefaultScriptingSupport] with some extensions.

We are showing notification before loading as we knew what changes will cause it. This is done by:
- implementing `org.jetbrains.kotlin.scripting.idea.listener` extension point and calling `suggestToUpdateConfigurationIfOutOfDate` instead of `ensureUpToDatedConfigurationSuggested` on document changes.
- implementing `org.jetbrains.kotlin.scripting.idea.loader` extension point and overriding `getInputsStamp`

We are also managing out of project scripts the same way as it is done in `GradleBuildRootsManager`.
