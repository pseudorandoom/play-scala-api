package services

import java.time.Instant
import javax.inject.{Inject, Singleton}
import java.sql.{Date => SqlDate, PreparedStatement}
import java.util.Date

import play.api.db.Database

import scala.annotation.tailrec
import scala.io.Source
import scala.xml.pull.{EvElemStart, EvText, XMLEventReader}

@Singleton
class XMLDataLoader @Inject()(db: Database) {
  val xmlReader = new XMLEventReader(Source.fromFile("conf/JAVA Test.xml"))
  val connection = db.getConnection()
  val insertStatement = "INSERT INTO Festivities (name, place, start, end) VALUES(?, ?, ?, ?)"
  val deleteTable = "DROP TABLE IF EXISTS Festivities"
  val createTable = "CREATE TABLE Festivities (ID bigint auto_increment, Name varchar(255), Place varchar(255), Start DATE, End DATE)"
  val statement = connection.createStatement()
  statement.execute(deleteTable)
  statement.execute(createTable)
  load(xmlReader)
  connection.commit()

  private def insert(map: Map[String, String], preparedStmt: PreparedStatement): Unit = {
    preparedStmt.setString(1, map.getOrElse("name", null))
    preparedStmt.setString(2, map.getOrElse("place", null))
    preparedStmt.setDate(3, new SqlDate(Date.from(Instant.parse(map.getOrElse("start", null))).getTime))
    preparedStmt.setDate(4, new SqlDate(Date.from(Instant.parse(map.getOrElse("end", null))).getTime))
    preparedStmt.addBatch()
  }

  private def load(reader: XMLEventReader): Unit = {
    val preparedStmt = connection.prepareStatement(insertStatement)
    @tailrec
    def loop(map: Map[String, String]): Unit = {
      if (map.size == 4) {
        insert(map, preparedStmt)
        loop(Map.empty)
      } else if (reader.hasNext) {
        val isText = (label:String) => label == "name" || label == "place" || label == "start" || label == "end"
        reader.next() match {
          case EvElemStart(_, label, _, _) => if (isText(label)) loop(map + (label -> (reader.next() match {case EvText(t) => t}))) else loop(map)
          case _ => loop(map)
        }
      } else {
        loop(map)
      }
    }
    loop(Map.empty)
    preparedStmt.executeBatch()
  }

  def getAll(): List[Festivity] = {
    val stmt = connection.createStatement()
    val rs = stmt.executeQuery("SELECT * FROM Festivities")
    def loop(ls: List[Festivity]): List[Festivity] = {
      if(rs.next()) {
        val id = rs.getLong(1)
        val name = rs.getString(2)
        val place = rs.getString(3)
        val start = rs.getDate(4)
        val end = rs.getDate(5)
        Festivity(id, name, place, start, end)::ls
      } else ls
    }
    loop(List.empty)
  }
}
