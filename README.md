# java-module-patching
Or what is "test-time module redefinition"?

## Module System Quick-Start Guide enhanced with Testing

The goal of this project is to perform intra-module (white box) and inter-module (black box) test runs without breaking the boundaries of the Java Module System.
To start from a common and well-known ground, the modules used in this project are based on the [Module System Quick-Start Guide](https://openjdk.java.net/projects/jigsaw/quick-start).

The example modules from the Quick-Start Guide, namely [`com.greetings`](src/com.greetings) and [`org.astro`](src/org.astro), are transferred to a modified directory layout structure.
The directory layout structure of this project allows separating production (short: **main**) from testing (short: **test**) code.
This project also adds another module named [`test.modules`](src/test.modules) that will be used to test the API of the two example modules.

This project also provides platform-agnostic foundation-tool-only-invoking programs in the [src/.build/](src/.build) directory.
The configuration via an external build tool like Maven is not a direct goal of this project, though might be added later.

### Directory Layout Structure

Consult the following drawing for an overview of the directory layout structure.

```text
└───src                            ___
    ├───.build                     | Platform-agnostic build programs
    |                              ___
    ├───com.greetings              | Sources of module "com.greetings"
    │   └───main                   |
    │       └───java               | <- module-info.java: `module com.greetings {...`
    │           └───com            |
    │               └───greetings  | <- Main.java: `System.out.format("Greetings %s!%n"...`
    |                              ___
    ├───org.astro                  | Sources of module "org.astro"
    │   ├───main                   |
    │   │   └───java               | <- module-info.java: `module org.astro {...`
    │   │       └───org            |
    │   │           └───astro      | <- World.java: `public String name() { return...`
    │   └───test                   |
    │       └───java               | <- module-info.java: `open /*test*/ module org.astro {...`
    │           └───org            |
    │               └───astro      | <- WorldTests.java: `@Test void accessPackagePrivateMember() {...`
    |                              ___
    └───test.modules               | Sources of module "test.modules"
        └───test                   |
            └───java               | <- module-info.java: `open /*test*/ module test.modules {...`
                └───test           |
                    └───modules    | <- IntegrationTests.java: `@Test void accessWorld() {...`
```

### Notable observations

- The sources of each module are located beneath a single base directory named like the module itself.

- A base module directory that only contains a **main** subdirectory is skipped from testing.

- A base module directory that only contains a **test** subdirectory is an inter-module (black box) candidate for testing.

- A base directory that contains both, namely **main** and **test**, subdirectories opts-in into [intra-module testing](#intra-module-testing).

- The sources of the two **main** modules copied from the Quick-Start Guide are located at
  - `src/com.greetings/main/java/`
  - `src/org.astro/main/java/`

- The sources of the two **test** modules newly introduced by this project are located at
  - `src/org.astro/test/java/`
  - `src/test.modules/test/java/`

- Simple arguments for the `--module-source-path` option of `javac` can be constructed:
  - **main**: `src/*/main/java`
  - **test**: `src/*/test/java`

To see the build of this project in action:

### Build: Assemble + Compile & Package + Test

- Install JDK 11 (or later)
- Start the following three programs from the base directory of this project:
  ```text
  java src/.build/Assemble3rdPartyModules.java
  java src/.build/CompileMainAndTestModules.java
  java src/.build/LaunchJUnitPlatform.java
  ```

The console output should read similar to this [run](https://github.com/sormuras/java-module-patching/runs/431213900#step:4:13):

```text
Assemble 3rd-party modules
< https://repo.maven.apache.org/maven2/org/apiguardian/apiguardian-api/1.1.0/apiguardian-api-1.1.0.jar
[...]
< https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter-params/5.6.0/junit-jupiter-params-5.6.0.jar

Compile main and test modules
- javac -d out/classes/main --module-source-path src/*/main/java --module org.astro,com.greetings
- jar --create --file out/modules/main/org.astro.jar -C out/classes/main/org.astro .
- jar --create --file out/modules/main/com.greetings.jar -C out/classes/main/com.greetings .
- javac -d out/classes/test --module-source-path src/*/test/java --module-path out/modules/main:lib --patch-module org.astro=out/modules/main/org.astro.jar --module org.astro,test.modules
- jar --create --file out/modules/test/org.astro.jar -C out/classes/test/org.astro .
- jar --create --file out/modules/test/test.modules.jar -C out/classes/test/test.modules .

Launch JUnit Platform
+ ../java --module-path out/modules/test:out/modules/main:lib --add-modules test.modules --patch-module org.astro=out/modules/main/org.astro.jar --module org.junit.platform.console --disable-banner --reports-dir=out/test-reports/test.modules --select-module test.modules
WARNING: module-info.class ignored in patch: out/modules/main/org.astro.jar
.
+-- JUnit Jupiter
   +-- IntegrationTests
      +-- accessWorld()
      +-- accessGreetings()

+ ../java --module-path out/modules/test:out/modules/main:lib --add-modules test.modules --patch-module org.astro=out/modules/main/org.astro.jar --module org.junit.platform.console --disable-banner --reports-dir=out/test-reports/org.astro --select-module org.astro
WARNING: module-info.class ignored in patch: out/modules/main/org.astro.jar
.
+-- JUnit Jupiter
   +-- WorldTests
      +-- accessPackagePrivateMember()
```

### Why is there a warning about "module-info.class ignored in patch"?

There are warnings emitted by the Java Module System for each modular test run above reading:

`WARNING: module-info.class ignored in patch: out/modules/main/org.astro.jar`

This is due to "test-time module redefinition".

The **test** module descriptor with its own directives tailored to testing purposes suppresses the **main** module descriptor.

## Intra-Module Testing

Intra-module testing is the modern incarnation of making us of the merging powers provided by the `--class-path` since the early days of testing.

### An old question answered then...

Consult the [JUnit 4 FAQ](https://junit.org/junit4/faq.html#organize_1) for a historical answer to the question _"Where should I put my test files?"_.
An quote from the answer reads:

> [...]
>
> An arguably better way is to place the tests in a separate parallel directory structure with package alignment.
> ```text
> src
>    com
>       xyz
>          SomeClass.java
> test
>    com
>       xyz
>          SomeClassTest.java
> ```
> These approaches allow the tests to access to all the public and package visible methods of the classes under test.
>
> [...]

See?

- Package names were expected to _clash_!

The `--class-path` merged those "separate parallel" directories into a virtual single one.
First-come, first-serve style.
Potentially scanning all elements of the `--class-path` for types and resources over and over again.

### ...and answered today.

"Same same but different" in the modular testing world nowadays.
Let's revisit the source directory layout structure for module `org.astro`:

```text
    |                              ___
    ├───org.astro                  | Sources of module "org.astro"
    │   ├───main                   |
    │   │   └───java               | <- module-info.java: `module org.astro {...`
    │   │       └───org            |
    │   │           └───astro      | <- World.java: `public String name() { return...`
    │   └───test                   |
    │       └───java               | <- module-info.java: `open /*test*/ module org.astro {...`
    │           └───org            |
    │               └───astro      | <- WorldTests.java: `@Test void accessPackagePrivateMember() {...`
```

- Package name are expected to _clash_.
- Module names are expected to _clash_ here too!
- Test class names are expected to use the same name as their class-under-test attached with `Tests` suffix.

We just can't rely on the classy `--class-path` to do merging-magic for us.

Here, the `--patch-module` and related options of `javac` and `java` are our tools of the trade.
With them, we may keep the boundaries of the Java Module System for the **main** modules intact.
Only for intra-module testing we need to redefine the API of a module in volatile (non-published) manner.
The next section will discuss this feature in detail.

## Test-time module redefinition

Let's zoom in further into the base directory of the `org.astro` module.

The **main** module describing compilation unit named `module-info.java` reads:

[`src/org.astro/main/java/module-info.java`](src/org.astro/main/java/module-info.java)

```java
module org.astro {
  exports org.astro;
}
```

The **test** module describing compilation unit, also named `module-info.java`, reads:

[`src/org.astro/test/java/module-info.java`](src/org.astro/test/java/module-info.java)

```java
open /*test*/ module org.astro /*extends "src/org.astro/main/java/module-info.java*/ {
  exports org.astro;

  requires org.junit.jupiter.api;
}
```

// TODO Explain the manual merge of directives of the main module.

// TODO Motivate the need for a new "patch" modifier as Robert did.

## Related Work

- "Testing In The Modular World" [sormuras.github.io/2018-09-11](https://sormuras.github.io/blog/2018-09-11-testing-in-the-modular-world)
- "RFE simplify usage of patched module" [Robert Scholte/2020-02-05](https://mail.openjdk.java.net/pipermail/jigsaw-dev/2020-February/thread.html#14357)
