# Sliide User Manager — KMP "UX Innovator" Challenge

A high-fidelity, cross-platform User Management System built with **Kotlin Multiplatform** and **Compose Multiplatform**, interfacing with the [GoRest Public API](https://gorest.co.in/).

## Features

| Feature | Implementation |
|---|---|
| **Smart User Feed** | Fetches the last page of `/users`, displays Name, Email, and relative timestamps ("5 minutes ago") computed in shared Kotlin logic |
| **Shimmer Loading** | Custom shimmer skeleton screens mirroring the exact card layout |
| **Error Handling** | Typed `AppException` hierarchy with dedicated "No Internet" UI (Material icon + retry), Snackbar feedback for API errors |
| **Add User (FAB)** | Floating Action Button (hides on scroll) → polished dialog with real-time email/name regex validation. On success, list scrolls to the new user with a shake highlight animation |
| **Delete + Undo** | Long-press or swipe-to-delete (with red background reveal) → confirmation dialog → animated removal → Snackbar with "Undo" that restores local state |
| **Swipe Hint** | First card plays a subtle peek animation on load to teach users the swipe-to-delete gesture |
| **Adaptive Layout** | Portrait: single-column list with bottom sheet user details · Landscape/Tablet (>600dp): master-detail two-pane layout |
| **Offline Support** | SQLDelight local cache; app renders cached data immediately, syncs when network is available |
| **Dark Mode** | Full Material 3 dynamic color scheme with light/dark support |
| **UX Polish** | Staggered list entry animations, Material icons throughout, rounded cards with depth, scroll-aware FAB |

## Architecture

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                       │
│  Compose Multiplatform (100% shared)            │
│  ┌─────────────┐  ┌────────────────────┐        │
│  │ UserListScreen│  │ Components (Cards, │        │
│  │ (Adaptive)   │  │ Shimmer, Dialogs)  │        │
│  └──────┬───────┘  └────────────────────┘        │
│         │                                        │
│  ┌──────▼────────────────────────────┐           │
│  │ UserListViewModel (MVI)           │           │
│  │ State → StateFlow<UserListState>  │           │
│  │ Effects → SharedFlow<Effect>      │           │
│  │ Intents → onIntent(Intent)        │           │
│  └──────┬────────────────────────────┘           │
├─────────┼────────────────────────────────────────┤
│         │       Domain Layer                     │
│  ┌──────▼───────┐  ┌──────────────────┐          │
│  │ Use Cases     │  │ Repository       │          │
│  │ (Observe,     │  │ Interface        │          │
│  │  Create,      │  │ (contract only)  │          │
│  │  Delete,      │  └──────────────────┘          │
│  │  Restore)     │  ┌──────────────────┐          │
│  └───────────────┘  │ Validation,      │          │
│                     │ RelativeTime     │          │
│                     └──────────────────┘          │
├──────────────────────────────────────────────────┤
│                  Data Layer                      │
│  ┌──────────────┐  ┌───────────────────┐         │
│  │ GoRestApi     │  │ LocalDataSource   │         │
│  │ (Ktor HTTP)   │  │ (SQLDelight)      │         │
│  └──────┬────────┘  └───────┬───────────┘         │
│         │                   │                     │
│  ┌──────▼───────────────────▼────────┐            │
│  │ UserRepositoryImpl                │            │
│  │ (offline-first, sync strategy)    │            │
│  └───────────────────────────────────┘            │
├──────────────────────────────────────────────────┤
│  Platform (expect/actual)                        │
│  Android: AndroidSqliteDriver, OkHttp engine     │
│  iOS: NativeSqliteDriver, Darwin engine          │
└──────────────────────────────────────────────────┘
```

### Pattern: **MVI (Model-View-Intent)**

The ViewModel exposes:
- **`state: StateFlow<UserListState>`** — a single immutable state object that the UI observes
- **`effects: SharedFlow<UserListEffect>`** — one-shot events (Snackbar, navigation) that don't belong in state
- **`onIntent(intent: UserListIntent)`** — the single entry point for all user actions

This ensures unidirectional data flow and makes the ViewModel trivially testable.

### Offline-First Strategy

1. **`observeUsers()`** always reads from SQLDelight (reactive `Flow`)
2. **`refreshUsers()`** fetches from the API, then replaces the local cache
3. **Writes** (create/delete) hit the API first, then sync locally on success
4. **Undo** restores local state without an API call

## Tech Stack

| Layer | Technology |
|---|---|
| **UI** | Compose Multiplatform (Material 3) |
| **Architecture** | MVI with shared ViewModel |
| **Networking** | Ktor (OkHttp on Android, Darwin on iOS) |
| **Persistence** | SQLDelight with `expect/actual` drivers |
| **DI** | Koin (shared + platform modules) |
| **Async** | Kotlin Coroutines + Flow |
| **Time** | kotlinx-datetime (shared relative timestamps) |
| **Testing** | kotlin-test, Turbine, Ktor MockEngine |

## Video Demo

https://github.com/user-attachments/assets/a5b94e08-de46-4d78-8fcf-e611ab357914

## Testing

Tests live in `shared/src/commonTest/` and cover:

- **`RelativeTimeFormatterTest`** — 18 test cases covering all time buckets (seconds → years), boundary conditions, and edge cases (future timestamps)
- **`ValidationUtilsTest`** — 22 test cases for name and email validation including edge cases (special characters, Unicode, length boundaries)
- **`UserListViewModelTest`** — 23 test cases for MVI state transitions, intent handling, delete/undo flows, create-user error handling, and form validation

Run tests:
```bash
./gradlew :shared:allTests
```

## Setup & Run

### Prerequisites
- Android Studio Hedgehog+ with KMP plugin
- Xcode 15+ (for iOS)
- GoRest API token from [gorest.co.in](https://gorest.co.in/consumer/login)

### Configuration
1. Get your GoRest API token
2. Replace `YOUR_GOREST_TOKEN_HERE` in `GoRestApiService.kt` with your actual token

### Run Android
```bash
./gradlew :androidApp:installDebug
```

### Run iOS
Open `iosApp/iosApp.xcodeproj` in Xcode and run on a simulator.

## 🤖 AI Orchestration Approach

This project demonstrates how a senior engineer can use AI as a **force multiplier** — not by accepting raw output, but by directing it with clear architectural intent and reviewing every line against production standards.

### Philosophy: Architect First, Generate Second

Every AI interaction followed the same discipline:

1. **I defined the contract** — interfaces, layer boundaries, data flow direction — before asking AI to generate any implementation
2. **AI filled in the implementation** — boilerplate, mappers, regex patterns, animation code
3. **I reviewed, rejected, or refined** — catching issues like a hardcoded `AnimatedVisibility(visible = true)` that could never animate, or a missing `encodeDefaults` flag that silently dropped JSON fields

The architecture was never AI-suggested. MVI over MVVM, offline-first with API-first writes, typed `AppException` over raw strings, `expect/actual` for platform drivers — these were deliberate choices made before any code was generated.

### What I Directed vs. What AI Produced

| My Role (Architecture & Direction) | AI's Role (Implementation) |
|---|---|
| Chose MVI with single immutable state + SharedFlow effects | Generated the `UserListState`, `UserListIntent`, `UserListEffect` boilerplate |
| Designed offline-first sync strategy (observe local, refresh from API, write-through) | Implemented `UserRepositoryImpl` with the strategy I specified |
| Specified RFC 5322 email validation with Unicode name support | Produced the regex patterns and `ValidationUtils` with edge cases |
| Decided on `SwipeToDismissBox` + peek hint to teach the gesture | Implemented the `Animatable`-based peek animation and dismiss wiring |
| Required typed error hierarchy mapping Ktor exceptions to user-facing messages | Generated `AppException` sealed class and the `toAppException()` mapper |
| Chose `ModalBottomSheet` for portrait detail (over navigation or dialog) | Wired the bottom sheet to `selectedUser` state with dismiss handling |
| Defined the test boundaries: ViewModel state transitions, validation edge cases, time formatting | Generated exhaustive test cases including boundaries I specified (254-char email, future timestamps, delete+undo flows) |

### Quality Gates I Applied

AI output was never committed without review. Key interventions:

- **Fixed broken animation**: AI generated `AnimatedVisibility(visible = true)` which was a no-op — replaced with `Modifier.animateItem()` which actually animates LazyColumn insertions/removals
- **Fixed silent API failure**: `CreateUserRequest.status` had a default value that `kotlinx.serialization` silently omitted — added `encodeDefaults = true`
- **Fixed error visibility**: Create-user errors showed a Snackbar behind the open dialog — added dialog dismissal on error path
- **Parsed API errors**: Original code threw `"Failed to create user: 422"` — rewrote to parse GoRest's error response body for actionable messages like `"email: has already been taken"`
- **Removed dead code**: Cleaned up unused imports, unreachable animation code, and redundant wrappers

### Productivity Impact

| Metric | Without AI | With AI Orchestration |
|---|---|---|
| **Boilerplate** (Gradle, DI, mappers, drivers) | Hours of manual setup | Minutes — generated to my spec, reviewed once |
| **Test coverage** | Would likely write ~15 tests | 63 tests covering edge cases I specified + AI-suggested boundaries |
| **UX polish** (animations, gestures, transitions) | Separate spike/iteration cycle | Implemented inline — described the interaction, reviewed the output |
| **Time allocation** | ~70% writing code, ~30% designing | ~30% writing/reviewing, ~70% architecting and directing |

The key insight: AI didn't make architectural decisions — it eliminated the gap between having a design and having working code. Every pattern, boundary, and UX choice was mine; the implementation velocity was AI's contribution.
