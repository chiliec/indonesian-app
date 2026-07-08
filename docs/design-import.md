# Design import

## Visual reference

The Lancar visual reference lives in a Claude Design project:

- **Project ID:** `47fb9cbc-d3eb-4f17-9138-05ba6b535434`
- **File:** `Lancar iOS App.dc.html`

## How to import

The design file is accessed via the `claude_design` MCP server:

1. Run `/design-login` in Claude Code to authenticate.
2. Connect the MCP endpoint: `https://api.anthropic.com/v1/design/mcp`
3. Import the project by its ID (`47fb9cbc-d3eb-4f17-9138-05ba6b535434`).
4. Open `Lancar iOS App.dc.html` to browse color and type tokens.

## Applying tokens to the app

The entry point for theming is `composeApp/src/commonMain/kotlin/com/axveer/lancar/ui/App.kt`. The `App` composable wraps all content in `MaterialTheme`. Color and typography overrides go here as a custom `MaterialTheme(colorScheme = ..., typography = ...)` call derived from the design tokens.

## v1 scope note

v1 ships a neutral Material 3 theme (no token overrides). The design import step is deferred — the visual reference guides layout intent and color direction, but the functionality is intentionally rethought for the KMP context rather than ported 1:1 from the iOS mockup.
