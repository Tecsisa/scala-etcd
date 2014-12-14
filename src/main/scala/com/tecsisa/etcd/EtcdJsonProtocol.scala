/*
* Copyright 2014 Tecnología, Sistemas y Aplicaciones S.L.
*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy of
* the License at
*
 * http://www.apache.org/licenses/LICENSE-2.0
*
 * Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations under
* the License.
*/
package com.tecsisa.etcd

import org.json4s._, native.JsonMethods._

object EtcdJsonProtocol {

  //single key/values
  case class Node(key: String, value: Option[String] = None, modifiedIndex: Int = 0, createdIndex: Int = 0)
  case class EtcdResponse(action: String, node: Node, prevNode: Option[Node])
  case class EtcdRequest(action: String, node: Node, waiting: Boolean = false, recursive: Boolean = false)

  implicit lazy val formats = DefaultFormats

  def parseResponse(json: String): EtcdResponse = {
    parse(json).extract[EtcdResponse]
  }

}
