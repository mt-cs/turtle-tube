<h1 align="center" style="display: block; font-size: 2.5em; font-weight: bold; margin-block-start: 1em; margin-block-end: 1em;">
  <br><br><strong>TURTLE TUBE</strong><br>
  <img src="turtle-animal-svgrepo-com.svg" style="width:200px"/>
  <br>Fault-Tolerant Leader-based Publish/Subscribe Replication System<br>
</h1>
<p align="center">
A reliable stream processing communication engine to enable ingesting and processing data in real-time within a distributed system <br><br>
</p>


## Table of contents[![](https://user-images.githubusercontent.com/60201466/166403770-b5813248-17d5-4b23-acfe-cf60936d539f.svg)](#table-of-contents)
1. [Description](https://github.com/mt-cs/turtle-tube/edit/main/README.md#description-)
2. [Architecture](https://github.com/mt-cs/turtle-tube/edit/main/README.md#architecture-)
3. [Run Configurations](https://github.com/mt-cs/turtle-tube/edit/main/README.md#run-configurations-)
4. [Features](https://github.com/mt-cs/turtle-tube/edit/main/README.md#features-)
5. [Design Principles](https://github.com/mt-cs/turtle-tube/edit/main/README.md#design-principles-)

---

## Description[![](https://user-images.githubusercontent.com/60201466/166403770-b5813248-17d5-4b23-acfe-cf60936d539f.svg)](#description)

**TURTLE TUBE** manages communication within a distributed network by ensuring reliability, fault-tolerant, and consistency within a leader-based replication in a publish/subscribe system. The system works on an assumption that all nodes have a crash stop model failure model with an accurate failure detector.

The TURTLE TUBE engine guarantees that:
* A failure of one application does not cause a crash of the system.
* The consumer are automatically notified about available leader status.
* All followers will catch up with replication during the join procedure.
* Strong consistency, a consumer must receive all messages in order.
* Options for pull-based and push-based consumers.
* Consumers can connect to followers and not just leaders.
* Persistent storage and send message log by offset
* Ensure the rf is maintained during node failure

---

## Architecture [![](https://user-images.githubusercontent.com/60201466/166403770-b5813248-17d5-4b23-acfe-cf60936d539f.svg)](#architecture)

![turtle-tube-architecture](https://user-images.githubusercontent.com/60201466/170609711-94f31e68-82cb-42cf-9d4c-b9af380e020e.jpg)

---

## Features[![](https://user-images.githubusercontent.com/60201466/166403770-b5813248-17d5-4b23-acfe-cf60936d539f.svg)](#features)

> 💡 The TURTLE TUBE pub/sub system handles:

* Membership and failure detection using the bully algorithm and heartbeat gossiping system
* Reliability using ACK approach
* Inject failure to handle latency
* Synchronous and partial asynchronous replication 
* Leader-based system using bully election
* Dynamically add new instances of the Broker during program execution
* A pull-based Consumer API similar to the original Kafka design.



_Here are the TURTLE TUBE additional features:_
| Features | Description |
| --- | --- |
|**Persist Log to Disk and Use Byte Offsets as Message IDs**| Flush the segment files to disk only after a configurable number of messages have been published.|
|**Push-based Subscriber**| a Consumer to be push-based and the Broker be stateful. |
|**Replication factor**| The leader will replicate the topic to *rf* followers rather than all followers. |
|**Pull-based reads from followers**| A Consumer may connect to a follower to subscribe to a topic.|

---

## Run Configurations [![](https://user-images.githubusercontent.com/60201466/166403770-b5813248-17d5-4b23-acfe-cf60936d539f.svg)](#run-configurations)

```
java -cp <jar file> ReplicationDriver -type <node type> -config <config json file> -log <output log file>
```

Run the main driver, `ReplicationDriver.java`, using different configurations specified in `./config`. For examples:

1. **Load Balancer**
```
java -cp dsd-project.jar ReplicationDriver -type loadbalancer -config config/configLoadBalancerReplication.json -log loadbalancer.log
```

2. **Broker**
```
java -cp dsd-project.jar ReplicationDriver -type broker -config config/configBrokerReplication1.json -log broker1.log
```

3. **Producer**
```
java -cp dsd-project.jar ReplicationDriver -type broker -config config/configProducerReplication1.json -log producer1.log
```

2. **Consumer**
```
java -cp dsd-project.jar ReplicationDriver -type broker -config config/configConsumerReplication.json -log consumer1.log
```

---

## Design Principles [![](https://user-images.githubusercontent.com/60201466/166403770-b5813248-17d5-4b23-acfe-cf60936d539f.svg)](#design-principles)

### Messaging Framework with Fault Injection

TURTLE TUBE messaging framework provides the ability to send and receive messages over a network and to inject interfaces failure by losing or delaying messages.

#### Fault Injector

There are two implementations of the Sender's and Receiver's fault injector:
* Default Implementation: Send and receive as expected, failing only if the underlying network connection actually fails.
* Lossy Implementation: Allow the developer to inject interfaces failure. Specify a **loss rate** and a **delay parameter** for both sender and receiver.
  - Loss rate: specified as a decimal from 0 to 1, will define the probability that a message is lost in the network. A loss rate of .1 would indicate that an average of 10% of messages will be lost.
  - Delay parameter: specified as an integer delay, will define the maximum delay of a message in milliseconds. Assume actual delay is uniformly distributed from 0 to delay.

#### Communication

Communication happens over TCP using the java.nio AsynchronousChannel APIs.
TURTLE TUBE uses [Protocol Buffers](https://developers.google.com/protocol-buffers) to communicate between sender and receiver so that the `send`/`receive` messages wrap/unwrap the `byte[]` appropriately.

#### Concurrent Connection

A host is able to support any number of concurrent connections. Once a connection is established, a host is able to send and receive from that connection. Though a connection must be initiated by a client to a server, once the connection is established the hosts will behave as peers.

1. **May 9** - **Persist Log to Disk and Use Byte Offsets as Message ID** Basic implementation
2. **May 12** - **Push-based Subscriber** Design and implement a mechanism for a Consumer to register to receive updates to a topic. The Broker will proactively push out new messages to any registered consumers.
3. **May 15** - **Replication with persistent storage and push-based** Update persistent storage implementation to handle replication with snapshot using multiple files for each topic. Update push-based subscriber to handle node failure during replication.
4. **May 16** - **Pull-based reads from followers** A Consumer may connect to a follower to subscribe to a topic. If that follower fails, the Consumer will reconnect to active followers and specify its start point in the message stream
5. **May 17** - **Replication factor** Allow the creator of a topic to specify a replication factor (*rf*) for that topic. When a follower fails all topics it is storing must be redistributed to one or more other followers to ensure the rf is maintained

<!-- markdownlint-enable -->
