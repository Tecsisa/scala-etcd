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
import scala.concurrent.duration._
import scala.language.postfixOps
import EtcdExceptions._

import scala.concurrent.duration.Duration

object EtcdClient {

  object Action {
    val CreateDir = "createDir"
    val DeleteDir = "deleteDir"
    val ListDir = "listDir"
    val DeleteKey = "deleteKey"
    val Get = "get"
    val Set = "set"
    val UnsetExpiration = "unsetExpiration"
  }

  private val defaultConn = s"$DefaultEtcdHost:$DefaultEtcdPort"

  def local(): EtcdClient = new EtcdClient(defaultConn)
  def remote(host: String, port: Int = DefaultEtcdPort) = new EtcdClient(s"$host:$port")

}

class EtcdClient(conn: String) {

  import EtcdClient._

  private val baseUrl = s"$conn/v2/keys"

  def createDir(dir: String): Future[EtcdSingleResponse] =
    processRequest[EtcdSingleResponse](EtcdRequest(Action.CreateDir, Node(dir)))

  def deleteDir(dir: String, recursive: Boolean = false): Future[EtcdSingleResponse] =
    processRequest[EtcdSingleResponse](EtcdRequest(Action.DeleteDir, Node(dir), recursive = recursive))

  def deleteKey(key: String): Future[EtcdSingleResponse] =
    processRequest[EtcdSingleResponse](EtcdRequest(Action.DeleteKey, Node(key)))

  def getKey(key: String): Future[EtcdSingleResponse] =
    getKeyAndWait(key, wait = false)

  def getKeyAndWait(key: String, wait: Boolean = true): Future[EtcdSingleResponse] =
    processRequest[EtcdSingleResponse](EtcdRequest(Action.Get, Node(key), waiting = wait))

  def lsDir(key: String, recursive: Boolean = false, sorted: Boolean = false): Future[EtcdListResponse] =
    processRequest[EtcdListResponse](EtcdRequest(Action.ListDir, Node(key), recursive = recursive, sorting = Some(sorted)))

  def setKey(key: String, value: String, ttl: Duration = 0 second, sorted: Boolean = false): Future[EtcdSingleResponse] =
    processRequest[EtcdSingleResponse](EtcdRequest(Action.Set, Node(key, Some(value)), waiting = false,
      ttl = if (ttl > (0 second)) Some(ttl.toSeconds.toInt) else None, sorting = if (sorted) Some(true) else None))

  def unsetExpiration(key: String, value: String): Future[EtcdSingleResponse] =
    processRequest[EtcdSingleResponse](EtcdRequest(Action.UnsetExpiration, Node(key, Some(value)), prevExist = Some(true)))

  private def processRequest[T <: EtcdResponse : EtcdResponseFormatter](request: EtcdRequest): Future[T] = {
    val execute: Req => Future[T] = { req =>
      //must follow redirects without removing query parameters
      val result = Http.configure(x => x.setRemoveQueryParamsOnRedirect(false).setFollowRedirects(false))(req OK as.String).either
      Future {
        result() match {
          case Right(content) => implicitly[EtcdResponseFormatter[T]].parseJson(content.asInstanceOf[String])
          case Left(StatusCode(403))  => throw UnforbiddenOperationException(request.node.key)
          case Left(StatusCode(404))  => throw KeyNotFoundException(request.node.key)
          case Left(t: Throwable) => throw new RuntimeException("Unexpected error: " + t.getMessage, t)
        }
      }
    }
    request.action match {
      case Action.CreateDir =>
        execute((:/(baseUrl) / request.node.key).PUT <<? Map("dir" -> true.toString))
      case Action.DeleteDir =>
        execute((:/(baseUrl) / request.node.key).DELETE <<?
          Map("recursive" -> request.recursive.toString, "dir" -> true.toString))
      case Action.DeleteKey =>
        execute((:/(baseUrl) / request.node.key).DELETE)
      case Action.Get =>
        execute((:/(baseUrl) / request.node.key).GET <<? Map("wait" -> request.waiting.toString))
      case Action.ListDir =>
        execute((:/(baseUrl) / request.node.key).GET <<?
          Map("recursive" -> request.recursive.toString,
              "sorted" -> request.sorting.getOrElse(false).toString))
      case Action.Set =>
        val params: Map[String, String] = Map("value" -> request.node.value.getOrElse("")) ++
          (request.ttl.map { case ttl => Map("ttl" -> ttl.toString) }).getOrElse(Map.empty[String, String])
        val baseReq = :/(baseUrl) / request.node.key
        val req = if (!request.sorting.getOrElse(false)) baseReq.PUT else baseReq.POST
        execute(req << params)
      case Action.UnsetExpiration =>
        execute((:/(baseUrl) / request.node.key).PUT <<
          Map("value" -> request.node.value.getOrElse(""),
              "ttl" -> "", "prevExist" -> request.prevExist.getOrElse("false").toString))
    }

  }

}
