# Findings for 0.2.0

## Document status

Rechecked against `efb4665` on 2026-10-29. No outstanding bugs remain from the original findings.

- **Cons-valued tails:** fixed in `2bbbb8d`; constructor/factory regression tests cover cached and ordinary tails, proper and improper lists, and repeated wrapping.
- **Unterminated block comments:** fixed in `8ec8668`; regression tests cover incomplete and nested comments, iteration, and error positions.
- **Null predicates:** fixed in `5e6e2ef`. `Datum.isAtom(null)` and `Datum.isList(null)` now throw `NullPointerException` immediately.
- **Null list source:** fixed in `7678e8d`. `new DatumList((Datum) null)` now throws `NullPointerException` at construction.
- **Direct reads and iterator lookahead:** intentional, documented behavior; not a defect.
- **DataDumper:** planned for 0.2.0; not an outstanding defect for this release.

Validation: reran the full library test suite and Javadoc generation successfully, without warnings. Also ran a standalone check confirming immediate rejection of all three null inputs above. This closes the original findings; it is not an exhaustive audit of the library.

## Preamble

Found while completing the public/protected API Javadocs. No runtime behavior was changed. The examples below were checked against the compiled library; the existing 181 tests pass.

## Cons-valued tails invalidate cached list metadata

Both `ConsList` array constructors cache only the supplied prefix length and the supplied tail, even when that tail is another cons chain. `DatumList` trusts those cached fields.

```java
Datum a = Symbol.of("a"), b = Symbol.of("b");
DatumList list = Datum.asList(
    new ConsList(a, new Datum[0], Cons.ofList(b)));
```

This iterates over two elements, but `size()` returns 1 and `isProperList()` returns false. The underlying chain is the proper list `(a b)`. Include the tail chain’s length and final atom when computing metadata, or explicitly reject cons-valued tails. Review the `Cons.of` overloads that select this implementation too.

## Unterminated block comments

```java
new DataLoader(new java.io.StringReader("#| unfinished")).readDatum()
```

Previously this returned null (EOF), rather than throwing `DataLoadingException`. `skipBlockComment()` now rejects EOF while any comment nesting remains open. Regression tests cover incomplete delimiters, nested comments, direct reads, iteration, and error positions; balanced comments still parse normally.

## Direct reads bypass iterator lookahead

For input `a b`, calling `hasNext()`, `readDatum()`, then `next()` returns `b` followed by `a`. `hasNext()` buffers `a`, but `readDatum()` reads directly from the stream. The Javadocs now explain this restriction. Consider using a private raw-read method and making the public read method consume buffered data first.

## Null handling is inconsistent

`Datum.isAtom(null)` and `Datum.isList(null)` both return true because their implementation negates an `instanceof` test. `new DatumList((Datum) null)` also succeeds, but operations that construct a `DatumWalk` later reject its null head. Decide whether predicates should reject null or return false, and validate the view’s source at construction.

## Serialization remains unfinished

`DataDumper` only wraps an output stream; it has no serialization operation. Its Javadocs and the package overview now make this explicit. Datum `toString()` methods are diagnostic and cannot substitute for serialization.
