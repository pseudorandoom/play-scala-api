package services

import java.util.Date

case class Festivity (id: Long, name: String, place: String, start: Date, end: Date){
  def setId(id: Long): Festivity = Festivity(id, this.name, this.place, this.start, this.end)
  def setName(name: String): Festivity = Festivity(this.id, name, this.place, this.start, this.end)
  def setPlace(place: String): Festivity = Festivity(this.id, this.name, place, this.start, this.end)
  def setStart(start: Date): Festivity = Festivity(this.id, this.name, this.place, start, this.end)
  def setEnd(end: Date): Festivity = Festivity(this.id, this.name, this.place, this.start, end)
}

case object Festivity{
  val EMPTY = Festivity(0L, null, null, null, null)
}