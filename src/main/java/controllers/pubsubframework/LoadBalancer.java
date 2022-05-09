package controllers.pubsubframework;

import com.google.protobuf.InvalidProtocolBufferException;

import controllers.faultinjector.FaultInjectorFactory;
import controllers.membershipmodule.MembershipUtils;
import controllers.messagingframework.ConnectionHandler;
import controllers.messagingframework.Listener;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import model.Membership;

/**
 * Load balancer class
 *
 * @author marisatania
 */
public class LoadBalancer {
  private final int port;
  private boolean isRunning;
  private volatile String leaderHost;
  private volatile int leaderPort;
  private volatile boolean isLeaderSelected;
  private final ReentrantLock reentrantLock;
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
    Membership.MemberInfo leaderInfo = null;
    while (isRunning) {

        byte[] leaderInfoArr;
        try {
          leaderInfoArr = connection.receive();
        } catch (IOException ioe) {
          LOGGER.warning("IOException in load balancer receive request: " + ioe.getMessage());
          isLeaderSelected = false;
          break;
        }

        if (leaderInfoArr != null) {
          try {
            leaderInfo = Membership.MemberInfo.parseFrom(leaderInfoArr);
          } catch (InvalidProtocolBufferException e) {
            LOGGER.warning("Error load balancer receive request: " + e.getMessage());
          }

          if (leaderInfo != null) {
            int leaderId = leaderInfo.getId();

            if (leaderInfo.getTypeValue() == 1) {
              reentrantLock.lock();
              leaderHost = leaderInfo.getHost();
              leaderPort = leaderInfo.getPort();
              reentrantLock.unlock();
              LOGGER.info("Current leader: " + leaderId);
              isLeaderSelected = true;
              MembershipUtils.sendLeaderLocation(connection,
                  leaderId, leaderHost, leaderPort);
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

  /**
   * Close listener
   */
  public void close() {
    this.isRunning = false;
  }
}