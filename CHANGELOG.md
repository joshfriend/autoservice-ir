# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0-SNAPSHOT]

### Added
- Initial implementation of AutoService as a Kotlin compiler plugin
- Support for `@AutoService` annotation from Google's AutoService library
- Smart type inference - automatically infers service type when class has single supertype
- Multi-module structure (compiler plugin + Gradle plugin)
- Comprehensive unit tests using kotlin-compile-testing
- Functional tests using autonomousapps gradle-testkit
- K2 compiler support
- Optional validation that implementations match service interfaces
- Configuration options: enabled, debug, verify
- Maven Central publishing configuration
- Gradle Plugin Portal publishing configuration
- GitHub Actions CI/CD workflows

### Features
- Works as drop-in replacement for KSP/KAPT-based AutoService
- Generates META-INF/services files at compile time
- Supports multiple service interfaces per implementation
- Supports multiple implementations per service interface
- Debug logging option for troubleshooting
