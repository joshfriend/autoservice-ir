#!/bin/bash
set -e

# AutoService IR Release Script
# Adapted from spotlight's release process

if [ -z "$1" ]; then
  echo "Usage: ./release.sh <version>"
  echo "Example: ./release.sh 0.1.0"
  exit 1
fi

NEW_VERSION=$1

# Validate version format
if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Error: Version must be in format X.Y.Z (e.g., 0.1.0)"
  exit 1
fi

# Check if we're on main branch
CURRENT_BRANCH=$(git branch --show-current)
if [ "$CURRENT_BRANCH" != "main" ]; then
  echo "Error: Must be on main branch to release. Current branch: $CURRENT_BRANCH"
  exit 1
fi

# Check if working directory is clean
if ! git diff-index --quiet HEAD --; then
  echo "Error: Working directory is not clean. Commit or stash your changes."
  exit 1
fi

# Get current version from gradle.properties
CURRENT_VERSION=$(grep '^VERSION_NAME=' gradle.properties | cut -d'=' -f2 | xargs)
echo "Current version: $CURRENT_VERSION"
echo "New version: $NEW_VERSION"

# Update version in gradle.properties
echo "Updating gradle.properties..."
sed -i.bak "s/^VERSION_NAME=.*/VERSION_NAME=$NEW_VERSION/" gradle.properties
rm gradle.properties.bak

# Update CHANGELOG.md if it exists
if [ -f "CHANGELOG.md" ]; then
  echo "Updating CHANGELOG.md..."
  # Replace [Unreleased] with the new version and today's date
  TODAY=$(date +%Y-%m-%d)
  sed -i.bak "s/## \[Unreleased\]/## [Unreleased]\n\n## [$NEW_VERSION] - $TODAY/" CHANGELOG.md
  rm CHANGELOG.md.bak
fi

# Commit version change
echo "Committing version change..."
git add gradle.properties CHANGELOG.md 2>/dev/null || git add gradle.properties
git commit -m "Release v$NEW_VERSION"

# Create tag
echo "Creating tag v$NEW_VERSION..."
git tag -a "v$NEW_VERSION" -m "Release v$NEW_VERSION"

# Push changes and tag
echo ""
echo "Ready to release v$NEW_VERSION!"
echo ""
echo "Review the changes and then run:"
echo "  git push origin main"
echo "  git push origin v$NEW_VERSION"
echo ""
echo "This will trigger the CI/CD pipeline to:"
echo "  1. Run tests"
echo "  2. Publish to Maven Central"
echo "  3. Publish to Gradle Plugin Portal"
echo "  4. Create GitHub Release"
echo "  5. Bump version to next snapshot"
