# Contributing to GhostOS

First off, thank you for considering contributing to GhostOS. This is a research project pushing the boundaries of Android's permission model.

## ⚠️ Important Notice

GhostOS operates in a legal and technical gray area. By contributing, you acknowledge:
- You understand the Device Owner and LSPosed mechanisms
- Your code will run with UID 1000 privileges
- You're comfortable with advanced Android internals

## Development Setup

1. Fork and clone the repository
2. Open in Android Studio Hedgehog or later
3. Install LSPosed on test device
4. Enable Device Owner via ADB for testing

## Code Style

- Kotlin: Follow official Kotlin coding conventions
- C/C++: Follow Android NDK style guide
- Commit messages: Conventional Commits format

## Pull Request Process

1. Update README/CHANGELOG if applicable
2. Test on physical device (not just emulator)
3. Document any new hooks or injection points
4. Ensure SafetyNet implications are noted

## Areas Needing Help

| Area | Priority | Skills Needed |
|------|----------|---------------|
| Point Blank native hooks | 🔴 High | C, ptrace, ARM assembly |
| ART method patching | 🔴 High | Android runtime internals |
| SELinux context handling | 🟡 Medium | Android security |
| UI/UX polish | 🟢 Low | Material Design, animations |
| Documentation | 🟢 Low | Technical writing |

## Questions?

Open an issue with the "question" label.
