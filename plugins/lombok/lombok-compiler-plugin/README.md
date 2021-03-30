Kotlin plugin to support Lombok annotations in the same compilation unit with kotlin

Features support:

[~] [Config files](https://projectlombok.org/features/configuration)

 - [x] Basic support - single config file
 - [ ] Import other config files
 - [ ] Config files discovery and bubbling

[~] [@Getter/@Setter](https://projectlombok.org/features/GetterSetter)
  
 - [x] Basic support
 - [x] Class-level 
 - [x] @Accessors config support: chain and fluent
 - [~] Config support
   - [x] lombok.getter.noIsPrefix
   - [x] lombok.accessors.fluent
   - [x] lombok.accessors.chain
   - [ ] lombok.accessors.prefix
   - [ ] lombok.noArgsConstructor.extraPrivate
   - [ ] lombok.copyableAnnotations
 - [ ] Copy annotations
 - [ ] Strip defined prefixes - in config and @Accessors
 - [ ] Skip generation with AccessLevel.NONE
 - [ ] Strip 'is' prefix for boolean fields

[~] [@With](https://projectlombok.org/features/With)
  
  - [x] Basic support
  - [ ] Copy annotations
  
[x] [@NoArgsConstructor, @RequiredArgsConstructor and @AllArgsConstructor](https://projectlombok.org/features/constructor)
 - [x] @NoArgsConstructor
 - [x] @AllArgsConstructor
 - [x] @RequiredArgsConstructor

[x] [@Data](https://projectlombok.org/features/Data)

[~] [@Value](https://projectlombok.org/features/Value)
  - [x] generate getters and constructors
  - [ ] make class final, make fields private and final

[ ] [@Builder](https://projectlombok.org/features/Builder)


Other todos:
 - [x] Generic classes
 - [x] Actually run compiled code 
 - [x] Don't generate members that already exist (if having a duplicate is a problem)
 - [x] Gradle integration (as subplugin or just a way to enable lombok support)
 - [ ] Maven integration (as subplugin or just a way to enable lombok support)
 - [ ] Nullability from annotations. Check if it is inherited from variable definition



 
