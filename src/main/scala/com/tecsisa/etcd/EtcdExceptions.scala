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

object EtcdExceptions {

  private val KeyNotFoundMessage = "The key was not found: "
  private val UnforbiddenOperationMessage =
    "Unforbidden operation. See if you're creating a directory and this already exists: "

  abstract class EtcdException(message: String, cause: Throwable = null) extends RuntimeException(message, cause)

  case class KeyNotFoundException(key: String, cause: Throwable = null)
    extends EtcdException(KeyNotFoundMessage + key, cause)

  case class UnforbiddenOperationException(key: String, cause: Throwable = null)
    extends EtcdException(UnforbiddenOperationMessage + key, cause)

}
