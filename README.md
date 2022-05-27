<h1 align="center" style="display: block; font-size: 2.5em; font-weight: bold; margin-block-start: 1em; margin-block-end: 1em;">
  <br><br><strong>TURTLE TUBE</strong><br>
  <img src="turtle-animal-svgrepo-com.svg" style="width:200px"/>
  <br>Fault-Tolerant Leader-based Publish/Subscribe Replication System<br>
</h1>
<p align="center">
A reliable stream processing communication engine to enable ingesting and processing data in real-time within a distributed system <br><br>
</p>


## Table of contents[![](https://user-images.githubusercontent.com/60201466/166403770-b5813248-17d5-4b23-acfe-cf60936d539f.svg)](#table-of-contents)
1. [Description](https://github.com/mt-cs/turtle-tube#description)
2. [Architecture](https://github.com/mt-cs/turtle-tube#architecture-)
3. [Features](https://github.com/mt-cs/turtle-tube/blob/main/README.md#features)
4. [Run Configurations](https://github.com/mt-cs/turtle-tube/edit/main/README.md#run-configurations-)
5. [Design Principles](https://github.com/mt-cs/turtle-tube/edit/main/README.md#design-principles-)
   - [Messaging Framework with Fault Injection](https://github.com/mt-cs/turtle-tube/edit/main/README.md#-messaging-framework-with-fault-injection)
   - [Reliable Data Transfer](https://github.com/mt-cs/turtle-tube/edit/main/README.md#-reliable-data-transfer)
   - [Publish/Subscribe System](https://github.com/mt-cs/turtle-tube/edit/main/README.md#-publishsubscribe-system)
   - [Fault Tolerant](https://github.com/mt-cs/turtle-tube/edit/main/README.md#-fault-tolerant)
   - [Strong Consistency](https://github.com/mt-cs/turtle-tube/edit/main/README.md#-strong-consistency)
   - [Membership and Failure Detection](https://github.com/mt-cs/turtle-tube/edit/main/README.md#-membership-and-failure-detection)
   - [Replication](https://github.com/mt-cs/turtle-tube/edit/main/README.md#-replication)
   - [Election](https://github.com/mt-cs/turtle-tube/edit/main/README.md#-election)
   - [Asynchronous Followers](https://github.com/mt-cs/turtle-tube/edit/main/README.md#-asynchronous-followers)
   - [Persist Log to Disk and Use Byte Offsets as Message ID](https://github.com/mt-cs/turtle-tube/edit/main/README.md#-persist-log-to-disk-and-use-byte-offsets-as-message-id)
   - [Push-based/ Pull-based Subscriber](https://github.com/mt-cs/turtle-tube/edit/main/README.md#-push-based-pull-based-subscriber)
   - [Reads From Followers](https://github.com/mt-cs/turtle-tube/edit/main/README.md#-reads-from-followers)
   - [Replication Factor](https://github.com/mt-cs/turtle-tube/edit/main/README.md#-replication-factor)
6. [Datasets](https://github.com/mt-cs/turtle-tube/edit/main/README.md#datasets-)

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
|**Push-based Subscriber**| A Consumer to be push-based and the Broker be stateful. |
|**Replication factor**| The leader will replicate the topic to *rf* followers rather than all followers. |
|**Reads from followers**| A Consumer may connect to a follower to subscribe to a topic.|

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

### 🐢 Messaging Framework with Fault Injection

TURTLE TUBE messaging framework provides the ability to send and receive messages over a network and to inject interfaces failure by losing or delaying messages.

#### Fault Injector

There are two implementations of the Sender's and Receiver's fault injector:
* **Default Implementation**: Send and receive as expected, failing only if the underlying network connection actually fails.
* **Lossy Implementation**: Allow the developer to inject interfaces failure. Specify a **loss rate** and a **delay parameter** for both sender and receiver.
  - **Loss rate**: specified as a decimal from 0 to 1, will define the probability that a message is lost in the network. A loss rate of .1 would indicate that an average of 10% of messages will be lost.
  - **Delay parameter**: specified as an integer delay, will define the maximum delay of a message in milliseconds. Assume actual delay is uniformly distributed from 0 to delay.

#### Communication

Communication happens over TCP using the java.nio AsynchronousChannel APIs.
TURTLE TUBE uses [Protocol Buffers](https://developers.google.com/protocol-buffers) to communicate between sender and receiver so that the `send`/`receive` messages wrap/unwrap the `byte[]` appropriately.

#### Concurrent Connection

A host is able to support any number of concurrent connections. Once a connection is established, a host is able to send and receive from that connection. Though a connection must be initiated by a client to a server, once the connection is established the hosts will behave as peers.

### 🐢 Reliable Data Transfer
TURTLE TUBE's reliable data transfer protocol works over the `Lossy` connection messaging framework described above. A receiver host will ACKnowledge data as it is received, and a sender host will retransmit data that is detected as lost. This solution implements stop-and-wait protocol.

### 🐢 Publish/Subscribe System
#### Producer

`Producer` API may be used by an application running on any host that publishes messages to a broker. `Producer` will allow the application to do the following:

1. Connect to a `Load Balancer` and request the leader address
2. Connect to a leader `Broker`
3. Send data to the leader `Broker` by providing a `byte[]` containing the data and a `String` containing the topic.

TURTLE TUBE uses a simplified [Kafka API](https://kafka.apache.org/documentation/#producerapi) implementation and omits the use of the Properties object and simply pass the relevant information into the Producer constructor. It also omits the use of generic types and assume that the send method accepts a byte[] of data. TURTLE TUBE's Producer API is as follows:

```
// Open a connection to the Load Balancer by creating a new Producer object
Producer producer = new Producer(loadBalancerLocation, producerId);

producer.connectToLoadBalancer();
producer.getLeaderAddress();

// Connect to the leader broker
producer.connectToBroker();

StreamProcessor streamProcessor = new StreamProcessor(producerConfig.getFilename());
ArrayList<LogStream> streamsList = streamProcessor.processStream();

// Send data
for (LogStream stream : streamsList) {
	producer.send(stream.getTopic(), stream.getData());
}

// Close the connection 
producer.close();

```

#### Consumer

The `Consumer` API that may be used by an application running on any host that consumes messages from a broker. The `Consumer` will allow the application to do the following:

1. Connect to a `Load Balancer` and request the leader or follower `Broker` address
2. Connect to a `Broker`
3. Retrieve data from the `Broker` using a pull-based or push-based approach by specifying a topic of interest and a starting offset in the message stream

Below is the simplified API for the Pull-Based Consumer:
```
// Specify the location of the broker, topic of interest for this specific
// consumer object, and a starting offset position in the message stream.

// Connect to the consumer
Consumer consumer = new Consumer(loadBalancerLocation, topic, startingPosition, push/pull, leader/follower, consumerId);

// Continue to pull messages...forever
while(true) {
	byte[] message = consumer.poll(Duration.ofMillis(100));
	// do something with this data!
}

// When forever finally finishes...
consumer.close();
```

In a real world application, the `Consumer` would do something with the data consumed. For demonstration purposes the consumer will just save it to a log file. 


#### Broker

The `Broker` will accept an unlimited number of connection requests from producers and consumers. 


### 🐢 Fault Tolerant

The goal of this project is to provide `Broker` fault tolerance by replicating all data stored by the `Broker` onto *n* other instances of the `Broker` running on different hosts. With *n+1* `Broker` instances, your solution must be able to tolerate the failure of up to *n*. As long as one `Broker` is available, all `Producer` and `Consumer` requests must succeed.

### 🐢 Strong Consistency

A `Consumer` must receive all messages in order. It is possible that a `Broker` receives a message from a `Producer` and fails before it is replicated to ***any of*** the followers. The ***only*** time your system will have data loss is in this scenario. If *even one* follower receives a message before the leader fails that must be provided to the `Consumer`.


### 🐢 Membership and Failure Detection

The `Broker` will maintain a membership list that will be dynamically updated as hosts fail or come online. 

**Dynamic Join:** Dynamically add new instances of the Broker during program execution. The membership table should only contain the live members at any given time. A new Broker may be configured with the IP/port of one existing Broker. It is possible that not all `Broker` instances will be online when a specific `Broker` starts up. TURTLE TUBE handle this case by retrying the connection until the `Broker` is available. TURTLE TUBE assumes that the first `Broker` that comes online is the leader.

**Heartbeat:** All`Broker` instances will send regular heartbeat messages to all other `Broker` instances in their membership tables. If a `Broker` *b1* does not hear from another `Broker` *b2* for some configurable delay period it will suspect it has failed. If *b2* is a follower it can be removed from the membership table. If *b2* is the leader, *b1* will begin an election.

### 🐢 Replication

**Synchronous Replication:** When the leader receives a new message from a `Producer` it will ***synchronously*** replicate the message to all followers in the membership table. The `Producer` request will not complete until all followers have the new data. 

**Join Procedure:** When a new follower joins it will retrieve a snapshot of the database from the leader or another follower. This makes sure that the new follower receives all future updates and does not miss any writes.

### 🐢 Election

**Bully election:** TURTLE TUBE implements the Bully distributed election algorithm to replace a crash-stop failure of the leader. It is possible that multiple `Broker` instances discover the failed leader simultaneously and simultaneously start the election. The Bully algorithm handles this case.

**Consistency:** If the leader was in the process of a write operation when it failed it is possible that some followers have the data and some do not. Once the new leader is elected, TURTLE TUBE ensures that the most recent data is replicated to all followers.

**Producer/Consumer Notification:** The Bully algorithm requires the the new leader send a *victory* message to the followers. In addition, the load balancer will notify all `Producer` and `Consumer` instances of the new leader.

### 🐢 Asynchronous Followers
Allow followers to be asynchronous. Some followers may be configured as asynchronous.Producer might receive confirmation that a send has completed before the follower receives the update. 

### 🐢 Persist Log to Disk and Use Byte Offsets as Message ID
TURTLE TUBE saves the log to permanent storage and using byte offsets rather than integer message IDs for identifying the appropriate consumer read location. The implementation derived from the following design described in the original [Kafka paper](http://notes.stephenholiday.com/Kafka.pdf): *“For better performance, we flush the segment files to disk only after a configurable number of messages have been published or a certain amount of time has elapsed. A message is only exposed to the consumers after it is flushed.”*

![kafka](https://kafka.apache.org/32/images/kafka_log.png)

### 🐢 Push-based/ Pull-based Subscriber
TURTLE TUBE implement both pull-based and push-based mechanisms. In the pull-based approach, the `Broker` will be stateless with respect to the `Consumer` hosts. Push-based is a mechanism for a Consumer to register to receive updates to a topic. The Broker is stateful and will proactively push out new messages to any registered consumers.

### 🐢 Reads From Followers
A Consumer may connect to a follower to subscribe to a topic. If that follower fails, the Consumer will reconnect to active followers and specify its start point in the message stream

### 🐢 Replication Factor
TURTLE TUBE allows the creator of a topic to specify a replication factor (*rf*) for that topic. When a follower fails all topics it is storing must be redistributed to one or more other followers to ensure the rf is maintained

---

## Datasets [![](https://user-images.githubusercontent.com/60201466/166403770-b5813248-17d5-4b23-acfe-cf60936d539f.svg)](#datasets)

Each application will mimic a front-end web server by replaying data from logs. Here are the data sets used to emulate web application log stream:
- [Kaggle](https://www.kaggle.com/eliasdabbas/web-server-access-logs)
- [Loghub](https://github.com/logpai/loghub)

<!-- markdownlint-enable -->
