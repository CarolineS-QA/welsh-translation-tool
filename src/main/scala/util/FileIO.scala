/*
 * Copyright 2015-2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util

import java.io.{File, PrintWriter}

import scala.io.Source

trait KeyValueParser{
  def splitKeyValues(line:String, token:String): List[(String, (String, String))] = {
    if (line.trim.isEmpty || line.trim.startsWith("#")) {
      List.empty
    } else {
      val cols = line.split(token).toList
      val key = cols.headOption.getOrElse("").trim

      val values = cols.tail
      val value1 = cols.tail.headOption
      val value2 = if(value1.isDefined){
                     cols.tail.tail.headOption
                   }else{ None }

      List(key -> (value1.getOrElse("").trim, value2.getOrElse("").trim))
    }
  }

  def splitKeyValue(line:String, token:String): List[(String, String)] = {
    if (line.trim.isEmpty || line.trim.startsWith("#")) {
      List.empty
    } else {
      val cols = line.split(token).toList
      val key = cols.headOption.getOrElse("").trim
      val value = cols.tail.mkString(token).trim
      List(key -> value)
    }
  }

}


trait FileReader {
  lazy val source = Source

  def linesFromFile(fileName: String, mustExist: Boolean): Iterator[String] = {
    try {
      source.fromFile(fileName).getLines()
    }
    catch{
      case _: Throwable => {
        if (mustExist) {
          println(s"Error: Couldn't open file: $fileName :(")
        } else{
          println(s"Warning: Couldn't open file: $fileName, but it may just not exist yet. :) ")
        }
        Iterator(" = ")
      }
    }
  }
}


trait CsvReader extends FileReader with KeyValueParser {
  def readFromCsv(translations:String):List[(String, (String, String))] = {
    val lines = for (line <- linesFromFile(translations, true)) yield line
    lines.flatMap { line =>
      splitKeyValues(line, "\t")
    }.toList
  }
}


trait WrappedPrintWriter{
  def writeFile(fileName: String, content: String): Unit  = {
    val pw = new PrintWriter(fileName)
    pw.println(content)
    pw.close()
  }
}


trait PathParser {
  def extractPath(fileName: String): String = {
    val maybeDir = new File(fileName)
    if (maybeDir.isDirectory) {fileName}
    else {
      val slashIndex = fileName.lastIndexOf("/")
      val lastSlash = if (slashIndex < 0) 0 else slashIndex
      val dir = fileName.substring(0, lastSlash)
      val file = new File(dir)
      if (file.exists()) {dir}
      else {"."}
    }
  }
}