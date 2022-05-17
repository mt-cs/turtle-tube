package controllers.pubsubframework;

import com.google.protobuf.InvalidProtocolBufferException;

import controllers.faultinjector.FaultInjectorFactory;
import controllers.membershipmodule.MembershipUtils;
import controllers.messagingframework.ConnectionHandler;
import controllers.messagingframework.Listener;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import model.Membership;
import model.Membership.MemberInfo;
import util.Constant;

/**
 * Load balancer class
 *
 * @author marisatania
 */
public class LoadBalancer {
  private final int port;
  private final boolean isRunning;
  private volatile String leaderHost;
  private volatile int leaderPort;
  private volatile boolean isLeaderSelected;
  private final ReentrantLock reentrantLock;
  private final ConcurrentHashMap<ConnectionHandler, MemberInfo> leaderConnectionMap;
  private final ConcurrentHashMap<ConnectionHandler, MemberInfo> producerConnectionMap;
  private final ConcurrentHashMap<ConnectionHandler, MemberInfo> consumerConnectionMap;
  private final ConcurrentHashMap<ConnectionHandler, MemberInfo> followerConnectionMap;
  private final ConcurrentLinkedQueue<MemberInfo> followerQueue;
  private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Constructor for load balancer
   *
   * @param port load balancer port
   */
  public LoadBalancer(int port) {
    this.port = port;
    this.isRunning = true;
    this.isLeaderSelected = false;
    this.reentrantLock = new ReentrantLock();
    this.followerQueue = new ConcurrentLinkedQueue<>();
    this.followerConnectionMap = new ConcurrentHashMap<>();
    this.leaderConnectionMap = new ConcurrentHashMap<>();
    this.producerConnectionMap = new ConcurrentHashMap<>();
    this.consumerConnectionMap = new ConcurrentHashMap<>();
  }

  /**
   * Listening for connection
   */
  public void listenToConnection() {
    Thread loadBalancerServer = new Thread( () -> {
      Listener listener = new Listener(port,
          new FaultInjectorFactory(0).getChaos());
      while (isRunning) {
        LOGGER.info("Listening for connection on port: " + port);
        ConnectionHandler connection = listener.nextConnection();
        Thread listenerThread = new Thread( () -> receiveRequest(connection));
        listenerThread.start();
      }
    });
    loadBalancerServer.start();
  }

  /**
   * Distribute data according data type value
   */
  private void receiveRequest(ConnectionHandler connection) {
    Membership.MemberInfo requestInfo = null;
    while (isRunning) {

        byte[] requestInfoArr;
        try {
          requestInfoArr = connection.receive();
        } catch (IOException ioe) {
          LOGGER.warning("IOException in load balancer receive request: " + ioe.getMessage());
          if (followerConnectionMap.containsKey(connection)) {
            requestInfo = followerConnectionMap.get(connection);
            LOGGER.info("Removing failed follower: " + requestInfo.getId());
            followerQueue.remove(requestInfo);
            followerConnectionMap.remove(connection);
          } else if (leaderConnectionMap.containsKey(connection)){
            requestInfo = leaderConnectionMap.get(connection);
            LOGGER.info("Removing failed leader: " + requestInfo.getId());
            leaderConnectionMap.remove(connection);
            isLeaderSelected = false;
          }
          break;
        }

        if (requestInfoArr != null) {
          try {
            requestInfo = Membership.MemberInfo.parseFrom(requestInfoArr);
          } catch (InvalidProtocolBufferException e) {
            LOGGER.warning("Error load balancer receive request: " + e.getMessage());
          }

          if (requestInfo != null) {
            int leaderId = requestInfo.getId();

            if (requestInfo.getTypeValue() == 1) {
              if (requestInfo.getIsLeader()) {
                reentrantLock.lock();
                leaderHost = requestInfo.getHost();
                leaderPort = requestInfo.getPort();
                reentrantLock.unlock();
                LOGGER.info("Current leader: " + leaderId);
                isLeaderSelected = true;
                leaderConnectionMap.put(connection, requestInfo);
              } else {
                followerQueue.add(requestInfo);
                LOGGER.info("Added follower to queue: " + requestInfo.getId());
                followerConnectionMap.put(connection, requestInfo);
              }
            } else if (requestInfo.getTypeValue() == 0) {
              if (isLeaderSelected) {
                MembershipUtils.sendBrokerLocation(connection, leaderId,
                    leaderHost, leaderPort, isLeaderSelected, Constant.LOADBALANCER_TYPE);
              }
              producerConnectionMap.put(connection, requestInfo);
            } else if (requestInfo.getTypeValue() == 2) {
              if (isLeaderSelected) {
                MembershipUtils.sendBrokerLocation(connection, leaderId,
                    leaderHost, leaderPort, isLeaderSelected, Constant.LOADBALANCER_TYPE);
              }
              consumerConnectionMap.put(connection, requestInfo);
            } else if (requestInfo.getTypeValue() == 4) {
              MemberInfo followerInfo = followerQueue.poll();
              if (followerInfo != null) {
                LOGGER.info("Current follower: " + followerInfo.getId());
                MembershipUtils.sendBrokerLocation(connection,
                    followerInfo.getId(), followerInfo.getHost(),
                    followerInfo.getPort(), followerInfo.getIsLeader(),
                    Constant.LOADBALANCER_TYPE);
              }
            }
          }
        } else {
          connection.close();
          break;
        }
    }
    connection.close();
  }
}