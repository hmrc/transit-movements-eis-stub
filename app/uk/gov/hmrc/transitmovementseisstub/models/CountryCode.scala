package uk.gov.hmrc.transitmovementseisstub.models

sealed trait CountryCode

object CountryCode {
  case object GB extends CountryCode
  case object XI extends CountryCode

  val values: Seq[CountryCode] = Seq(GB, XI)

  def find(code: String): Option[CountryCode] = values.find(_.toString.equalsIgnoreCase(code))
}
