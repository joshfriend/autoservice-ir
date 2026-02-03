# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.1]

### Changed
- Generate metadata about service classes during FIR using `FirDeclarationDataRegistry` so that IR can read them directly instead of reparsing the `@AutoService` annotations and classes.
- Remove mirror classes during IR generation

## [0.1.0]

### Added
- Initial implementation of AutoService as a Kotlin compiler plugin
- Support for `@AutoService` annotation from Google's AutoService library
- Smart type inference - automatically infers service type when class has single supertype
- Optional validation that implementations match service interfaces
