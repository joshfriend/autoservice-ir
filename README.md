# AutoService IR

A Kotlin compiler plugin implementation of Google's AutoService annotation processor. This plugin generates `META-INF/services` files at compile time using Kotlin's FIR and IR backends, providing better performance and full K2 compiler support.

## Features
- **Works with existing code**: Compatible with the `@AutoService` annotation from [Google's AutoService library][google-auto-service]
- **Smart type inference**: Automatically infers service type when a class has only one supertype
- **K2 Compiler Support**: Fully compatible with Kotlin 2.0+ (K2 compiler) using both FIR and IR
- **FIR and IR-based**: Leverages Kotlin's modern compiler plugin infrastructure which is faster than KSP/Kapt
- **Drop-in replacement**: Can replace KSP or KAPT-based AutoService processing
- **ProGuard/R8 rules**: Includes keep rules to preserve service implementations during minification

## Installation
Add the AutoService Gradle plugin to your build configuration:

**build.gradle(.kts):**
```kotlin
plugins {
    kotlin("jvm") version "2.3.0"
    id("com.fueledbycaffeine.autoservice") version "<latest>"
}
```

## Usage
The AutoService plugin automatically processes classes annotated with `@AutoService` during compilation, generating the necessary `META-INF/services` files for Java's ServiceLoader mechanism.

### Basic Usage - Inferred Type
```kotlin
package com.example

import com.fueledbycaffeine.autoservice.AutoService

// Define your service interface
interface Logger {
    fun log(message: String)
}

// Just use @AutoService - the type is automatically inferred!
@AutoService
class ConsoleLogger : Logger {
    override fun log(message: String) {
        println(message)
    }
}
```

**What this generates:**

File: `build/classes/kotlin/main/META-INF/services/com.example.Logger`
```
com.example.ConsoleLogger
```

### Type Inference Details
When using `com.fueledbycaffeine.autoservice.AutoService`, the service type is automatically inferred when your class has **exactly one supertype** (besides `Any`):

**When inference works:**
- ✅ Class implements exactly one interface
  ```kotlin
  @AutoService  // MyInterface inferred
  class MyImpl : MyInterface { }
  ```
- ✅ Class extends exactly one abstract/concrete class
  ```kotlin
  @AutoService  // AbstractService inferred
  class ConcreteService : AbstractService() { }
  ```
- ❌ Class has multiple supertypes (must specify explicitly)
  ```kotlin
  @AutoService(InterfaceA::class, InterfaceB::class)  // Must be explicit
  class Multi : InterfaceA, InterfaceB { }
  ```
- ❌ Class has no supertypes (must specify explicitly)
  ```kotlin
  @AutoService(SomeInterface::class)  // Must be explicit
  class Standalone : SomeInterface { }
  ```

### With Explicit Service Type
If you prefer or need to be explicit:

```kotlin
import com.fueledbycaffeine.autoservice.AutoService

@AutoService(Logger::class)  // Explicitly specify the service type
class FileLogger : Logger {
    override fun log(message: String) {
        File("app.log").appendText("$message\n")
    }
}
```

**Loading services at runtime:**
```kotlin
// Automatically discovers all Logger implementations
val loggers = ServiceLoader.load(Logger::class.java)
loggers.forEach { logger ->
    logger.log("Hello from ${logger.javaClass.simpleName}")
}
```

### Using Google's Annotation (Compatibility Mode)
For existing codebases or compatibility with other tools:

```kotlin
import com.google.auto.service.AutoService

// Add the dependency in build.gradle(.kts):
// implementation("com.google.auto.service:auto-service-annotations:1.1.1")

@AutoService(Logger::class)  // Google's annotation requires explicit type
class FileLogger : Logger {
    override fun log(message: String) {
        File("app.log").appendText("$message\n")
    }
}
```

### Multiple Service Interfaces
Register a single implementation for multiple service types:

```kotlin
interface Logger {
    fun log(message: String)
}

interface Formatter {
    fun format(data: Map<String, Any>): String
}

@AutoService(Logger::class, Formatter::class)
class JsonLogger : Logger, Formatter {
    override fun log(message: String) {
        println(message)
    }
    
    override fun format(data: Map<String, Any>): String {
        // Format as JSON
        return data.entries.joinToString(",", "{", "}") { 
            """"${it.key}":"${it.value}""""
        }
    }
}
```

**Generates two service files:**
- `META-INF/services/com.example.Logger` → `com.example.JsonLogger`
- `META-INF/services/com.example.Formatter` → `com.example.JsonLogger`

### Multiple Implementations

You can have multiple implementations of the same service:

```kotlin
interface Logger {
    fun log(message: String)
}

@AutoService(Logger::class)
class ConsoleLogger : Logger {
    override fun log(message: String) = println(message)
}

@AutoService(Logger::class)
class FileLogger : Logger {
    override fun log(message: String) {
        File("app.log").appendText("$message\n")
    }
}

@AutoService(Logger::class)
class NetworkLogger : Logger {
    override fun log(message: String) {
        // Send to logging service
    }
}
```

**Generates one service file:**

`META-INF/services/com.example.Logger`:
```
com.example.ConsoleLogger
com.example.FileLogger
com.example.NetworkLogger
```

## Migration Guide
Replace the KSP/KAPT plugin with this plugin:

```kotlin
plugins {
    kotlin("jvm")
    id("com.fueledbycaffeine.autoservice") version "<latest>"
    // Remove: id("com.google.devtools.ksp") or kotlin("kapt")
}

dependencies {
    // Remove KSP/KAPT processor dependencies:
    // ksp("dev.zacsweers.autoservice:auto-service-ksp:<version>")  // Remove this
    // kapt("com.google.auto.service:auto-service:<version>")       // Remove this
    
    // Option 1: Keep Google's annotation (no code changes)
    implementation("com.google.auto.service:auto-service-annotations:<version>")
    
    // Option 2: Use our annotation for type inference (change imports)
    // No dependency needed!
}
```

## Comparison with Alternative Processors
| Feature                     | AutoService-IR | [KSP AutoService][auto-service-ksp] | [KAPT AutoService][google-auto-service] |
|-----------------------------|----------------|-------------------------------------|-----------------------------------------|
| Type Inference              | ✅ Yes          | ❌ No                                | ❌ No                                    |
| Service file merging        | ✅ Yes          | ❌ No                                | ✅ Yes                                   |
| Real-time IDE error checking | ✅ Yes (FIR)    | ❌ Build required                    | ❌ Build required                        |

### ProGuard/R8 Support
The plugin automatically supports ProGuard and R8 minification through **annotation-based keep rules** bundled in the `annotations` artifact:

```proguard
# Bundled in META-INF/proguard/autoservice-annotations.pro
-keep @com.fueledbycaffeine.autoservice.AutoService class * {
    <init>();
}
-keep @com.google.auto.service.AutoService class * {
    <init>();
}
```

These rules automatically preserve any class annotated with `@AutoService` along with its no-argument constructor (required by `ServiceLoader`).

This works automatically with Android's default R8 configuration, which consumes rules from `META-INF/proguard/` in dependencies. No additional configuration is needed.

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments
Inspired by [@ZacSweers][zac] [auto-service-ksp][auto-service-ksp] implementation of AutoService, and [Metro][metro] compiler plugin patterns. He figured out all the hard bits of incremental compilation with Metro and showed me how to do it.

[google-auto-service]: https://github.com/google/auto/tree/main/service
[auto-service-ksp]: https://github.com/ZacSweers/auto-service-ksp
[metro]: https://github.com/ZacSweers/Metro
[zac]: https://github.com/ZacSweers