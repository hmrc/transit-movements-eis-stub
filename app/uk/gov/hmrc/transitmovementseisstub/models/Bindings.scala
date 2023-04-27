package uk.gov.hmrc.transitmovementseisstub.models

import play.api.mvc.PathBindable

object Bindings {

  implicit def customsOfficePathBindable: PathBindable[CustomsOffice] = new PathBindable[CustomsOffice] {

    override def bind(key: String, value: String): Either[String, CustomsOffice] =
      value match {
        case "gb" => Right(CustomsOfficeGB)
        case "xi" => Right(CustomsOfficeXI)
        case _    => Left(s"$key value $value is not valid. expecting gb or xi")
      }

    override def unbind(key: String, value: CustomsOffice): String =
      value match {
        case CustomsOfficeGB => "gb"
        case CustomsOfficeXI => "xi"
      }
  }

}
