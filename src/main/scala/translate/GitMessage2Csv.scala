package translate

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

import java.io.File

import util._

object GitMessage2Csv extends GitMessage2Csv{}

trait GitMessage2Csv extends KeyValueParser with FileReader with WrappedPrintWriter with Commands{

  val csvHeader = "Key\tEnglish\tWelsh\tComments"
  val delimiter = "\t"
  val token = "="
  val noWelshFound = "No Welsh translation found"
  val noEnglishFound = "No English messages found"
  val englishUnchanged = "English message unchanged"
  val englishChanged = "Message changed (previous message was: "
  val separator = " / "
  val englishChangedEnd = ")"
  val newLine = "\n"

  val currentEnglishMessages = "current_messages"
  val currentWelshMessages = "current_messages.cy"
  val oldEnglishMessages = "old_messages"
  val oldWelshMessages = "old_messages.cy"

  def messages2csv(csvOutputFileName: String):Unit = {

    val enList = fetchMessages(currentEnglishMessages, mustExist = true)
    val cyList = fetchMessages(currentWelshMessages, mustExist = false)
    val oldEnList = fetchMessages(oldEnglishMessages, mustExist = true)

    val outputCsvLines = enList.map{ enMessage =>

      val oOldEnMsg = oldEnList.find(oldEnMessage => enMessage._1 == oldEnMessage._1)
      val oCyMsg = cyList.find(cyMessage => enMessage._1 == cyMessage._1).map(cyMsg => cyMsg._2)

      oCyMsg.fold(enMessage._1 + delimiter + enMessage._2 + delimiter + delimiter + noWelshFound)
      {cyMsg =>
        checkEnglishMessageChanged(enMessage._1, enMessage._2, oOldEnMsg.getOrElse(("",""))._2, cyMsg)
      } + newLine
    }

    writeFile(csvOutputFileName, csvHeader + newLine + outputCsvLines.fold("")((key,value) => key + value))
  }

  private def checkEnglishMessageChanged(key: String, enMessage: String, oldEnMsg: String, cyMsg: String): String = {

    if(oldEnMsg == enMessage){
      if(cyMsg == ""){
        key + delimiter + enMessage + delimiter + delimiter + noWelshFound
      }
      else {
        key + delimiter + enMessage + delimiter + cyMsg  + delimiter + englishUnchanged
      }
    }
    else{
      key + "\t" + enMessage + delimiter + delimiter + englishChanged+ oldEnMsg + separator + cyMsg + englishChangedEnd
    }
  }


  def fetchGitFiles(projectDir: String, gitCloneRef: String, gitCommitRef: String):Unit = {

    val pr = System.getProperties()
    // Windows / Linux / MacOS / Other
    pr.get("os.name") match {
      case (linux: String) if(linux.contains("Linux")) => executeCommand(s"./git-retrieve.sh $projectDir $gitCloneRef $gitCommitRef")
      case (windows: String) if(windows.contains("Windows")) => println("Only Bash script created so far. Windows bat file will be added, or please feel free to add it! :)")
      case (macOS: String) if(macOS.contains("Mac")) => executeCommand(s"./git-retrieve.sh $projectDir $gitCloneRef $gitCommitRef")
      case _ => println("Only Bash script created so far. ")
    }
  }

  def fetchMessages(lang:String, mustExist: Boolean):List[(String, String)] = {
    val lines = for (line <- linesFromFile(lang, mustExist)) yield line
    lines.flatMap{ line =>
      splitKeyValue(line, token).map(line => line._1 -> line._2)
    }.toList
  }

}