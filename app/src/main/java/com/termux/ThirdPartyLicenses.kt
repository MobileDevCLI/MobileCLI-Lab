package com.termux

/**
 * ThirdPartyLicenses - Displays open source licenses for third-party components
 *
 * This satisfies legal requirements for:
 * - Apache 2.0 (attribution requirement)
 * - GPLv3 (source code availability requirement)
 */
object ThirdPartyLicenses {

    /**
     * Get the full third-party license text for display in the app
     */
    fun getLicenseText(): String = """
THIRD-PARTY SOFTWARE LICENSES
═══════════════════════════════════════════════════════════════════════

MobileCLI includes the following open-source components:

───────────────────────────────────────────────────────────────────────
TERMINAL EMULATOR FOR ANDROID
───────────────────────────────────────────────────────────────────────

Copyright (c) 2011-2023 Jack Palevich and contributors
Licensed under the Apache License, Version 2.0

The terminal-view and terminal-emulator libraries provide the core
terminal emulation functionality.

Source Code: https://github.com/jackpal/Android-Terminal-Emulator
License: http://www.apache.org/licenses/LICENSE-2.0

───────────────────────────────────────────────────────────────────────
TERMUX TERMINAL LIBRARIES
───────────────────────────────────────────────────────────────────────

Copyright (c) 2016-2024 Fredrik Fornwall and Termux Contributors
Licensed under the Apache License, Version 2.0

Modified terminal libraries maintained by the Termux project.

Source Code: https://github.com/termux/termux-app
License: http://www.apache.org/licenses/LICENSE-2.0

───────────────────────────────────────────────────────────────────────
TERMUX-AM (ACTIVITY MANAGER)
───────────────────────────────────────────────────────────────────────

Copyright (c) 2020-2024 Termux Contributors
Licensed under the GNU General Public License v3.0 (GPLv3)

This component enables launching Android activities from the terminal.
It is distributed as a separate binary program that communicates with
MobileCLI via inter-process communication.

*** IMPORTANT: Source Code Availability ***

As required by the GPLv3 license, the complete source code for
termux-am is freely available at:

    https://github.com/termux/termux-am

You may obtain a copy of the GPLv3 license at:
    https://www.gnu.org/licenses/gpl-3.0.html

You have the right to:
- Use this software for any purpose
- Study how the software works and modify it
- Redistribute copies
- Distribute modified versions

───────────────────────────────────────────────────────────────────────
ANDROIDX LIBRARIES
───────────────────────────────────────────────────────────────────────

Copyright (c) The Android Open Source Project
Licensed under the Apache License, Version 2.0

Components: core-ktx, appcompat, material, constraintlayout, lifecycle

Source Code: https://android.googlesource.com/platform/frameworks/support
License: http://www.apache.org/licenses/LICENSE-2.0

───────────────────────────────────────────────────────────────────────
KOTLIN & KOTLIN COROUTINES
───────────────────────────────────────────────────────────────────────

Copyright (c) JetBrains s.r.o.
Licensed under the Apache License, Version 2.0

Source Code: https://github.com/JetBrains/kotlin
License: http://www.apache.org/licenses/LICENSE-2.0

───────────────────────────────────────────────────────────────────────
BOOTSTRAP PACKAGES
───────────────────────────────────────────────────────────────────────

The Linux packages (bash, coreutils, nodejs, python, git, etc.) that
you download through MobileCLI are provided by the Termux project and
are subject to their individual licenses (primarily GPL, MIT, BSD).

These packages are NOT bundled with MobileCLI - they are downloaded
at runtime from Termux package repositories at your request.

Package Repository: https://packages.termux.dev
Package Sources: https://github.com/termux/termux-packages

═══════════════════════════════════════════════════════════════════════
                    APACHE LICENSE, VERSION 2.0
═══════════════════════════════════════════════════════════════════════

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

═══════════════════════════════════════════════════════════════════════
                     GNU GENERAL PUBLIC LICENSE
                        Version 3, 29 June 2007
═══════════════════════════════════════════════════════════════════════

The complete text of the GPLv3 license is available at:
https://www.gnu.org/licenses/gpl-3.0.html

Key provisions:
- You may copy, distribute and modify the software as long as you
  track changes/dates in source files
- Any modifications to GPL code must also be made available under GPL
- You can use GPL software in commercial applications
- If you distribute binaries, you must make source available

For termux-am, the source is available at:
https://github.com/termux/termux-am

═══════════════════════════════════════════════════════════════════════

MobileCLI © 2026 MobileDevCLI. All rights reserved.

MobileCLI's own source code (8,825 lines of Kotlin) is proprietary
and NOT covered by the above open-source licenses.

For questions about licensing, contact: mobiledevcli@gmail.com

""".trimIndent()

    /**
     * Get a shorter summary for quick display
     */
    fun getLicenseSummary(): String = """
MobileCLI uses open-source components:

• Terminal Emulator (Apache 2.0)
• Termux Libraries (Apache 2.0)
• termux-am (GPLv3) - Source: github.com/termux/termux-am
• AndroidX (Apache 2.0)
• Kotlin (Apache 2.0)

Tap to view full license details.
""".trimIndent()

    /**
     * URLs for source code access (for GPL compliance)
     */
    object SourceUrls {
        const val TERMUX_AM = "https://github.com/termux/termux-am"
        const val TERMINAL_EMULATOR = "https://github.com/jackpal/Android-Terminal-Emulator"
        const val TERMUX_APP = "https://github.com/termux/termux-app"
        const val TERMUX_PACKAGES = "https://github.com/termux/termux-packages"
    }
}
