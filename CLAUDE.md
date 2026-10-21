# CLAUDE.md

## Project overview

Heraldry is a Spigot plugin that gives players and factions a coat of arms and renders it onto Minecraft map items, the one surface a vanilla client will display an arbitrary image on. Arms can be imported from an image, including one exported from [Armoria](https://azgaar.github.io/Armoria/).

**Status: pre-implementation.** The repository holds a buildable, empty Spigot plugin skeleton. The scope of the first release, and its deliberate non-goals, are defined in [MVP.md](MVP.md). Read it before implementing anything.

## Technology stack

- Java, compiled with `options.release = 17` (see the comment in `build.gradle`: the oldest Minecraft version in `minecraft-versions.json`, 1.19.4, runs on Java 17). Builds run on **JDK 21**, as CI does.
- Gradle 8.8 via the committed wrapper (`gradlew`).
- Spigot API `1.21.4-R0.1-SNAPSHOT` (`compileOnly`, repeated as `testImplementation` so tests can see it).
- JUnit Jupiter 5.10.2.

## Build and test commands

```bash
# JDK 21 is required; point JAVA_HOME at it, for example:
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# Full build: compile, unit tests, jar -> build/libs/Heraldry-<version>.jar
./gradlew clean build

# Tests only
./gradlew test

# A single test class
./gradlew test --tests 'com.dansplugins.heraldry.HeraldrySmokeTest'
```

CI (`.github/workflows/build.yml`) runs `./gradlew clean build` on JDK 21, then the [release-gates](https://github.com/Dans-Plugins/release-gates) API compatibility check, which verifies every Bukkit class, field and method the jar uses exists on each version listed in `minecraft-versions.json` and that the bytecode fits the Java each runs on. To run it locally after a build:

```bash
curl -fsSL -o /tmp/check_api_compat.py \
  https://raw.githubusercontent.com/Dans-Plugins/release-gates/v12/gates/check_api_compat.py
python3 /tmp/check_api_compat.py --versions minecraft-versions.json target build/libs
```

There is no linter or formatter configured.

## Repository layout

```
src/main/java/com/dansplugins/heraldry/   Plugin source; Heraldry.java is the JavaPlugin entry point
src/main/resources/plugin.yml        Plugin descriptor; ${version} is expanded from build.gradle
src/test/java/com/dansplugins/heraldry/   JUnit tests
minecraft-versions.json              Minecraft versions the plugin claims to support (read by CI)
.github/workflows/build.yml          Build + API compatibility check
MVP.md                               Scope of v1 and its non-goals
```

## Dependencies on other plugins

No hard dependencies. MVP.md lists [Medieval Factions](https://github.com/Dans-Plugins/Medieval-Factions) as a **soft dependency** for faction-owned arms; the skeleton's `plugin.yml` does not yet declare `softdepend`. MVP.md flags an unresolved licensing question about Armoria's WappenWiki-derived charges (non-commercial only): do not bundle, redistribute or server-side render Armoria charges, or call the Armoria API at runtime, until that is resolved.

## Coding conventions

- Package root `com.dansplugins.heraldry`; classes PascalCase, methods and fields camelCase, 4-space indentation.
- The project follows the Dans-Plugins standards in [dpc-conventions](https://github.com/Dans-Plugins/dpc-conventions), including its documentation practices: `USER_GUIDE.md`, `COMMANDS.md`, `CONFIG.md` and `CHANGELOG.md` are added with the first release.
- Keep Bukkit/Spigot API use within what every version in `minecraft-versions.json` provides; the CI gate fails otherwise.

## Do / Don't

**Do:**
- Keep work inside the v1 scope in MVP.md. Its "v1 does not" list is deliberate.
- Add unit tests alongside new behaviour and run `./gradlew clean build` before pushing.
- Raise MVP.md's open questions with the maintainer instead of silently picking an answer when a change depends on one.

**Don't:**
- Don't raise `targetJavaVersion` above 17 or bump the Spigot API without also revisiting `minecraft-versions.json`.
- Don't change the release-gates version or remove the API check from `build.yml` as a side effect of other work.
- Don't commit build output (`build/`, `.gradle/`); `.gitignore` already excludes it.
