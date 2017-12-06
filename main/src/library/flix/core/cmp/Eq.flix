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

pub class Eq[a] {

    ///
    /// Returns `true` if `x` is equal to `y`.
    ///
    def equal(x: a, y: a): Bool // TODO: Name

    ///
    /// Reflexivity: An element `a` must be equal to itself.
    ///
    law reflexive(): Bool = ∀(x: a). equal(x, x)

    // TODO: More laws

}