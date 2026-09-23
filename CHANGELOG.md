# Changelog

All notable changes to this project. Follows [Semantic Versioning](https://semver.org/).

Versions are published as `everylibs-reflection`, `everylibs-common`, `everylibs-utils` and
`everylibs-executors` under the group `br.com.finalcraft.everylibs`. All four always share one
version.

## [Unreleased]

## [1.1.0] - 2026-09-23

A class lookup that says *why* it came back empty.

### Added

- **`ClassReflection.lookupClass(String)` and `lookupClass(String, ClassLoader)`, returning a
  `ClassLookup`.** Its outcome is one of `FOUND`, `ABSENT` or `UNLINKABLE`, and an unlinkable lookup
  carries the `LinkageError` the runtime threw (`getLinkageError()`). `getClass` answers `null` for
  both a missing class and one that is present but fails to link or initialize - right for "can I use
  it?", wrong for "why not?". A hybrid server that carries a type and cannot load it is a failure to
  report, not a platform difference to branch on, and `lookupClass` is how a caller tells the two
  apart.

### Changed

- `getClass(String)`, `getClass(String, ClassLoader)` and `isClassLoaded(String)` now answer through
  `lookupClass`. Their results are the same; the visible difference is that `isClassLoaded` now
  caches a class it found, as `getClass` always did.

## [1.0.0] - 2026-08-30

First published release: the general-purpose pieces carved out of EverNifeCore, one artifact per
module, Java 8 bytecode with a Java 8 API floor.

### Added

- **`everylibs-reflection`** - the `FCReflectionUtil` tree (`getClasses()`, `getFields()`,
  `getMethods()`, `getConstructors()`, `getAnnotations()`) over cached, `MethodHandle`-backed
  invokers (`FieldAccessor`, `MethodInvoker`, `ConstructorInvoker`); `ClassReflect` for a fixed
  owner; `classpath.JarFinder`. Field and method lookups walk superclasses and then implemented
  interfaces, so inherited `default` methods resolve. Includes the `getField(String className,
  String name)` overload.
- **`everylibs-common`** - `Tuple`, `Triple`, `MinMax`, `SimpleEntry`, `MergeListResult`,
  `TriState` and `FCJavaVersion`.
- **`everylibs-utils`** - `FCCollectionsUtil`, `FCTimeUtil`, `FCFileUtil`, `FCMathUtil`,
  `FCInputReader` and `NumberWrapper`, whose `hashCode` agrees with its `doubleValue`-based
  `equals`.
- **`everylibs-executors`** - `FCExecutorsUtil`, `SimpleThreadFactory` and
  `VirtualThreadedScheduledExecutor`, which use Java 21 virtual threads when the runtime has them.

[Unreleased]: https://github.com/EverNife/EveryLibs/compare/v1.1.0...HEAD
[1.1.0]: https://github.com/EverNife/EveryLibs/compare/v1.0.0...v1.1.0
[1.0.0]: https://github.com/EverNife/EveryLibs/releases/tag/v1.0.0
