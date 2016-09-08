package services

import java.time.Instant
import javax.inject.{Inject, Singleton}
import java.sql.{PreparedStatement, ResultSet, Statement, Timestamp}

import play.api.db.Database

import scala.annotation.tailrec
import scala.io.Source
import scala.xml.pull.{EvElemStart, EvText, XMLEventReader}

@Singleton
class DataAccess @Inject()(db: Database) {
  val xmlReader = new XMLEventReader(Source.fromFile("conf/JAVA Test.xml"))
  val connection = db.getConnection(false)
  val insertStatement = "INSERT INTO Festivities (name, place, start, end) VALUES(?, ?, ?, ?)"
  val deleteTable = "DROP TABLE IF EXISTS Festivities"
  val createTable = "CREATE TABLE Festivities (ID bigint auto_increment PRIMARY KEY, Name varchar(30), Place varchar(30), Start DATE, End DATE)"
  val statement = connection.createStatement()
  statement.execute(deleteTable)
  statement.execute(createTable)
  load(xmlReader)
  connection.commit()

  private def setStatementValues(map: Map[String, Any], preparedStmt: PreparedStatement): Unit = {
    preparedStmt.setString(1, map.get("name").map(_.toString).getOrElse(null))
    preparedStmt.setString(2, map.get("place").map(_.toString).getOrElse(null))
    preparedStmt.setTimestamp(3, Timestamp.from(Instant.parse(map.get("start").map(_.toString).getOrElse(null))))
    preparedStmt.setTimestamp(4, Timestamp.from(Instant.parse(map.get("end").map(_.toString).getOrElse(null))))
  }

  private def setStatementValues(f: Festivity, preparedStmt: PreparedStatement): Unit = {
    setStatementValues(Map(
      "name" -> f.name,
      "place" -> f.place,
      "start" -> f.start,
      "end" -> f.end
    ), preparedStmt)
  }

  private def load(reader: XMLEventReader): Unit = {
    val preparedStmt = connection.prepareStatement(insertStatement)
    @tailrec
    def loop(map: Map[String, String]): Unit = {
      if (map.size == 4) {
        setStatementValues(map, preparedStmt)
        preparedStmt.addBatch()
        loop(Map.empty)
      } else if (reader.hasNext) {
        val isText = (label:String) => label == "name" || label == "place" || label == "start" || label == "end"
        reader.next() match {
          case EvElemStart(_, label, _, _) => if (isText(label)) loop(map + (label -> (reader.next() match {case EvText(t) => t}))) else loop(map)
          case _ => loop(map)
        }
      }
    }
    loop(Map.empty)
    preparedStmt.executeBatch()
  }

  private def readAsList(rs: ResultSet): List[Festivity] = {
    @tailrec
    def loop(ls: List[Festivity]): List[Festivity] = {
      if(rs.next()) {
        val id = rs.getLong(1)
        val name = rs.getString(2)
        val place = rs.getString(3)
        val start = rs.getTimestamp(4)
        val end = rs.getTimestamp(5)
        loop(Festivity(id, name, place, start.toInstant, end.toInstant)::ls)
      } else ls
    }
    loop(List.empty)
  }

  def getAll(): List[Festivity] = {
    val stmt = connection.createStatement()
    val rs = stmt.executeQuery("SELECT * FROM Festivities")
    val list = readAsList(rs)
    connection.commit()
    list
  }

  def save(f: Festivity): Festivity = {
    val stmt = connection.prepareStatement(insertStatement, Statement.RETURN_GENERATED_KEYS)
    setStatementValues(f, stmt)
    stmt.executeUpdate()
    val generated = stmt.getGeneratedKeys
    generated.next
    val id = generated.getLong(1)
    connection.commit()
    f.setId(id)
  }

  def update(f: Festivity): Festivity = {
    val updateStatement = "UPDATE Festivities SET name=?, place=?, start=?, end=? WHERE ID = ?"
    val stmt = connection.prepareStatement(updateStatement)
    setStatementValues(f, stmt)
    stmt.setLong(5, f.id)
    stmt.executeUpdate()
    connection.commit()
    f
  }

  def query(field: String, value: String): List[Festivity] = {
    query(field)(value)((p, v, i) => p.setString(i, v))
  }

  def query(field: String, value: Instant): List[Festivity] = {
    query(field)(value)((p, v, i) => p.setTimestamp(i, Timestamp.from(v)))
  }

  def queryDateRange(start: Instant, end: Instant): List[Festivity] = {
    val stmt = connection.prepareStatement("SELECT * FROM Festivities WHERE start >= ? AND end <= ?")
    stmt.setTimestamp(1, Timestamp.from(start))
    stmt.setTimestamp(2, Timestamp.from(end))
    val result = stmt.executeQuery()
    val list = readAsList(result)
    connection.commit()
    list
  }

  // curried for better type inference
  private def query[T](field: String)(value: T)(b: (PreparedStatement, T, Int) => Unit): List[Festivity] = {
    val stmt = connection.prepareStatement("SELECT * FROM Festivities WHERE " + field + "=?")
    b(stmt, value, 1)
    val results = stmt.executeQuery()
    val list = readAsList(results)
    connection.commit()
    list
  }
}
