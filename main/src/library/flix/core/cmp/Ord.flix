/*
 * Copyright 2017 Magnus Madsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Total Order type class.
 */
pub class Ord[a] <= PartialOrd[a] {

    // TODO: Add more laws.

}

impl Ord[Bool]

impl Ord[Int32]

impl Ord[(a, b)] <= Ord[a], Ord[b]

impl Ord[Option[a]] <= Ord[a]

impl Ord[List[a]] <= Ord[a]

impl Ord[Result[a, b]] <= Eq[a], Eq[b]

disallow Ord[Float32]

disallow Ord[Float64]

disallow Ord[a -> b]
