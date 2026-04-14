# Security Policy

## Supported Versions

| Version | Supported          |
|---------|-------------------|
| 0.1.x   | ✅ Active development |
| < 0.1   | ❌ Pre-release only |

## Reporting a Vulnerability

**DO NOT OPEN A PUBLIC ISSUE** for security vulnerabilities.

Instead, email: [your email]

Please include:
- Detailed description of the vulnerability
- Steps to reproduce
- Affected Android versions
- Potential impact

You will receive a response within 48 hours.

## Security Philosophy

GhostOS is a **research tool** demonstrating that:
1. Device Owner + LSPosed = UID 1000 inheritance
2. SystemServer `fork()` spawns privileged processes
3. Android's permission model can be bypassed without exploits

We disclose capabilities transparently so Google can patch if necessary.

## Known Limitations

- LSPosed detection by STRONG_INTEGRITY
- SELinux restrictions on some OEM ROMs
- Samsung Knox additional protections

## Responsible Disclosure

We follow Google's 90-day disclosure policy for any discovered vulnerabilities.
