# java-module-patching
Or what is "test-time module redefinition"?

## Related Work

- "pro/ModuleHelper.mergeModuleDescriptor()" [forax/2016](https://github.com/forax/pro/blob/2ddd9425cb95617b6dfd6c7d077ed387a5f6809c/src/main/java/com.github.forax.pro.helper/com/github/forax/pro/helper/ModuleHelper.java#L338)
- "Testing In The Modular World" [sormuras.github.io/2018-09-11](https://sormuras.github.io/blog/2018-09-11-testing-in-the-modular-world)
- "RFE simplify usage of patched module" [Robert Scholte/2020-02-05](https://mail.openjdk.java.net/pipermail/jigsaw-dev/2020-February/thread.html#14357)

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

The last program `LaunchJUnitPlatform.java` starts the JUnit Platform Console Launcher on the module path.
Its [command line options](https://junit.org/junit5/docs/current/user-guide/#running-tests-console-launcher-options) offer a `--select-module` option.
This option selects the specified module for test discovery.
Effectively, that results in executing and evaluating each `@Test`-annotated method with the specified module.

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
Only for intra-module testing we need to redefine the API of a module in volatile (non-published, not-reusable) manner.
The next section will discuss this feature in detail.

## Test-time module redefinition

Let's zoom in further into the base directory of the `org.astro` module.
That magnification will finally lead to the motivation why a new `PATCH` (or what-ever it will be called) modifier was requested via ["RFE simplify usage of patched module"](https://mail.openjdk.java.net/pipermail/jigsaw-dev/2020-February/thread.html#14357).

### Main-variant of module `org.astro`

The **main** module describing compilation unit named `module-info.java` and located in the `src/org.astro/main/java` directory reads:

[`src/org.astro/main/java/module-info.java`](src/org.astro/main/java/module-info.java)

```java
module org.astro {
  exports org.astro;
}
```

It describes the **main** API of module `org.astro`.

That API defines what dependent modules may see and use from module `org.astro`.
In a real project, this module would be published for re-use.

### Test-variant of module `org.astro`

The **test** module describing compilation unit, also named `module-info.java`, but located in the `src/org.astro/test/java` directory reads:

[`src/org.astro/test/java/module-info.java`](src/org.astro/test/java/module-info.java)

```java
open /* test */ module org.astro /* patches "src/org.astro/main/java/module-info.java */ {
  exports org.astro;

  requires org.junit.jupiter.api;
}
```

That module describes the **test** API of module `org.astro`.

This **test** module variant is _**NOT**_ intended to be published _**NOR**_ is it expected to be re-used by other modules.
The inline comments in the compilation unit try to underline those intentions.

The only purpose of this **test** module variant is to serve as the starting point or root configuration for the Java Module System at test-time in an intra-module scenario.
Because at test-time (compilation and runtime) the author of the tests may want to make use of additional modules.
Additional modules related to the task of testing: that includes system modules, project modules, and of potentially modules provided by testing frameworks.

Technically, the **test** module patches, strictly speaking adds, module modifiers and directives on top of the **main** module descriptor.
Here test author may open the module for deep reflection as required by an testing framework, of course for test-time only.
Here test author may read additional modules, like the external `org.junit.jupiter.api` module in this example or a module offered by the system, e.g. `java.sql`.
Here test author may provide and use additional services, if the test code needs those to perform its automated checks.

Note: all the mentioned patches above are a non-issue in the inter-module (black box) testing scenario.
Consult for example the [`test.modules`](src/test.modules/test/java/module-info.java) module declaration.
It's a standalone root configuration for starting tests declared within that very module.
There is no related **main** `test.modules` module for this **test** module.

### Introduce new module modifier: PATCH

Copy module directives from the **main** to the **test** variant is brittle.
This is why I second Robert's suggestion to introduce a new `patch` modifier.
Applied to this project, especially to the `org.astro` module, the **test** module would read like:

> One solution that might fit here is the introduction of a new modifier:
> ```text
>  patch open module org.astro {
>     requires org.junit.jupiter.api;
>  }
> ```

Citing Robert again:

> This describes clear the purpose of the module, and as the "patch" already
  implies: it should be handled with care.
  Allowing such module descriptor should help adopting the modular system
  faster. It doesn't require the knowledge of the module related flags, the
  build tool can solve it. As the developer is already familiar with the main
  module descriptor, adding a patched module descriptor when required is just
  a simple step.

An extended **test** variant of the module could read:

```java
patch open module org.astro {
    requires java.sql;              // Needed by some tests...
    requires org.junit.jupiter.api; // Test API
    requires org.assertj.core;      // Assertion Framework
    // ... here be more additional directives
}
```

Technical note: the [warning](#why-is-there-a-warning-about-module-infoclass-ignored-in-patch) described above
may already hint to a good place where the modifiers and directives of the **main** and **test** module can be merged.
Rémi already showed how to perform that [merge](https://github.com/forax/pro/blob/2ddd9425cb95617b6dfd6c7d077ed387a5f6809c/src/main/java/com.github.forax.pro.helper/com/github/forax/pro/helper/ModuleHelper.java#L338).


## Alternatives

- Let the end-user learn and understand the command line options of `javac` and `java` related the tweaking the configuration of the Java Module System.
- Let each build tool come up with its solution for providing additional module directives for intra-module testing.
