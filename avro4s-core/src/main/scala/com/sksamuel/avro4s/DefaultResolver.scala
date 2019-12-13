package com.sksamuel.avro4s

import java.nio.ByteBuffer
import java.util.UUID

import org.apache.avro.LogicalTypes.Decimal
import org.apache.avro.generic.{GenericEnumSymbol, GenericFixed}
import org.apache.avro.util.Utf8
import org.apache.avro.{Conversions, Schema}
import CustomDefaults._
import org.apache.avro.Schema.Type._
import scala.collection.JavaConverters._

/**
  * When we set a default on an avro field, the type must match
  * the schema definition. For example, if our field has a schema
  * of type UUID, then the default must be a String, or for a schema
  * of Long, then the type must be a java Long and not a Scala long.
  *
  * This class will accept a scala value and convert it into a type
  * suitable for Avro and the provided schema.
  */
object DefaultResolver {

  /**
    * Tries to transform the value into something compatible with the schema
    *
    * If the cleverness doesn't work, return the value as-is
    *
    * @param value the value to transform, as a String
    * @param schema the target schema, determining what type we want for the value
    * @return the value in the most compatible form possible for the schema
    */
  def resolve(value: String, schema: Schema): Any = schema.getType match {
    case INT => value.toInt
    case LONG => value.toLong
    case FLOAT => value.toFloat
    case DOUBLE => value.toDouble
    case BOOLEAN => value.toBoolean
    case STRING => value
    case _ => value
  }

  def apply(value: Any, schema: Schema): AnyRef = value match {
    case Some(x) => apply(x, schema)
    case u: Utf8 => u.toString
    case uuid: UUID => uuid.toString
    case enum: GenericEnumSymbol => enum.toString
    case fixed: GenericFixed => fixed.bytes()
    case bd: BigDecimal => bd.toString()
    case byteBuffer: ByteBuffer if schema.getLogicalType.isInstanceOf[Decimal] =>
      val decimalConversion = new Conversions.DecimalConversion
      val bd = decimalConversion.fromBytes(byteBuffer, schema, schema.getLogicalType)
      java.lang.Double.valueOf(bd.doubleValue)
    case byteBuffer: ByteBuffer => byteBuffer.array()
    case x: scala.Long => java.lang.Long.valueOf(x)
    case x: scala.Boolean => java.lang.Boolean.valueOf(x)
    case x: scala.Int => java.lang.Integer.valueOf(x)
    case x: scala.Double => java.lang.Double.valueOf(x)
    case x: scala.Float => java.lang.Float.valueOf(x)
    case x: Map[_, _] => x.asJava
    case x: Seq[_] => x.asJava
    case p: Product => customDefault(p, schema)
    case v if isScalaEnumeration(v) => customScalaEnumDefault(value)
    case _ =>
      value.asInstanceOf[AnyRef]
  }

}
