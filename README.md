# EveryLibs

A set of small, modular, independently-published Java libraries. Each module is a
separate Maven artifact under the `br.com.finalcraft.everylibs` group, so a consumer
pulls in only what it needs.

Every module is **written in Java 17 syntax** and **compiled to Java 8 bytecode**
(via [Jabel](https://github.com/bsideup/jabel)), built with a **Java 25 toolchain**.
Features from newer JVMs (e.g. virtual threads) are reached through guarded runtime
reflection, so the Java 8 floor always holds.

> **Origin** — EveryLibs is extracted from, and used by,
> [EverNifeCore](https://github.com/EverNife/EverNifeCore). It carves the
> general-purpose pieces of that project into small, standalone artifacts that any JVM
> project can depend on.

📖 **Full documentation lives in the [Wiki](https://github.com/EverNife/EveryLibs/wiki)** —
start with [Installation](https://github.com/EverNife/EveryLibs/wiki/Installation) and
[Reflection](https://github.com/EverNife/EveryLibs/wiki/Reflection).

## Modules

| Module      | Artifact               | Depends on            | Contents |
|-------------|------------------------|-----------------------|----------|
| `reflection`| `everylibs-reflection` | —                     | `FCReflectionUtil` — a tree over `MethodHandle`-backed, cached field/method/constructor/class/annotation lookups, plus the fluent `ClassReflect` handle and `classpath.JarFinder`. |
| `common`    | `everylibs-common`     | —                     | Shared primitives: `Tuple`, `Triple`, `MinMax`, `SimpleEntry`, `MergeListResult`, `TriState`, and `FCJavaVersion`. |
| `utils`     | `everylibs-utils`      | `common`              | `FCCollectionsUtil`, `FCTimeUtil`, `FCFileUtil`, `FCMathUtil`, `FCInputReader`, `NumberWrapper`. |
| `executors` | `everylibs-executors`  | `common`, `reflection`| `FCExecutorsUtil`, `SimpleThreadFactory`, `VirtualThreadedScheduledExecutor`. |

`reflection` is a leaf (pure `java.lang.reflect` + `java.lang.invoke`). `utils` and
`executors` expose `common` via `api`, so it arrives transitively with them.

## Using a module (Gradle)

```groovy
repositories {
    mavenCentral()
    maven { url = 'https://maven.petrus.dev/public' }
}

dependencies {
    implementation 'br.com.finalcraft.everylibs:everylibs-reflection:1.0.0'
    // implementation 'br.com.finalcraft.everylibs:everylibs-utils:1.0.0'
    // implementation 'br.com.finalcraft.everylibs:everylibs-executors:1.0.0'
}
```

See [Installation](https://github.com/EverNife/EveryLibs/wiki/Installation) for Maven and
the per-module breakdown.

## Building

```powershell
.\gradlew build
```
