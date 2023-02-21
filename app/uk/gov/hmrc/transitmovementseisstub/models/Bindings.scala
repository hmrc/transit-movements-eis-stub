package uk.gov.hmrc.transitmovementseisstub.models

import play.api.mvc.PathBindable

object Bindings {

  implicit val countryCodeBinding = new PathBindable[CountryCode] {
    override def bind(key: String, value: String): Either[String, CountryCode] =
      CountryCode.find(value).toRight(s"Country code $value is not valid for this service")

    override def unbind(key: String, value: CountryCode): String = value.toString
  }

}
