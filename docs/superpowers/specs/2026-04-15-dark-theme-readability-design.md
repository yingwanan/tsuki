# Dark Theme Readability Refresh

## Goal

Refactor the Android app's dark mode into a black-and-white dominant system with a restrained cool-gray accent so the UI reads clearly, feels consistent with the product theme, and removes the current washed-out translucent look.

## Problems Observed

- Dark mode currently mixes tinted primary colors with semi-transparent surfaces, which makes hierarchy look muddy.
- Text contrast is inconsistent, especially for secondary labels and descriptive copy.
- Shared containers such as cards, navigation chrome, and headers depend on alpha overlays instead of stable depth tokens.
- Background imagery competes with content instead of staying subordinate.

## Success Criteria

- Dark mode uses a stable charcoal background with opaque dark surfaces and clear separation between levels.
- Body text, labels, metadata, and hints become easier to read without needing brighter saturated colors.
- Navigation, cards, editor panes, settings panels, and config/deployment screens all share the same dark token system.
- Accent usage is reduced to focused states, selected states, and lightweight separators or highlights.

## Design Direction

### Palette

- Base background: near-black charcoal.
- Surface levels: 2 to 3 opaque dark grays with clear luminance differences.
- Primary text: near-white.
- Secondary text: cool light gray with enough contrast for metadata and helper copy.
- Accent: a single restrained cool gray-blue used sparingly for selected and focused states.
- Dividers and outlines: low-saturation cool gray lines.

### Visual Rules

- Remove translucent glass-like panels in dark mode.
- Prefer contrast through opaque containers and borders, not alpha stacking.
- Keep imagery dimmed and subordinate to content.
- Selected states should feel precise, not glowing.
- Error messaging stays distinct, but the rest of the UI avoids multicolor emphasis.

## Implementation Scope

### Theme Layer

- Rework dark color tokens in `ui/theme/Color.kt`.
- Rebuild `darkColorScheme` in `ui/theme/Theme.kt` around the new palette.
- Add or update token tests to enforce opacity and distinct contrast boundaries.

### Shared Chrome

- Update `MizukiWriterApp.kt` background treatment, image overlay, and bottom navigation styling.
- Update `PrimaryScreenScaffold.kt` and `PrimaryScreenHeader.kt` to use stable opaque dark surfaces instead of translucent backgrounds.

### Screen Surfaces

- Replace `surface.copy(alpha = ...)` usage across content, editor, settings, config, dashboard, posts, deployments, and repository file screens.
- Align card, panel, and list item containers to the new surface levels.
- Rebalance title, body, label, helper, and metadata colors to improve readability.

### Interaction Styling

- Normalize selected navigation item treatment.
- Keep focus, active, and selected states on the cool accent without flooding containers.
- Retain error visibility while keeping the rest of the theme monochrome-led.

## Data Flow and Risk

- No business logic or persistence changes are intended.
- Existing user-selected background images remain supported, but the overlay treatment will be adjusted.
- The main implementation risk is visual inconsistency from partially migrated containers; mitigation is to sweep all dark surface usage patterns rather than patch isolated screens.
- Another risk is overwriting current in-progress theme work in the dirty worktree; mitigation is to inspect and merge with existing local changes before editing each file.

## Testing Plan

- Add or extend tests around theme surface opacity and token separation.
- Run targeted JVM tests for theme token checks.
- Run at least one app build or equivalent verification covering Compose compilation after the theme update.
- Perform a code sweep for old translucent dark-surface patterns to ensure the migration is complete.

## Out of Scope

- Light theme redesign.
- Typography redesign beyond contrast-related adjustments.
- New feature work or navigation changes unrelated to dark theme readability.
