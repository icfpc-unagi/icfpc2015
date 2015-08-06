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
#ifndef GOOGLEAPIS_ESCAPING_H_  // NOLINT
#define GOOGLEAPIS_ESCAPING_H_

#include <string>
namespace googleapis {
using std::string;


namespace strings {


int WebSafeBase64Escape(const unsigned char *src, int szsrc, char *dest,
                        int szdest, bool do_padding);
void WebSafeBase64Escape(const unsigned char *src, int szsrc,
                         string *dest, bool do_padding);

int Base64Escape(const unsigned char *src, int szsrc, char *dest, int szdest);
void Base64Escape(const unsigned char *src, int szsrc,
                  string* dest, bool do_padding);
void Base64Escape(const string& src, string* dest);

int CalculateBase64EscapedLen(int input_len, bool do_padding = true);

bool Base64Unescape(const char *src, int szsrc, string* dest);
inline int Base64Unescape(const string& src, string* dest) {
  return Base64Unescape(src.data(), src.size(), dest);
}

bool WebSafeBase64Unescape(const char *src, int szsrc, string* dest);

inline int WebSafeBase64Unescape(const string& src, string* dest) {
  return WebSafeBase64Unescape(src.data(), src.size(), dest);
}

}  // namespace strings

}  // namespace googleapis
#endif  // GOOGLEAPIS_ESCAPING_H_  // NOLINT
