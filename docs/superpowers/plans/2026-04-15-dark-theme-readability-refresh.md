# Dark Theme Readability Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current muddy dark mode with a black-and-white-led palette that uses opaque surfaces, stronger text contrast, and a restrained cool accent across all major Compose screens.

**Architecture:** Rebuild the dark-mode color tokens first, then thread those tokens through shared app chrome and every screen still using translucent dark containers. Keep business logic untouched and limit the work to theme, presentation, and readability-related styling so verification can focus on theme token tests and Compose compilation.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, JUnit, Gradle

---

### Task 1: Lock Theme Expectations With Tests

**Files:**
- Modify: `app/src/test/java/com/blogmd/mizukiwriter/ui/theme/SurfaceStyleTokensTest.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/theme/Color.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/theme/Theme.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
@Test
fun `dark theme uses opaque layered surfaces and brighter primary text`() {
    assertThat(CardSurfaceDark.alpha).isEqualTo(1f)
    assertThat(CardSurfaceDark).isNotEqualTo(BackgroundDark)
    assertThat(TextLight.luminance()).isGreaterThan(TextSecondaryLight.luminance())
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.blogmd.mizukiwriter.ui.theme.SurfaceStyleTokensTest`
Expected: FAIL because the current dark tokens do not yet enforce the final contrast and layering rules.

- [ ] **Step 3: Write minimal implementation**

```kotlin
val BackgroundDark = Color(0xFF0C0D0F)
val CardSurfaceDark = Color(0xFF17191D)
val TextLight = Color(0xFFF5F7FA)
val TextSecondaryLight = Color(0xFFB6BDC8)
```

Update `DarkColors` so `background`, `surface`, `surfaceVariant`, `outline`, `primary`, `onPrimary`, `onSurface`, and `onSurfaceVariant` reflect the new dark palette.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.blogmd.mizukiwriter.ui.theme.SurfaceStyleTokensTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/test/java/com/blogmd/mizukiwriter/ui/theme/SurfaceStyleTokensTest.kt app/src/main/java/com/blogmd/mizukiwriter/ui/theme/Color.kt app/src/main/java/com/blogmd/mizukiwriter/ui/theme/Theme.kt
git commit -m "refactor: rebuild dark theme tokens"
```

### Task 2: Rebuild Shared App Chrome Around Opaque Dark Surfaces

**Files:**
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/MizukiWriterApp.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/components/PrimaryScreenScaffold.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/components/PrimaryScreenHeader.kt`
- Test: `app/src/test/java/com/blogmd/mizukiwriter/ui/theme/SurfaceStyleTokensTest.kt`

- [ ] **Step 1: Write the failing test**

Add one assertion that the shared dark surfaces remain opaque after the token update, for example by checking the dark navigation/container token values exposed through `Color.kt`.

```kotlin
@Test
fun `shared dark containers stay opaque`() {
    assertThat(GlassDark.alpha).isEqualTo(1f)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.blogmd.mizukiwriter.ui.theme.SurfaceStyleTokensTest`
Expected: FAIL because the current shared container color still depends on translucency.

- [ ] **Step 3: Write minimal implementation**

- Replace the background-image overlay in `MizukiWriterApp.kt` with a darker, more stable dimming treatment.
- Replace the bottom navigation surface color from `surface.copy(alpha = 0.85f)` to an opaque container token.
- Update selected navigation colors to use the restrained cool accent for text/icon and a subtle accent-tinted indicator.
- Replace the translucent top-bar background in `PrimaryScreenScaffold.kt` with an opaque container or background token.
- Align `PrimaryScreenHeader.kt` title treatment with the stronger dark text hierarchy.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.blogmd.mizukiwriter.ui.theme.SurfaceStyleTokensTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/blogmd/mizukiwriter/ui/MizukiWriterApp.kt app/src/main/java/com/blogmd/mizukiwriter/ui/components/PrimaryScreenScaffold.kt app/src/main/java/com/blogmd/mizukiwriter/ui/components/PrimaryScreenHeader.kt app/src/test/java/com/blogmd/mizukiwriter/ui/theme/SurfaceStyleTokensTest.kt
git commit -m "refactor: restyle dark app chrome"
```

### Task 3: Migrate Screen Containers Off Alpha-Based Dark Surfaces

**Files:**
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/feature/posts/PostsRoute.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/feature/editor/EditorRoute.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/feature/settings/SettingsRoute.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/feature/dashboard/DashboardRoute.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/feature/config/ConfigRoute.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/feature/config/ConfigHubRoute.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/feature/config/ConfigFileRoute.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/feature/config/ConfigDetailsRoute.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/feature/deployments/DeploymentsRoute.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/feature/deployments/DeploymentSettingsRoute.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/feature/deployments/DeploymentGuideRoute.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/feature/repositoryfile/RepositoryFileRoute.kt`
- Modify: `app/src/main/java/com/blogmd/mizukiwriter/ui/feature/content/ContentRoute.kt`

- [ ] **Step 1: Write the failing test**

Extend the theme token test with a guard that all shared dark surface tokens used by cards remain fully opaque.

```kotlin
@Test
fun `dark card tokens never rely on alpha overlays`() {
    assertThat(CardSurfaceDark.alpha).isEqualTo(1f)
    assertThat(SwipeCardMaskDark.alpha).isEqualTo(1f)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests com.blogmd.mizukiwriter.ui.theme.SurfaceStyleTokensTest`
Expected: FAIL until the token and surface usage are aligned with the new dark system.

- [ ] **Step 3: Write minimal implementation**

- Replace every `MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)` card color with an opaque dark surface token or `MaterialTheme.colorScheme.surface` / `surfaceVariant` choice that matches the new hierarchy.
- Review nearby text calls on the same screens and move secondary metadata to `onSurfaceVariant` where it improves readability.
- Keep error and status messaging distinct without reintroducing saturated container backgrounds.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests com.blogmd.mizukiwriter.ui.theme.SurfaceStyleTokensTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/blogmd/mizukiwriter/ui/feature/posts/PostsRoute.kt app/src/main/java/com/blogmd/mizukiwriter/ui/feature/editor/EditorRoute.kt app/src/main/java/com/blogmd/mizukiwriter/ui/feature/settings/SettingsRoute.kt app/src/main/java/com/blogmd/mizukiwriter/ui/feature/dashboard/DashboardRoute.kt app/src/main/java/com/blogmd/mizukiwriter/ui/feature/config/ConfigRoute.kt app/src/main/java/com/blogmd/mizukiwriter/ui/feature/config/ConfigHubRoute.kt app/src/main/java/com/blogmd/mizukiwriter/ui/feature/config/ConfigFileRoute.kt app/src/main/java/com/blogmd/mizukiwriter/ui/feature/config/ConfigDetailsRoute.kt app/src/main/java/com/blogmd/mizukiwriter/ui/feature/deployments/DeploymentsRoute.kt app/src/main/java/com/blogmd/mizukiwriter/ui/feature/deployments/DeploymentSettingsRoute.kt app/src/main/java/com/blogmd/mizukiwriter/ui/feature/deployments/DeploymentGuideRoute.kt app/src/main/java/com/blogmd/mizukiwriter/ui/feature/repositoryfile/RepositoryFileRoute.kt app/src/main/java/com/blogmd/mizukiwriter/ui/feature/content/ContentRoute.kt app/src/test/java/com/blogmd/mizukiwriter/ui/theme/SurfaceStyleTokensTest.kt
git commit -m "refactor: unify dark screen surfaces"
```

### Task 4: Verify the Dark Theme Migration End to End

**Files:**
- Modify: `verification.md`
- Review: `docs/superpowers/specs/2026-04-15-dark-theme-readability-design.md`
- Review: `docs/superpowers/plans/2026-04-15-dark-theme-readability-refresh.md`

- [ ] **Step 1: Run targeted theme tests**

Run: `./gradlew testDebugUnitTest --tests com.blogmd.mizukiwriter.ui.theme.SurfaceStyleTokensTest`
Expected: PASS

- [ ] **Step 2: Run broader Compose/unit verification**

Run: `./gradlew testDebugUnitTest`
Expected: PASS or a documented unrelated failure outside the dark-theme scope.

- [ ] **Step 3: Run build verification**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Sweep for old translucent surface patterns**

Run: `grep -RIn --exclude-dir=node_modules --exclude-dir=.next --exclude-dir=dist --exclude-dir=build "surface.copy(alpha" app/src/main/java`
Expected: no remaining dark-surface alpha copies, or only intentional non-theme cases that are documented.

- [ ] **Step 5: Record verification and commit**

```bash
git add verification.md
git commit -m "docs: record dark theme verification"
```
