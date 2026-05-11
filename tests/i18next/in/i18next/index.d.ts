/**
 * Regression test for LibrarySpecific.i18next:
 * TFunctionSelector has call signatures that produce identical JVM-erased signatures.
 * Without the patch, the generated Scala fails to compile with "double definition" errors.
 *
 * Dropped by patch:
 *   - 4-tparam overloads (unconditionally)
 *   - 3-tparam overloads with a type param bounded by `string`
 */
export interface TFunctionSelector<Ns, KPrefix, Source> {
  // 4-tparam: dropped (case 4 => true)
  <Target, Key, NewNs, KPrefix2>(key: string): string;
  // 3-tparam with DV extends string: dropped (case 3 with string upper bound)
  <Key, DV extends string, NewNs>(key: string): string;
  // 2-tparam: kept — this is the overload that compiles
  <Key, NewNs>(key: string): string;
}

export type TFunction<Ns, KPrefix> = TFunctionSelector<Ns, KPrefix, object>;
