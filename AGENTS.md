> Your output will be reviewed by Codex when the work is done.

## Response Efficiency — CRITICAL

Minimize tokens in responses:
- No preamble: just do it, don't announce what you're about to do
- No summary: don't recap what you just did
- No meta-commentary: no "Great question!" or "Sure!"
- Unchanged code: abbreviate with `// ... existing code`
- Simple tasks = one sentence confirmation only

---

# ScalablyTyped Converter — AI Agent Context

**ScalablyTyped Converter** converts TypeScript definition files (`.d.ts`) into Scala.js facades, enabling use of JS libraries in Scala.js projects.

- **Stack:** Scala 2.12 + 3.3, SBT, scalafmt, parser-combinators, Circe, Coursier, os-lib
- **Docs:** [README.md](README.md), [CLAUDE.md](CLAUDE.md)

---

## Architecture

```
logging ← core ← ts/scalajs ← phases ← importer-portable ← importer/cli
                                                           ← sbt-converter (Scala 2.12 only)
```

### Module Responsibilities

| Module | Purpose |
|--------|---------|
| `logging` | Logging utilities (`com.olvind.logging`) |
| `core` | TypeScript AST (`TsTree`), Scala.js AST (`Tree`), shared utilities (`IArray`, `Name`) |
| `ts` | TypeScript lexer + parser (parser-combinators), TS-level transforms |
| `scalajs` | Scala.js AST transforms, flavour implementations (Normal/Slinky/Japgolly) |
| `phases` | `PhaseRes` monad, `PhaseRunner`, phase caching |
| `importer-portable` | Conversion pipeline: Phase1 → Phase2 → PhaseFlavour → Phase3 |
| `importer` | JVM runner (`Main`), snapshot tests, Coursier-based compiler |
| `cli` | CLI entry point with scopt argument parsing |
| `sbt-converter` | SBT plugin wrapping `importer-portable`; Scala 2.12 only |

### Conversion Pipeline

```
.d.ts files
    ↓ Phase1ReadTypescript  — parse, resolve modules, apply TS transforms
    ↓ Phase2ToScalaJs       — convert TS AST → Scala.js AST, apply Scala.js transforms
    ↓ PhaseFlavour          — apply flavour rewrites (Normal / Slinky / Japgolly)
    ↓ Phase3Compile         — compile generated Scala, publish artifacts
```

### TS Transforms (`ts/src/.../ts/transforms/`)
Applied in Phase1: `ExpandTypeMappings`, `InlineConstEnum`, `ResolveTypeLookups`, `SimplifyParents`, `MoveStatics`, `SplitMethods`, and more.

### Scala.js Transforms (`scalajs/src/.../scalajs/transforms/`)
Applied in Phase2: `CombineOverloads`, `CompleteClass`, `FakeLiterals`, `UnionToInheritance`, `ModulesCombine`, `LimitUnionLength`, and more.

### Flavours (`scalajs/src/.../scalajs/flavours/`)
- `NormalFlavour` — standard Scala.js facades
- `SlinkyFlavour` — React components for Slinky
- `JapgollyFlavour` — React components for scalajs-react
- `SlinkyNativeFlavour` — Slinky for React Native

---

## Patterns

| Concept | Pattern |
|---------|---------|
| Phase result | `PhaseRes[Source, Output]` — propagate via `.map`/`.flatMap`; never throw across phases |
| AST traversal | Visitor pattern: `visitPackageTree`, `visitClassTree`, etc. |
| Collections | `IArray[T]` instead of `Seq`/`List` in performance-critical code |
| Names | `TsIdent`/`Name` for simple names; `QualifiedName` for fully-qualified paths |
| Encoding: union types | Short → Scala `A \| B`; long → `UnionToInheritance` sealed trait |
| Encoding: literal types | `FakeLiterals` → sealed trait + companion `val` |
| Encoding: modules | TypeScript module → Scala package + `^` object for top-level members |
| Encoding: type mappings | Static → expanded inline; generic → commented out |
| Dependencies | All deps declared in `project/Deps.scala` |
| Formatting | scalafmt — run `sbt scalafmtAll` before finishing |

---

## AI Agent Rules

### Principles
1. Read existing similar code before writing new code
2. Use `PhaseRes` for phase results — never throw exceptions across phase boundaries
3. `IArray` is not a standard Scala collection — use its own API
4. Changing a transform in `ts/` or `scalajs/` may require snapshot test updates
5. `sbt-converter` depends only on `importer-portable`, not on JVM-specific `importer`
6. All new dependencies go into `project/Deps.scala`
7. Run `sbt scalafmtAll` before finishing

### Adding New Code
- **TS transform:** add to `ts/src/.../ts/transforms/`, register in `Phase1ReadTypescript.scala`
- **Scala.js transform:** add to `scalajs/src/.../scalajs/transforms/`, register in `Phase2ToScalaJs.scala`
- **Flavour change:** edit the relevant `*Flavour.scala` in `scalajs/src/.../scalajs/flavours/`
- **CLI flag:** add to `cli/` scopt config and wire through `ConversionOptions`
- **New dependency:** add to `project/Deps.scala`, reference from the appropriate module in `build.sbt`
- **Snapshot test:** add a test `.d.ts` under `importer/src/test/resources/`, add test case in `ImporterTest.scala`

### Common Mistakes
- `sbt-converter`: only `importer-portable` deps; never add JVM-specific (`importer`) deps
- `IArray`: has no standard `Iterable` methods — use `IArray.fromTraversable`, `.toVector`, or IArray-specific API
- Phase results: always `.map`/`.flatMap` on `PhaseRes`; don't unwrap with `.get` or throw
- Snapshot tests update automatically when run locally (not in CI); intentional changes — just run tests locally
- Cross-version: `sbt-converter` is Scala 2.12 only; use `CrossVersion.for3Use2_13` for libs shared between Scala 2/3

### When to Plan
- New encoding strategy affecting multiple transforms or phases
- New flavour or significant flavour change
- Modifying the phase pipeline order
- Changes that affect the `sbt-converter` API surface (used by end users)

**Don't plan:** single-file bugfixes, adding a snapshot test, formatting, logging

### Pre-completion Checklist
1. **Self-check** — "Would a senior engineer approve this at code review?"
2. `sbt compile` passes without errors (check all affected modules)
3. `sbt scalafmtAll` run
4. No compiler warnings
5. `sbt "importer/testOnly org.scalablytyped.converter.internal.importer.ImporterTest*"` passes (or snapshots intentionally updated locally)
6. For parser changes: `sbt "importer/testOnly org.scalablytyped.converter.internal.ts.parser.*"` passes

### Standards
- Never throw across phase boundaries — use `PhaseRes` / `Either`
- No `var`, `null`, mutable collections
- Minimize change scope — fix root cause, not symptoms
- Prefer `IArray` over `Seq`/`Vector` in hot paths
