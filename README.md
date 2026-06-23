# EveryLibs

A set of small, modular, independently-published Java libraries. Each module is a
separate Maven artifact under the `br.com.finalcraft.everylibs` group, so a consumer
pulls in only what it needs.

Every module is **written in Java 17 syntax** and **compiled to Java 8 bytecode**
(via [Jabel](https://github.com/bsideup/jabel)), built with a **Java 25 toolchain**.
Features from newer JVMs (e.g. virtual threads) are reached through guarded runtime
reflection, so the Java 8 floor always holds.

## Modules

| Module      | Artifact               | Depends on            | Contents |
|-------------|------------------------|-----------------------|----------|
| `common`    | `everylibs-common`     | —                     | Shared primitives (`Tuple`, `Triple`, `MinMax`, `SimpleEntry`, `MergeListResult`, `TriState`) and `FCJavaVersion`. |
| `reflection`| `everylibs-reflection` | —                     | `FCReflectionUtil` tree facade over the per-member lookups (`FieldReflection`, `MethodReflection`, `ConstructorReflection`, `ClassReflection`, `AnnotationReflection`), the `ConstructorInvoker` / `FieldAccessor` / `MethodInvoker` interfaces, the fluent `ClassReflect` handle, `ReflectionException`, and `classpath.JarFinder`. See [reflection/MIGRATION.md](reflection/MIGRATION.md). |
| `utils`     | `everylibs-utils`      | `common`              | `FCTimeUtil`, `FCInputReader`, `FCFileUtil`, `FCCollectionsUtil`, `FCMathUtil`, `NumberWrapper`. |
| `executors` | `everylibs-executors`  | `common`, `reflection`| `FCExecutorsUtil`, `SimpleThreadFactory`, `VirtualThreadedScheduledExecutor`. |

## Using a module (Gradle)

```groovy
repositories {
    mavenCentral()
    maven { url = 'https://maven.petrus.dev/public' }
}

dependencies {
    implementation 'br.com.finalcraft.everylibs:everylibs-reflection:1.0.0'
    implementation 'br.com.finalcraft.everylibs:everylibs-utils:1.0.0'
    implementation 'br.com.finalcraft.everylibs:everylibs-executors:1.0.0'
}
```

The `everylibs-common` artifact arrives transitively, since every module exposes it
via `api`.

## Building

```powershell
.\gradlew build
```
