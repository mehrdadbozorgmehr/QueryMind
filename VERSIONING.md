# Version Numbering in Open Source Projects

## Understanding Semantic Versioning (SemVer)

Open source projects typically follow **Semantic Versioning** (SemVer), a widely-adopted versioning scheme that makes it easy to understand what changes are included in each release.

## Version Format: MAJOR.MINOR.PATCH

A version number consists of three parts:

```
MAJOR.MINOR.PATCH
  7  .  1  .  5
```

### MAJOR version (7)
- **When to increment:** When you make incompatible API changes
- **Meaning:** Breaking changes that may require users to modify their code
- **Example:** Removing a public API method, changing method signatures

### MINOR version (1)
- **When to increment:** When you add functionality in a backward-compatible manner
- **Meaning:** New features that don't break existing functionality
- **Example:** Adding new API methods, new features, enhancements

### PATCH version (5)
- **When to increment:** When you make backward-compatible bug fixes
- **Meaning:** Bug fixes, security patches, minor improvements
- **Example:** Fixing bugs, improving performance, security updates

## What Does "7.1.x" Mean?

When you see a version like **7.1.x**, the "x" is a **wildcard** that represents **any patch version**. This notation is commonly used to refer to a series of releases.

### Examples:
- **7.1.x** means: version 7.1.0, 7.1.1, 7.1.2, 7.1.3, etc.
- **7.1.x** includes all patch releases in the 7.1 minor version series
- **7.1.x** does NOT include 7.0.x or 7.2.x

### Practical Usage:

1. **In Documentation:**
   - "This feature is available in version 7.1.x or later"
   - Means: Any version starting from 7.1.0 onwards (7.1.0, 7.1.1, 7.1.2, etc.)

2. **In Dependency Management:**
   - Some package managers use similar notation
   - `~7.1.0` or `7.1.x` means: "compatible with 7.1.0, accepting any patch updates"
   - Allows automatic updates for bug fixes but not for new features

3. **In Release Notes:**
   - "Security fix for 7.1.x users"
   - Means: All users on any 7.1 patch version should update

## Version Ranges

You might also see these notations:

| Notation | Meaning |
|----------|---------|
| `7.1.x` | Any patch version of 7.1 (7.1.0, 7.1.1, 7.1.2, ...) |
| `7.x` | Any minor/patch version of 7 (7.0.0, 7.1.0, 7.2.5, ...) |
| `x.x.x` or `*` | Any version at all |
| `>=7.1.0` | Version 7.1.0 or higher |
| `^7.1.0` | Compatible with 7.1.0 (npm notation: 7.1.0 ≤ version < 8.0.0) |
| `~7.1.0` | Approximately 7.1.0 (npm notation: 7.1.0 ≤ version < 7.2.0) |

## Practical Example: Project Versioning

Here's how Semantic Versioning works in practice, using this project as an example:

- **Current Version:** 0.0.1-SNAPSHOT (as defined in `pom.xml`)
- **SNAPSHOT suffix:** Indicates a development version (not yet released)

### Typical Version History:

- `0.0.1-SNAPSHOT` → Current development version
- `0.1.0` → First minor release with basic features
- `0.1.1` → Bug fix release
- `0.2.0` → New feature added (AI model support)
- `1.0.0` → First stable release with complete API
- `1.0.1` → Bug fix for stable release
- `1.1.0` → New feature added to stable version
- `2.0.0` → Major refactoring with breaking changes

## Pre-release Versions

You might also see:
- `1.0.0-alpha` → Alpha release (early testing)
- `1.0.0-beta` → Beta release (feature complete, testing)
- `1.0.0-rc.1` → Release Candidate (ready for release)
- `1.0.0-SNAPSHOT` → Development/snapshot build

## Best Practices

1. **Always specify versions explicitly** when possible
2. **Use version ranges carefully** to balance stability and updates
3. **Read release notes** before updating major versions
4. **Test after updates**, especially for major version changes
5. **Pin versions** in production for maximum stability

## Resources

- [Semantic Versioning Specification](https://semver.org/)
- [Maven Versioning](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html)
- [NPM Versioning](https://docs.npmjs.com/about-semantic-versioning)

---

For questions about QueryMind versioning, please open an issue in the repository.
