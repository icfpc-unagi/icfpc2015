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
#ifndef GOOGLEAPIS_THREAD_EXECUTOR_H_
#define GOOGLEAPIS_THREAD_EXECUTOR_H_


#include "googleapis/base/macros.h"
namespace googleapis {

class Closure;

namespace thread {

class Executor {
 public:
  Executor();
  virtual ~Executor();

  virtual int num_pending_closures() const = 0;
  virtual void Add(Closure* callback) = 0;
  virtual bool TryAdd(Closure* closure) = 0;

  // Caller retains ownership.
  static void SetDefaultExecutor(Executor* executor);
  static Executor* DefaultExecutor();

 private:
  DISALLOW_COPY_AND_ASSIGN(Executor);
};

// Immediately executes closures without using threads.
// Caller should delete when done with it.
Executor* NewInlineExecutor();

// Ownership is maintained internally by the Executor itself.
Executor* SingletonInlineExecutor();

}  // namespace thread

}  // namespace googleapis
#endif  // GOOGLEAPIS_THREAD_EXECUTOR_H_
