scala-etcd
==========

A scala client library for [etcd].

It uses [dispatch] to provide asynchronous none blocking HTTP communication with the [etcd v2 API].

In order to create an instance of this client, just type:

```Scala

  val client = EtcdClient.remote("host_ip", [host_port])

```

Parameter *port* is optional being used 4001 by default. In scenarios where you have a local etcd service, i.e. in a
CoreOS cluster, you could just type:

```Scala

  val client = EtcdClient.local

```

## Examples of use

Please see the [smoke testing] for examples of use.

## Next Steps

* Currently, test suite needs a local instance of etcd running. We need to get rid of this limitation.
* Etcd advanced scenarios as locking or comparing.

[etcd]: https://coreos.com/docs/distributed-configuration/getting-started-with-etcd
[dispatch]: http://dispatch.databinder.net/Dispatch.html
[etcd v2 API]: https://coreos.com/docs/distributed-configuration/etcd-api/
[smoke testing]: https://github.com/Tecsisa/scala-etcd/blob/master/src/test/scala/com/tecsisa/etcd/EtcdSmokeTest.scala

