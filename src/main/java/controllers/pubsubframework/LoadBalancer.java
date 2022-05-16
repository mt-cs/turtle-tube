package controllers.pubsubframework;

import com.google.protobuf.InvalidProtocolBufferException;

import controllers.faultinjector.FaultInjectorFactory;
import controllers.membershipmodule.MembershipUtils;
import controllers.messagingframework.ConnectionHandler;
import controllers.messagingframework.Listener;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import model.Membership;
import model.Membership.MemberInfo;

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
  private ConcurrentLinkedQueue<MemberInfo> followerQueue;
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
          isLeaderSelected = false;
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
                MembershipUtils.sendLeaderLocation(connection,
                    leaderId, leaderHost, leaderPort);
              } else {
                followerQueue.add(requestInfo);
                LOGGER.info("Added member to queue: " + requestInfo.getPort());
              }
            } if (requestInfo.getTypeValue() == 4) {
              MemberInfo followerInfo = followerQueue.poll();
              LOGGER.info("Current follower: " + followerInfo.getId());
              MembershipUtils.sendBrokerLocation(connection,
                  followerInfo.getId(), followerInfo.getHost(),
                  followerInfo.getPort(), followerInfo.getIsLeader());
            } else {
              if (isLeaderSelected) {
                MembershipUtils.sendLeaderLocation(connection,
                    leaderId, leaderHost, leaderPort);
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