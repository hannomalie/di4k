This is experimental, don't use at ho... in production!
======================================================

# di4k
The 83457th approach to a **dependency injection** framework that is really lightweight with no magic.

### TLDR
**di4k** is a KSP based compiler plugin that generates a module implementation for you by solely wiring
constructor calls.
It just generates the code that you would write by yourself when you would do dependency injection or modules
by yourself.

Code tells more than words.

You do this in your build:

```kotlin
plugins {
    id("com.google.devtools.ksp")
}
dependencies {
    ksp("de.hanno.di4k:processor:0.0.1-SNAPSHOT")
}
kotlin {
    sourceSets.main.kotlin.srcDir("build/generated/ksp/main/kotlin/")
}
```

You code this in your project:

```kotlin
data class Dependency(val aString: String)
data class FactoryDependency(val aString: String)
data class ComplexDependency(val aString: String, val aDependency: Dependency)
class Requirement

abstract class FooModule(
    val requirement: Requirement,
) {
    val bar: String
        get() = "barStringValue"
    val baz = "bazStringValue"

    abstract val factory: () -> FactoryDependency
    abstract val aDependency: Dependency
    abstract val aComplexDependency: ComplexDependency
}
```

and the plugin generates

```kotlin
open class FooModuleImpl(
    requirement: Requirement,
): FooModule(
    requirement,
) {
    override val factory = { FactoryDependency(bar) }
    override val aDependency = Dependency(bar)
    override val aComplexDependency = ComplexDependency(bar, aDependency)
}
```

into a folder in your build folder that is treated as a regular source folder by your build tool and IDE, so that
you have autocompletion and all the other good things in your IDE.

### How to use and current state

The TLDR above is really all you need to use this.

Currently, the plugin searches for abstract classes that contain the word "Module" as that is sufficient for me and my projects.
It would be trivial to let the plugin search for abstract classes that are annotated either by a @Module annotation
that I provide as a dependency or by any annotation with a simple name "Module" in order to prevent dependencies.

Afterwards, an open subclass will be generated. Open, because this would allow for simple dependency overrides by using
the override mechanism the Kotlin language already offers. Keep in mind though, that I probably have to add a lazy initialization
option, because otherwise property initialization order could become a problem.

Every abstract property in the abstract class will have an implementation generated that is just the primary constructor call
of the type or a reference to a property implementation of that type in the module.
When a property is of the function type () -> T, a lambda will be generated that calls T's primary constructor and the constructor
parameters are resolved within the module.

Concrete properties on the abstract class are ignored.

You can declare constructor parameters in the abstract class, and they will become constructor parameters for the subclass.
You can use this to pass in base configuration that is resolved somewhere outside the module.

There is no way to bypass the type system. You want to inject a String? You can do that once, and it will be
injected everywhere a String is required. I won't change anything about that as long as I can. Whenever you need to inject basic types
like Int or String from a config source, I recommend using a dedicated framework for input parsing and passing in
custom configuration types.

I haven't implemented retrieval of dependencies of a certain super type, but that will happen somewhen.
