/**
 * Data serialization methods via S-expressions.
 * Taken from a mix of R7RS, Racket and Perl syntax.
 *
 * <p>
 * S-expressions are modeled under three classes
 * ({@code Atom}, {@code Cons}, {@code Nil}),
 * belonging to two categories (atom and list),
 * overlapping at exactly {@code Nil}.
 *
 * <ul>
 *   <li>
 *     Consider an S-expression as binary tree:
 *     an <em>atom</em> is a terminal node; it either
 *     has a value ({@code Atom}) or it doesn&rsquo;t
 *     ({@code Nil}).
 *   </li>
 *   <li>
 *     A <em>cons</em>, modeled by the {@code Cons}
 *     class, is an internal node with two children,
 *     {@code car} and {@code cdr} (pronounced
 *     <em>cou-der</em>).
 * <!-- https://www.cs.drexel.edu/~popyack/Courses/AI/Fa21/extras/LISP/CADDDR.html -->
 *     Because cons is derived from the verb <q>construct</q>,
 *     and pluralizing it would leave the impression of a verb,
 *     it is customary to call it by the somewhat redundant
 *     names <q>cons cell</q> or <q>cons pair</q>.
 *   </li>
 *   <li>
 *     In S-expressions, a <em>list</em> is a chain of
 *     zero or more cons cells, terminated by {@code Nil}.
 *     The interpretation of cons cells in the context of
 *     lists favors treating the car as the first element
 *     (head) of the list, and cdr as the rest.  One may
 *     traverse the list by reading the car and replacing
 *     the list with the cdr.  The end of list is reached
 *     when the list is replaced with {@code Nil}, the
 *     list terminator.  For this reason, {@code Nil} is
 *     also considered a list&mdash;namely the empty list.
 *   </li>
 * </ul>
 */
package org.rapidcow.trivnote.libexpr;
