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

  sealed trait EtcdResponse

  //single key/values
  case class Node(key: String, value: Option[String] = None,
                  dir: Option[Boolean] = None, modifiedIndex: Int = 0,
                  createdIndex: Int = 0, expiration: Option[String] = None, ttl: Option[Int] = None)
  case class EtcdSingleResponse(action: String, node: Node, prevNode: Option[Node]) extends EtcdResponse

  //for handling dirs
  case class NodeListElement(key: String, value: Option[String], dir: Option[Boolean],
                             nodes: Option[List[NodeListElement]])
  case class EtcdListResponse(action: String, node: NodeListElement) extends EtcdResponse

  // requests
  case class EtcdRequest(action: String, node: Node, waiting: Boolean = false,
                         recursive: Boolean = false, ttl: Option[Int] = None,
                         prevExist: Option[Boolean] = None, sorting: Option[Boolean] = None)

  implicit lazy val formats = DefaultFormats

  abstract class EtcdResponseFormatter[T <: EtcdResponse] {
    def parseJson(json: String): T
  }

  implicit object EtcdSingleResponseFormatter extends EtcdResponseFormatter[EtcdSingleResponse] {
    def parseJson(json: String) = parse(json).extract[EtcdSingleResponse]
  }

  implicit object EtcdListResponseFormatter extends EtcdResponseFormatter[EtcdListResponse] {
    def parseJson(json: String) = parse(json).extract[EtcdListResponse]

  }

}
