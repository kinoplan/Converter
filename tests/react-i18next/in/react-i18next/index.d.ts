/**
 * Regression test for LibrarySpecific.reactI18next:
 *
 * 1. TransLegacy / TransSelector / IcuTransComponent / IcuTransWithoutContextComponent:
 *    Single-call-signature interfaces where FillInTParams.inlineTParams leaves a type param
 *    (TContext) as a free variable due to non-transitive substitution.
 *    PreferTypeAlias converts the interface to a type alias containing the free var.
 *    Without the patch the generated Scala fails to compile with "not found: type TContext".
 *
 * 2. useTranslation:
 *    Typed as a conditional type which ScalablyTyped cannot resolve, producing js.Any.
 *    The patch replaces the type with UseTranslationSelector so it is properly callable.
 */

type Wrapper<T> = { ctx: T };

// Interfaces with single call signatures that trigger the free-variable bug.
// The patch drops all TsMemberCall members from these interfaces.
export interface TransLegacy {
  <
    Key extends Wrapper<TOpt>,
    Ns = string,
    KPrefix = undefined,
    TContext = undefined,
    TOpt extends Wrapper<TContext> = Wrapper<TContext>
  >(props: Key): void;
}

export interface TransSelector {
  <
    Key extends Wrapper<TOpt>,
    Ns = string,
    TContext = undefined,
    TOpt extends Wrapper<TContext> = Wrapper<TContext>
  >(props: Key): void;
}

export interface IcuTransComponent {
  <
    Key extends Wrapper<TOpt>,
    TContext = undefined,
    TOpt extends Wrapper<TContext> = Wrapper<TContext>
  >(props: Key): void;
}

export interface IcuTransWithoutContextComponent {
  <
    Key extends Wrapper<TOpt>,
    TContext = undefined,
    TOpt extends Wrapper<TContext> = Wrapper<TContext>
  >(props: Key): void;
}

export declare const Trans: TransLegacy;

// useTranslation: conditional type replaced with UseTranslationSelector by the patch.
export interface UseTranslationSelector {
  (): void;
  (ns: string): void;
}

export interface UseTranslationLegacy {
  (ns?: string): void;
}

type _EnableSelector = false;

export declare const useTranslation: _EnableSelector extends true ? UseTranslationSelector : UseTranslationLegacy;
