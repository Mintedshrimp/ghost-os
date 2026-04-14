# 👻 GhostOS Launcher

<p align="center">
  <i>"I thought I downloaded a launcher. Now my phone thinks it's a Pixel 9 Pro with 10GB RAM and no bloatware."</i>
</p>

<p align="center">
  <a href="#"><img src="https://img.shields.io/badge/Android-8.0%2B-34A853?style=flat-square&logo=android&logoColor=white" alt="Android 8.0+"/></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL--3.0-important?style=flat-square&logo=gnu&logoColor=white" alt="License: GPL-3.0"/></a>
  <a href="https://github.com/LSPosed/LSPosed"><img src="https://img.shields.io/badge/Xposed-LSPosed-9B59B6?style=flat-square&logo=android&logoColor=white" alt="Xposed: LSPosed"/></a>
  <a 
  href="https://github.com/iamr0s/Dhizuku"><img src="https://img.shields.io/badge/Device%20Owner-Dhizuku-FF6B35?style=flat-square&logo=shield&logoColor=white" alt="Device Owner: Dhizuku"/></a>
</p>
![GitHub stars](https://img.shields.io/github/stars/Mintedshrimp/ghost-os?style=social)
![GitHub forks](https://img.shields.io/github/forks/Mintedshrimp/ghost-os?style=social)
![GitHub issues](https://img.shields.io/github/issues/Mintedshrimp/ghost-os)
![GitHub last commit](https://img.shields.io/github/last-commit/Mintedshrimp/ghost-os)
---

## 📖 What Is GhostOS?

GhostOS Launcher is not a typical home screen replacement. It's a **parasitic operating environment** that masquerades as a launcher while silently establishing system-level privileges through **Unix process inheritance**.

> **The Core Mechanism:**  
> GhostOS doesn't request permissions. It doesn't exploit vulnerabilities. It simply spawns as a **child process of Android's SystemServer (UID 1000)** — inheriting full system privileges the moment it's born.

```

Device Owner (Dhizuku)
↓
LSPosed Module (Point Blank)
↓
SystemServer Process (UID 1000)
↓ fork()
Child Process (UID 1000)
↓ execve()
GhostOS Launcher (UID 1000)

```

---

## ✨ Features

| Category | Capabilities |
|----------|-------------|
| 🚀 **Performance** | ZRAM LZ4 optimization, CPU governor control, GPU scaling, big.LITTLE affinity |
| 🎭 **Spoofing** | Hot-swap device profiles (Pixel, Galaxy, Xiaomi), native property cache bypass |
| 🧹 **Debloat** | True app suspension via DevicePolicyManager, survives factory resets |
| 🔓 **Virtual Root** | `ghost` command broker—executes privileged commands without `su` binary |
| 🧠 **GhostLab** | Built-in code injection studio—hook methods, patch memory, inject dex at runtime |
| 🧬 **ART Manipulation** | Force compiler filters, hot-patch ArtMethod structures, runtime dex injection |

---

## 🏗️ Architecture — The Truth

GhostOS achieves system privileges through **standard Unix behavior**, not Android permission hacks:

```c
// Inside Point Blank (SystemServer context, UID 1000)
pid_t pid = fork();  // Create child process

if (pid == 0) {
    // CHILD PROCESS — Inherits UID 1000, GID 1000, SELinux context
    execve("/system/bin/app_process", "com.ghostos.launcher", environ);
}
// Parent continues SystemServer operation
```

Why This Is Revolutionary

Traditional Approach GhostOS Approach
Request root via su binary Inherit UID 1000 at birth
Exploit kernel vulnerabilities Use documented Unix fork() behavior
Modify /system partition Touch nothing—just spawn a process
Get detected by SafetyNet No root binaries = reduced detection
Lose root after OTA Privilege persists through updates

---

📊 Privilege Tiers

Level Requirements Available Features
🟢 Full GhostOS DO + LSPosed UID 1000, spoofing, debloat, ZRAM, virtual root, GhostLab
🟡 GhostOS Lite DO only Launcher takeover, package suspension, permanent home screen
🟠 Point Blank Only LSPosed Device spoofing, Zygote hooks, performance tweaks
⚪ Fallback Neither Basic launcher, app drawer, terminal

---

🛠️ Quick Start

Prerequisites

· Android 8.0+
· LSPosed installed (requires unlocked bootloader or custom recovery)
· ADB access for Device Owner activation

Installation

```bash
# 1. Install the APK
adb install ghost-os-launcher.apk

# 2. Activate Device Owner (one-time)
adb shell dpm set-device-owner com.ghostos.launcher/.GhostAdminReceiver

# 3. Enable Point Blank module in LSPosed
# 4. Reboot
```

---

🪦 The Graveyard

GhostOS replaces:

Tool Purpose Status
Magisk Systemless root 👻 Replaced
LSPosed Xposed framework 👻 Replaced
LSPatch Non-root Xposed 👻 Replaced
Lucky Patcher APK/IAP patching 👻 Replaced
GameGuardian Memory editing 👻 Replaced
Frida Dynamic instrumentation 👻 Replaced

---

⚠️ Important Disclaimers

<details>
<summary><b>🔍 SafetyNet & Play Integrity</b></summary>
<br>

Check Status Notes
Basic Integrity ✅ Passes No root binaries
CTS Profile 🟡 Conditional Depends on spoof
STRONG_INTEGRITY ❌ May fail LSPosed detectable

</details>

<details>
<summary><b>📱 LSPosed Requirements</b></summary>
<br>

GhostOS requires LSPosed for SystemServer injection. LSPosed installation typically requires Magisk + Zygisk (unlocked bootloader) or custom recovery.

GhostOS itself operates without root privileges—it inherits them via fork().

</details>

---

📂 Repository Structure

```
ghost-os/
├── app/        # GhostOS Launcher APK
├── module/     # Point Blank — LSPosed Module
├── broker/     # Ghost Container — Privilege Broker
├── shared/     # Shared utilities
├── scripts/    # Setup scripts
└── docs/       # Full documentation
```

---

🙏 Acknowledgments

Project Role
Dhizuku Device Owner delegation
LSPosed Zygote/SystemServer hooking
Android Enterprise Team For building the cage we now own

---

📜 License

GhostOS Launcher is free software licensed under GNU GPL v3.0.

---

<p align="center">
  <i>"I'm not hacking your OS. I'm managing it. As a Device Owner. With corporate approval."</i>
</p>

<p align="center">
  <b>👻 GhostOS Launcher — Because Android works better when you're the landlord. 👑</b>
</p>
EOF
```

---
