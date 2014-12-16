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

import com.tecsisa.etcd.EtcdExceptions.KeyNotFoundException
import com.tecsisa.etcd.EtcdJsonProtocol.{NodeListElement, EtcdListResponse, EtcdSingleResponse}
import org.scalatest.{BeforeAndAfter, Matchers, FunSuite}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.{ExecutionContext, Await}
import scala.concurrent.duration._
import scala.language.postfixOps
import ExecutionContext.Implicits.global

/**
 * Currently, this smoke tests expect an etcd service
 * running local on the default port 4001. An easy way
 * to have that is running etcd as docker container.
 *
 * See https://github.com/coreos/etcd
 */
class EtcdSmokeTest extends FunSuite
  with BeforeAndAfter with ScalaFutures with Matchers {

  val timeout = 1 second
  implicit val p = PatienceConfig(timeout = timeout)

  private val BaseDirectory = "testing"
  private val client = EtcdClient.local()

  before {

    // deletes and creates testing directory
    Await.ready(for {
      _ <- client.deleteDir(BaseDirectory, recursive = true).recover { case _ => () }
      _ <- client.createDir(BaseDirectory)
    } yield (), timeout)

  }

  after {
    // deletes testing directory
    Await.ready(client.deleteDir(BaseDirectory, recursive = true).recover { case _ => () }, timeout)
  }

  test("createDir should create a new directory") {
    whenReady(client.createDir(BaseDirectory + "/Composers")) { r =>
      r shouldBe a [EtcdSingleResponse]
      r.action should === ("set")
      r.node.key should === (s"/$BaseDirectory/Composers")
      r.node.value should be (None)
      r.node.dir should be (Some(true))
    }
  }

  test("deleteDir should remove a directory") {
    Await.ready(client.createDir(BaseDirectory + "/Composers"), timeout)
    whenReady(client.deleteDir(BaseDirectory + "/Composers")) { r =>
      r shouldBe a [EtcdSingleResponse]
      r.action should === ("delete")
      r.node.key should === (s"/$BaseDirectory/Composers")
      r.node.value should be (None)
      r.node.dir should be (Some(true))
      r.prevNode.map { case node =>
        node.key should === (s"/$BaseDirectory/Composers")
        node.value should be (None)
        node.dir should be (Some(true))
      }
    }
  }

  test("a list dir should return all children nodes both keys and dirs") {
    Await.ready(for {
      _ <- client.createDir(BaseDirectory + "/Composers")
      _ <- client.createDir(BaseDirectory + "/Artists")
      _ <- client.createDir(BaseDirectory + "/Works")
      _ <- client.setKey(BaseDirectory + "/items", "1000")
    } yield (), timeout)
    whenReady(client.lsDir(BaseDirectory)) { r =>
      r shouldBe a [EtcdListResponse]
      r.action should === ("get")
      r.node.key should === (s"/$BaseDirectory")
      r.node.value should be (None)
      r.node.dir should be (Some(true))
      r.node.nodes.map { case nodes => nodes.size should === (4) }
    }
  }

  test("a list dir should deal with recursivity") {
    Await.ready(for {
      _ <- client.createDir(BaseDirectory + "/Composers")
      _ <- client.createDir(BaseDirectory + "/Artists")
      _ <- client.createDir(BaseDirectory + "/Artists/Ferenc Fricsay")
      _ <- client.createDir(BaseDirectory + "/Artists/Daniel Barenboim")
      _ <- client.createDir(BaseDirectory + "/Artists/Martha Argerich")
      _ <- client.setKey(BaseDirectory + "/Artists/items", "2000")
      _ <- client.createDir(BaseDirectory + "/Works")
      _ <- client.setKey(BaseDirectory + "/items", "1000")
    } yield (), timeout)

    // non-recursive first
    whenReady(client.lsDir(BaseDirectory)) { r =>
      r shouldBe a [EtcdListResponse]
      val mapNodes = r.node.nodes.map(nodes => nodes map (e => e.key -> e) toMap)
      mapNodes map { case nodes =>
        nodes.keys.size should === (4)
        nodes(s"/$BaseDirectory/Artists").nodes should be (None)
      }
    }

    // recursive
    whenReady(client.lsDir(BaseDirectory, recursive = true)) { r =>
      r shouldBe a [EtcdListResponse]
      val mapNodes = r.node.nodes.map(nodes => nodes map (e => e.key -> e) toMap)
      mapNodes map { case nodes =>
        nodes.keys.size should === (4)
        nodes(s"/$BaseDirectory/Artists").nodes should not be (None)
        nodes(s"/$BaseDirectory/Artists").nodes map { _.size should === (4) }
      }
    }
  }

  test("a list dir should deal with sorted keys") {
    Await.ready(for {
      _ <- client.setKey(BaseDirectory + "/Artists", "Ferenc Fricsay", sorted = true)
      _ <- client.setKey(BaseDirectory + "/Artists", "Daniel Barenboim", sorted = true)
      _ <- client.setKey(BaseDirectory + "/Artists", "Martha Argerich", sorted = true)
    } yield (), timeout)
    whenReady(client.lsDir(BaseDirectory + "/Artists", recursive = true, sorted = true)) { r =>
      r shouldBe a [EtcdListResponse]
      val mapNodes = r.node.nodes.map(nodes => (nodes.indices zip nodes).toMap)
      mapNodes map { case nodes =>
        nodes(0).value should === (Some("Ferenc Fricsay"))
        nodes(1).value should === (Some("Daniel Barenboim"))
        nodes(2).value should === (Some("Martha Argerich"))
      }
    }
  }

  test("setKey should set a new key") {
    whenReady(client.setKey(BaseDirectory + "/Bach", "Baroque")) { r =>
      r shouldBe a [EtcdSingleResponse]
      r.action should === ("set")
      r.node.key should === (s"/$BaseDirectory/Bach")
      r.node.value should === (Some("Baroque"))
      r.prevNode should === (None)
    }
  }

  test("setKey should allow to create a nested key") {
    whenReady(client.setKey(BaseDirectory + "/Composers/Bach", "Baroque")) { r =>
      r shouldBe a [EtcdSingleResponse]
      r.action should === ("set")
      r.node.key should === (s"/$BaseDirectory/Composers/Bach")
      r.node.value should === (Some("Baroque"))
      r.prevNode should === (None)
    }
  }

  test("setKey should allow to have sorted keys") {
    Await.ready(for {
      _ <- client.createDir(BaseDirectory + "/Bach")
    } yield (), timeout)
    whenReady(client.setKey(BaseDirectory + "/Bach/Works", "Johannes Passion", sorted = true)) { r =>
      r shouldBe a [EtcdSingleResponse]
      r.action should === ("create")
      r.node.key should startWith (s"/$BaseDirectory/Bach/Works/")
      r.node.value should === (Some("Johannes Passion"))
    }
  }

  test("getKey should get a previously created key") {
    val response = for {
      _ <- client.setKey(BaseDirectory + "/Bach", "Baroque")
      r <- client.getKey(BaseDirectory + "/Bach")
    } yield r
    whenReady(response) { r =>
      r shouldBe a [EtcdSingleResponse]
      r.action should === ("get")
      r.node.key should === (s"/$BaseDirectory/Bach")
      r.node.value should === (Some("Baroque"))
      r.prevNode should === (None)
    }
  }

  test("setKey should get information about the node previous state") {
    val response = for {
      _ <- client.setKey(BaseDirectory + "/Bach", "Baroque")
      r <- client.setKey(BaseDirectory + "/Bach", "Ancient")
    } yield r
    whenReady(response) { r =>
      r shouldBe a [EtcdSingleResponse]
      r.action should ===("set")
      r.node.key should === (s"/$BaseDirectory/Bach")
      r.node.value should === (Some("Ancient"))
      r.prevNode.map { case node =>
        node.key should === (s"/$BaseDirectory/Bach")
        node.value should === (Some("Baroque"))
      }
    }
  }

  test("setting a key with ttl should trigger its disappearance") {
    Await.ready(for {
      _ <- client.setKey(BaseDirectory + "/Bach", "Baroque", ttl = 1 second)
    } yield (), timeout)
    var response = for {
      r <- client.getKey(BaseDirectory + "/Bach")
    } yield r
    whenReady(response) { r =>
      r shouldBe a [EtcdSingleResponse]
      r.node.key should === (s"/$BaseDirectory/Bach")
      r.node.ttl should === (Some(1))
      r.node.expiration should not be None
    }
    Thread.sleep(1500)
    response = for {
      r <- client.getKey(BaseDirectory + "/Bach")
    } yield r
    whenReady(response.failed) { e =>
      e shouldBe a [KeyNotFoundException]
    }
  }

  test("unset expiration makes the key to remain") {
    Await.ready(for {
      _ <- client.setKey(BaseDirectory + "/Bach", "Baroque", ttl = 1 second)
    } yield (), timeout)
    var response = for {
      r <- client.unsetExpiration(BaseDirectory + "/Bach", "Baroque")
    } yield r
    whenReady(response) { r =>
      r shouldBe a [EtcdSingleResponse]
      r.node.key should === (s"/$BaseDirectory/Bach")
      r.node.value should be (Some("Baroque"))
      r.node.ttl should be (None)
      r.node.expiration should be (None)
      r.prevNode map { case node => {
          node.key should === (s"/$BaseDirectory/Bach")
          node.value should === (Some("Baroque"))
          node.ttl should not be None
          node.expiration should not be None
        }
      }
    }
    Thread.sleep(1500)
    response = for {
      r <- client.getKey(BaseDirectory + "/Bach")
    } yield r
    whenReady(response) { r =>
      r shouldBe a [EtcdSingleResponse]
      r.node.key should === (s"/$BaseDirectory/Bach")
    }
  }

  test("deleteKey removes a key and returns the previous node value") {
    val response = for {
      _ <- client.setKey(BaseDirectory + "/Bach", "Baroque")
      r <- client.deleteKey(BaseDirectory + "/Bach")
    } yield r
    whenReady(response) { r =>
      r shouldBe a [EtcdSingleResponse]
      r.action should === ("delete")
      r.node.key should === (s"/$BaseDirectory/Bach")
      r.node.value should === (None)
      r.prevNode.map { case node =>
        node.key should === (s"/$BaseDirectory/Bach")
        node.value should === (Some("Baroque"))
      }
    }
  }

}

