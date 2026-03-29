# Sliide User Manager — KMP "UX Innovator" Challenge

A high-fidelity, cross-platform User Management System built with **Kotlin Multiplatform** and **Compose Multiplatform**, interfacing with the [GoRest Public API](https://gorest.co.in/).

## 🎬 Features

| Feature | Implementation |
|---|---|
| **Smart User Feed** | Fetches the last page of `/users`, displays Name, Email, and relative timestamps ("5 minutes ago") computed in shared Kotlin logic |
| **Shimmer Loading** | Custom shimmer skeleton screens mirroring the exact card layout |
| **Error Handling** | Typed `AppException` hierarchy with dedicated "No Internet" UI, retry actions, and Snackbar feedback |
| **Add User (FAB)** | Floating Action Button → polished dialog with real-time email/name regex validation |
| **Delete + Undo** | Long-press → confirmation dialog → animated removal → Snackbar with "Undo" that restores local state |
| **Adaptive Layout** | Portrait: single-column list · Landscape/Tablet (>600dp): master-detail two-pane layout |
| **Offline Support** | SQLDelight local cache; app renders cached data immediately, syncs when network is available |
| **Dark Mode** | Full Material 3 dynamic color scheme with light/dark support |

## 🏗 Architecture

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

## 🧰 Tech Stack

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

## 🧪 Testing

Tests live in `shared/src/commonTest/` and cover:

- **`RelativeTimeFormatterTest`** — 18 test cases covering all time buckets (seconds → years), boundary conditions, and edge cases (future timestamps)
- **`ValidationUtilsTest`** — 22 test cases for name and email validation including edge cases (special characters, Unicode, length boundaries)
- **`UserListViewModelTest`** — 14 test cases for MVI state transitions, intent handling, error propagation, and form validation flow

Run tests:
```bash
./gradlew :shared:allTests
```

## 🚀 Setup & Run

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

## 🤖 AI-Assisted Development

This project was built with AI assistance (Claude) to accelerate development while maintaining architectural quality. Here's how AI was leveraged:

### What AI Generated
- **Boilerplate scaffolding**: Gradle configuration, module structure, `expect/actual` declarations
- **Regex patterns**: Email and name validation regex with comprehensive edge case coverage
- **Test cases**: AI generated exhaustive test cases including boundary conditions I might have missed (e.g., 254-char email limit, Unicode name support)
- **Shimmer animation**: The shimmer loading effect with proper `InfiniteTransition` usage
- **Mapper functions**: DTO → Entity → Domain model mappings

### What I Curated & Directed
- **Architecture decisions**: MVI over MVVM, offline-first strategy, single-state ViewModel pattern
- **Layer boundaries**: Strict separation — the domain layer has zero dependencies on data/UI
- **Error handling strategy**: Typed `AppException` hierarchy instead of raw strings
- **UX flow**: Undo-delete pattern using local-only restore (no API re-creation)
- **Adaptive layout**: BoxWithConstraints breakpoint at 600dp for master-detail
- **Code review**: Removed unnecessary complexity, ensured consistent naming, verified thread safety

### AI Productivity Impact
- **Time saved**: ~60% reduction in boilerplate writing time
- **Quality boost**: AI-generated test cases caught edge cases (future timestamps, Unicode names) that manual testing might miss
- **Focus shift**: Spent more time on architecture, UX polish, and code review rather than syntax
