package services.util

import play.api.http.Status
import com.google.common.base.Charsets
import com.google.common.io.BaseEncoding

/**
 * ResponseUtil
 * ----------------
 * Author: haqa
 * Date: 06 December 2017 - 08:55
 * Copyright (c) 2017  Office for National Statistics
 */
object EncodingUtil extends Status {

  def decodeArrayByte(bytes: Array[Byte]): String = bytes.map(_.toChar).mkString

  def encodeToArrayByte(str: String): Array[Byte] = str.getBytes("UTF-8")

  def encodeBase64(str: Seq[String], deliminator: String = ":"): String =
    BaseEncoding.base64.encode(str.mkString(deliminator).getBytes(Charsets.UTF_8))

  def decodeBase64(str: String): String =
    new String(BaseEncoding.base64().decode(str), "UTF-8")

}
