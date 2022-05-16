<h1 align="center" style="display: block; font-size: 2.5em; font-weight: bold; margin-block-start: 1em; margin-block-end: 1em;">
  <br><br><strong>TURTLE TUBE</strong>
  <br><br>Fault-Tolerant Leader-based Publish/Subscribe Replication System
</h1>


## Overview[![pin](https://user-images.githubusercontent.com/60201466/166403770-b5813248-17d5-4b23-acfe-cf60936d539f.svg)](#overview)

**TURTLE TUBE** is a reliable stream processing communication engine to enable ingesting and processing data in real-time within a distributed system. Turtle Tube works on an assumption that all nodes have a crash stop model failure model
with an accurate failure detector.

---

## Table of contents[![](https://user-images.githubusercontent.com/60201466/166403770-b5813248-17d5-4b23-acfe-cf60936d539f.svg)](#table-of-contents)
1. [Motivation](#motivation)
2. [Composotion](#composition)
3. [Proposed Features](#proposed-features)
4. [Milestones](#milestones)
5. [Deliverables](#deliverables)

---

## Motivation[![](https://user-images.githubusercontent.com/60201466/166403770-b5813248-17d5-4b23-acfe-cf60936d539f.svg)](#motivation)

For the final project, I would like to implement a set of the additional feature options that I have not implemented in managing communication within a distributed network. I believe that by completing the suggested features, I can learn more about reliability, fault-tolerant, leader-based replication, and consistency in a publish/subscribe system fully as the course intended. I named this project Turtle Tube with the hope that slow and steady wins the race.

---

## Composition[![](https://user-images.githubusercontent.com/60201466/166403770-b5813248-17d5-4b23-acfe-cf60936d539f.svg)](#composition)

> 💡 In current version, the TURTLE TUBE pub/sub system handles:

* Membership and failure detection using the bully algorithm and heartbeat gossiping system
* Reliability using ACK approach
* Inject failure to handle latency
* Synchronous and partial asynchronous replication 
* Leader-based system using bully election
* Dynamically add new instances of the Broker during program execution
* A pull-based Consumer API similar to the original Kafka design.

---

## Proposed Features [![](https://user-images.githubusercontent.com/60201466/166403770-b5813248-17d5-4b23-acfe-cf60936d539f.svg)](#proposed-features)

_Here are the TURTLE TUBE proposed features:_
| Features | Description |
| --- | --- |
|**Persist Log to Disk and Use Byte Offsets as Message IDs**| Flush the segment files to disk only after a configurable number of messages have been published.|
|**Push-based Subscriber**| a Consumer to be push-based and the Broker be stateful. |
|**Replication factor**| The leader will replicate the topic to *rf* followers rather than all followers. |
|**Pull-based reads from followers**| A Consumer may connect to a follower to subscribe to a topic.|

---

## Milestones [![](https://user-images.githubusercontent.com/60201466/166403770-b5813248-17d5-4b23-acfe-cf60936d539f.svg)](#milestones)

TURTLE TUBE milestones:
1. **May 7** - **Improving Current Implementation** Implement instructor and peer-review feedback, fix bugs if there is any.
2. **May 9** - **Persist Log to Disk and Use Byte Offsets as Message ID** Basic implementation
3. **May 12** - **Push-based Subscriber** Design and implement a mechanism for a Consumer to register to receive updates to a topic. The Broker will proactively push out new messages to any registered consumers.
4. **May 15** - **Replication with persistent storage and push-based** Update persistent storage implementation to handle replication with snapshot using multiple files for each topic. Update push-based subscriber to handle node failure during replication.
5. **May 16** - **Pull-based reads from followers** A Consumer may connect to a follower to subscribe to a topic. If that follower fails, the Consumer will reconnect to active followers and specify its start point in the message stream
6. **May 17** - **Replication factor** Allow the creator of a topic to specify a replication factor (*rf*) for that topic. When a follower fails all topics it is storing must be redistributed to one or more other followers to ensure the rf is maintained

## Deliverables [![](https://user-images.githubusercontent.com/60201466/166403770-b5813248-17d5-4b23-acfe-cf60936d539f.svg)](#deliverables)

The TURTLE TUBE engine guaranties that:
* The crash-stop failure of one application does not cause the crash of the system.
* The consumer are automatically notified about available leader status.
* All followers will catch up with replication during the join procedure.
* Strong consistency, a consumer must receive all messages in order.
* Options for pull-based and push-based consumers.
* Consumers can connect to followers and not just leaders.
* Persistent storage and send message log by offset
* Ensure the rf is maintained during node failure

<!-- markdownlint-enable -->
