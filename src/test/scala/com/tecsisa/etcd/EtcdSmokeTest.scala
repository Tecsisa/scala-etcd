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

package com.tecsisa.etcd

import org.scalatest.{BeforeAndAfter, Matchers, FunSuite}
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._
import scala.language.postfixOps

class EtcdSmokeTest extends FunSuite
  with BeforeAndAfter with ScalaFutures with Matchers {

  implicit val p = PatienceConfig(timeout = 1 second)

  private val BaseDirectory = "testing"
  private val client = EtcdClient.local

  before {

    // delete testing directory
    var response = client.deleteDir(BaseDirectory)
    whenReady(response) { result =>
      println(result)
    }

    // create tests directory
//    response = client.createDir(BaseDirectory)
//    whenReady(response) { result =>
//      println(result)
//    }

  }

  test ("dummy") {
    println("")
  }

}
