# CLAUDE.md — EveryLibs

Project-specific notes. Global rules (JDKs, git co-author, push-only-with-permission,
code conventions) live in the user's global `CLAUDE.md` and still apply.

## What this is

A set of small, independently-published JVM libraries, each a separate Maven artifact
under `br.com.finalcraft.everylibs`. **Extracted from and used by
[EverNifeCore](https://github.com/EverNife/EverNifeCore)** — the general-purpose pieces
carved out so any project can depend on just what it needs.

## Modules (dependency graph flows downward)

| Module | Artifact | Depends on | Contents |
|---|---|---|---|
| `reflection` | `everylibs-reflection` | — (leaf) | `FCReflectionUtil` tree over MethodHandle-backed, cached lookups; `ClassReflect`; `classpath.JarFinder` |
| `common` | `everylibs-common` | — | `Tuple`, `Triple`, `MinMax`, `SimpleEntry`, `MergeListResult`, `TriState`, `FCJavaVersion` |
| `utils` | `everylibs-utils` | `common` | `FCCollectionsUtil`, `FCTimeUtil`, `FCFileUtil`, `FCMathUtil`, `FCInputReader`, `NumberWrapper` |
| `executors` | `everylibs-executors` | `common`, `reflection` | `FCExecutorsUtil`, `SimpleThreadFactory`, `VirtualThreadedScheduledExecutor` |

`reflection` is a leaf (pure `java.lang.reflect` + `java.lang.invoke`). `utils`/`executors`
expose `common` via `api`.

## Build constraints (important)

- **Java 17 source syntax → Java 8 bytecode** via [Jabel](https://github.com/bsideup/jabel),
  with `options.release = 8` enforcing a **Java 8 API floor**. Production code must not use
  any Java 9+ API (no `Map.of`/`List.of`, `Optional.isEmpty`, `Stream.toList`, `VarHandle`,
  `MethodHandles.privateLookupIn`, records, …). `MethodHandle`, `ClassValue`, `Optional`,
  `ConcurrentHashMap` are Java 8 — fine.
- Newer-JVM features (e.g. Java 21 virtual threads) are reached via runtime reflection
  guarded by `FCJavaVersion`, never compile-time references.
- The build runs on the **Java 25 toolchain**; only `compileJava` is pinned to a Java 17
  compiler (Jabel rides javac internals). Tests run on Java 25.

```powershell
$env:JAVA_HOME = "C:\Users\Petrus\.jdks\temurin-25.0.3"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew build              # all modules
.\gradlew :reflection:test   # one module's tests
```

## Repo conventions

- **`wiki/`** is the GitHub wiki cloned as its own git repo (remote: `EveryLibs.wiki.git`),
  **gitignored** by this project (`/wiki/`). Edit/commit/push happen *inside* `wiki/`,
  separately from the main repo. See the wiki's `Editing-this-Wiki` page.
- **`specs/`** is a local-only working folder, **ignored** by git (`specs/.gitignore = *`).
  Design specs and migration notes live there but are never committed to the repo.
- **`idea-plugin/`** is a **git submodule** (its own repository, published separately) — the
  IntelliJ IDEA plugin that adds reflection-aware autocomplete/navigation/rename for EveryLibs
  lookups (the analogue of the IDE's own `Class.getDeclaredField("…")` support). It is **not** a
  Gradle-reactor module (`settings.gradle` doesn't include it): it targets the IDE runtime
  (JDK 17+, IntelliJ Platform Gradle Plugin 2.x, **Gradle 9.0+**) and builds standalone. Edit /
  commit / push happen *inside* `idea-plugin/`, then bump the submodule pointer in the main repo.
- Each module is published with its own `everylibs-*` artifactId (see its `build.gradle`).
