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

import EtcdJsonProtocol._
import dispatch._, Defaults._
import scala.concurrent.Future
import EtcdExceptions._

object EtcdClient {

  object Action {
    val CreateDir = "createDir"
    val DeleteDir = "deleteDir"
    val Get = "get"
    val Set = "set"
  }

  private val defaultConn = s"$DefaultEtcdHost:$DefaultEtcdPort"

  def local(): EtcdClient = new EtcdClient(defaultConn)

}

class EtcdClient(conn: String) {

  import EtcdClient._

  private val baseUrl = s"$conn/v2/keys"

  def createDir(dir: String): Future[EtcdResponse] =
    processRequest(EtcdRequest(Action.CreateDir, Node(dir)))

  def deleteDir(dir: String, recursive: Boolean = false): Future[EtcdResponse] =
    processRequest(EtcdRequest(Action.DeleteDir, Node(dir), recursive = recursive))

  def getKey(key: String): Future[EtcdResponse] =
    getKeyAndWait(key, wait = false)

  def getKeyAndWait(key: String, wait: Boolean = true): Future[EtcdResponse] =
    processRequest(EtcdRequest(Action.Get, Node(key), waiting = wait))

  def setKey(key: String, value: String): Future[EtcdResponse] =
    processRequest(EtcdRequest(Action.Set, Node(key, Some(value)), waiting = false))

  private def processRequest(request: EtcdRequest): Future[EtcdResponse] = {
    val execute: Req => Future[EtcdResponse] = { req =>
      val result = Http(req OK as.String).either
      result() match {
        case Right(content) => Future(parseResponse(content.asInstanceOf[String]))
        case Left(StatusCode(403))  => throw UnforbiddenOperationException(request.node.key)
        case Left(StatusCode(404))  => throw KeyNotFoundException(request.node.key)
        case Left(t: Throwable) => throw new RuntimeException("Unexpected error: " + t.getMessage, t)
      }
    }
    request.action match {
      case Action.CreateDir =>
        execute((:/(baseUrl) / request.node.key).PUT <<? Map("dir" -> true.toString))
      case Action.DeleteDir =>
        execute((:/(baseUrl) / request.node.key).DELETE <<?
          Map("recursive" -> request.recursive.toString, "dir" -> true.toString))
      case Action.Get =>
        execute((:/(baseUrl) / request.node.key).GET <<? Map("wait" -> request.waiting.toString))
      case Action.Set =>
        execute((:/(baseUrl) / request.node.key).PUT << Map("value" -> request.node.value.getOrElse("")))
    }

  }

}
