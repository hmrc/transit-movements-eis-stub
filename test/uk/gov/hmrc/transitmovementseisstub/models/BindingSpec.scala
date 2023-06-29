/*
 * Copyright 2023 HM Revenue & Customs
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
 */

package uk.gov.hmrc.transitmovementseisstub.models

import org.scalacheck.Gen
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

class BindingSpec extends AnyFreeSpec with Matchers {

  "CustomsOffice path bindable" - {

    "bind" - {

      "gb should return Gb" in {
        Bindings.countryCodePathBindable.bind("a", "gb") mustBe Right(CustomsOffice.Gb)
      }

      "xi should return Xi" in {
        Bindings.countryCodePathBindable.bind("a", "xi") mustBe Right(CustomsOffice.Xi)
      }

      "anything else should return Unknown" in forAll(Gen.alphaNumStr) {
        str =>
          Bindings.countryCodePathBindable.bind("a", str) mustBe Right(CustomsOffice.Unknown)
      }

    }

  }

}
