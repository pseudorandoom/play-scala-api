package services

import java.time.Instant

case class Festivity (id: Long, name: String, place: String, start: Instant, end: Instant){
  def setId(id: Long): Festivity = Festivity(id, this.name, this.place, this.start, this.end)
  def setName(name: String): Festivity = Festivity(this.id, name, this.place, this.start, this.end)
  def setPlace(place: String): Festivity = Festivity(this.id, this.name, place, this.start, this.end)
  def setStart(start: Instant): Festivity = Festivity(this.id, this.name, this.place, start, this.end)
  def setEnd(end: Instant): Festivity = Festivity(this.id, this.name, this.place, this.start, end)
}

case object Festivity{
  val EMPTY = Festivity(0L, null, null, null, null)
}