/*
 * \copyright Copyright 2013 Google Inc. All Rights Reserved.
 * \license @{
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @}
 */
/*
 * \license @{
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @}
 */

// Refactored from contributions of various authors in strings/strutil.cc
//
// This file contains string processing functions related to
// uppercase, lowercase, etc.

#include "googleapis/strings/case.h"

#include <string>
using std::string;

#include "googleapis/strings/ascii_ctype.h"

namespace googleapis {

CapsType GetCapitalization(const char* s) {
  // find the caps type of the first alpha char
  for (; *s && !(ascii_isupper(*s) || ascii_islower(*s)); ++s) {}
  if (!*s) return CAPS_NOALPHA;
  const CapsType firstcapstype = (ascii_islower(*s)) ? CAPS_LOWER : CAPS_UPPER;

  // skip ahead to the next alpha char
  for (++s; *s && !(ascii_isupper(*s) || ascii_islower(*s)); ++s) {}
  if (!*s) return firstcapstype;
  const CapsType capstype = (ascii_islower(*s)) ? CAPS_LOWER : CAPS_UPPER;

  if (firstcapstype == CAPS_LOWER &&
      capstype == CAPS_UPPER)
    return CAPS_MIXED;

  for (; *s; ++s)
    if ((ascii_isupper(*s) && capstype != CAPS_UPPER) ||
        (ascii_islower(*s) && capstype != CAPS_LOWER))
      return CAPS_MIXED;

  if (firstcapstype == CAPS_UPPER && capstype == CAPS_LOWER)
    return CAPS_FIRST;
  return capstype;
}

void LowerString(char* s) {
  for (; *s; ++s) {
    *s = ascii_tolower(*s);
  }
}

void LowerString(std::string* s) {
  auto end = s->end();
  for (auto i = s->begin(); i != end; ++i) {
    *i = ascii_tolower(*i);
  }
}

void LowerStringToBuf(const char* s, char* buf, int n) {
  for (int i = 0; i < n-1; i++) {
    char c = s[i];
    buf[i] = ascii_tolower(c);
    if (c == '\0') {
      return;
    }
  }
  if (n > 0)
    buf[n-1] = '\0';
}

void UpperString(char* s) {
  for (; *s; ++s) {
    *s = ascii_toupper(*s);
  }
}

void UpperString(std::string* s) {
  for (auto iter = s->begin(); iter != s->end(); ++iter) {
    *iter = ascii_toupper(*iter);
  }
}

void UpperStringToBuf(const char* s, char* buf, int n) {
  for (int i = 0; i < n-1; i++) {
    char c = s[i];
    buf[i] = ascii_toupper(c);
    if (c == '\0') {
      return;
    }
  }
  if (n > 0)
    buf[n-1] = '\0';
}

}  // namespace googleapis
