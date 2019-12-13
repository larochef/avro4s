package com.sksamuel.avro4s.json

import java.util

import com.sksamuel.avro4s._
import org.apache.avro.Schema

/**
  * Accepts a json string, and returns an Avro Schema that best matches the json string.
  *
  * Converts:
  *
  * - json arrays to avro arrays
  * - objects to records
  * - doubles to doubles
  * - ints/longs to longs
  * - booleans to booleans
  * - nulls to union(string,null)
  */
class JsonToAvroConverter(namespace: String,
                          avroStringTypeIsString: Boolean = false,
                          jsonFieldMapper: FieldMapper = DefaultFieldMapper) {

  import org.json4s._
  import org.json4s.native.JsonMethods._

  import scala.collection.JavaConverters._

  def convert(name: String, str: String): Schema = {
    convert(name, parse(str).transformField {
      case JField(n, v) =>
        val newName = toCamelCase(n, jsonFieldMapper)
        (newName, v)
    })
  }

  def convert(name: String, value: JValue): Schema = value match {
    case JArray(list) if list.isEmpty => Schema.create(Schema.Type.NULL)
    case JArray(list) => Schema.createArray(convert(name, list.head))
    case JBool(_) => Schema.create(Schema.Type.BOOLEAN)
    case JDecimal(_) => Schema.create(Schema.Type.DOUBLE)
    case JDouble(_) => Schema.create(Schema.Type.DOUBLE)
    case JInt(_) => Schema.create(Schema.Type.LONG)
    case JLong(_) => Schema.create(Schema.Type.LONG)
    case JNothing => Schema.create(Schema.Type.NULL)
    case JNull => Schema.createUnion(util.Arrays.asList(Schema.create(Schema.Type.NULL), createStringSchema))
    case JString(_) => createStringSchema
    case JSet(value) => Schema.createArray(convert(name, value.head))
    case JObject(values) =>
      val record = Schema.createRecord(name, null, namespace, false)
      val doc: String = null
      val default: AnyRef = null
      val fields = values.map { case (k, v) => new Schema.Field(k, convert(k, v), doc, default) }
      record.setFields(fields.asJava)
      record
  }

  private def createStringSchema = {
    val schema = Schema.create(Schema.Type.STRING)
    if (avroStringTypeIsString) schema.addProp("avro.java.string", "String".asInstanceOf[Any])
    schema
  }

  private def toCamelCase(s: String, from: FieldMapper): String = {
    def fromDelimited(sep: String, s: String): String = {
      val head :: tail = s.split(sep).toList
      head ++ tail.foldLeft("")((acc, word) => acc ++ word.capitalize)

    }

    def decapitalize(s: String): String = {
      if (s.nonEmpty) s.head.toLower.toString + s.tail else s
    }

    from match {
      case DefaultFieldMapper => s
      case PascalCase => decapitalize(s)
      case SnakeCase => fromDelimited("_", s)
      case LispCase => fromDelimited("-", s)
    }
  }

}
