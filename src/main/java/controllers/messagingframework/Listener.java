package controllers.messagingframework;

import interfaces.FaultInjector;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * Create server socket
 *
 * @author marisatania
 */
public class Listener {
  private ServerSocket listener;
  private Socket socket;
  private final FaultInjector faultInjector;
  private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Constructor for Listener
   *
   * @param port          the port number
   * @param faultInjector inject failure
   */
  public Listener(int port, FaultInjector faultInjector) {
    this.faultInjector = faultInjector;
    try {
      this.listener = new ServerSocket(port);
      LOGGER.info("Listener is waiting for connection...");
    } catch (IOException ioe) {
      LOGGER.warning("IOException when instantiating server socket: " + ioe);
    }
  }

  /**
   * Accept next connection
   *
   * @return connection socket
   */
  public controllers.messagingframework.ConnectionHandler nextConnection() {
    try {
      this.socket = listener.accept();
      LOGGER.info("Connection established.");
    } catch (IOException ioe) {
      LOGGER.warning("IOException when accepting next connection: " + ioe);
    }
    return new controllers.messagingframework.ConnectionHandler(socket, faultInjector);
  }
}
