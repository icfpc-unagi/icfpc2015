#ifndef BASE_STRINGS_H_
#define BASE_STRINGS_H_

#include "googleapis/strings/join.h"
#include "googleapis/strings/split.h"
#include "googleapis/strings/strcat.h"
#include "googleapis/strings/stringpiece.h"
#include "googleapis/strings/util.h"

using googleapis::StringPiece;

// Replaces the first occurrence (if replace_all is false) or all occurrences
// (if replace_all is true) of oldsub in s with newsub. In the second version,
// *res must be distinct from all the other arguments.
//
// Definition:
//   string StringReplace(StringPiece s, StringPiece oldsub,
//                        StringPiece newsub, bool replace_all);
//   void StringReplace(StringPiece s, StringPiece oldsub,
//                      StringPiece newsub, bool replace_all,
//                      string* res);
using googleapis::StringReplace;

// Replaces all occurrences of substring in s with replacement. Returns the
// number of instances replaced. s must be distinct from the other arguments.
//
// Less flexible, but faster, than RE::GlobalReplace().
//
//   int GlobalReplaceSubstring(StringPiece substring,
//                              StringPiece replacement,
//                              string* s);
using googleapis::GlobalReplaceSubstring;

// Returns whether str begins with prefix.
//
// Definition:
//   inline bool HasPrefixString(StringPiece str,
//                               StringPiece prefix);
using googleapis::HasPrefixString;

// StrCat()
//    This merges the given strings or numbers, with no delimiter.  This
//    is designed to be the fastest possible way to construct a string out
//    of a mix of raw C strings, StringPieces, strings, bool values,
//    and numeric values.
//
//    Don't use this for user-visible strings.  The localization process
//    works poorly on strings built up out of fragments.
//
//    For clarity and performance, don't use StrCat when appending to a
//    string.  In particular, avoid using any of these (anti-)patterns:
//      str.append(StrCat(...)
//      str += StrCat(...)
//      str = StrCat(str, ...)
//    where the last is the worse, with the potential to change a loop
//    from a linear time operation with O(1) dynamic allocations into a
//    quadratic time operation with O(n) dynamic allocations.  StrAppend
//    is a better choice than any of the above, subject to the restriction
//    of StrAppend(&str, a, b, c, ...) that none of the a, b, c, ... may
//    be a reference into str.
using googleapis::StrCat;

namespace strings {
// Definition:
//   std::vector<StringPiece> Split(const StringPiece& source,
//                                  const StringPiece& delim);
using googleapis::strings::Split;

// Definition:
//   void Join(const CONTAINER& components,
//             StringPiece delim,
//             string* result);
//   string Join(const CONTAINER& components, StringPiece delim);
using googleapis::strings::Join;

}  // namespace strings

#endif  // BASE_STRINGS_H_
