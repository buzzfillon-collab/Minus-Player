# Contributing

Minus Player is being built with a focus on playback reliability and maintainable Android architecture.

## Development principles

1. Keep playback logic independent from UI.
2. Prefer small, testable components over monolithic classes.
3. Treat large-screen/windowed Android as a first-class form factor.
4. Add regression tests for playback and media-library bugs.
5. Do not claim codec support without testing the relevant decoder path.
