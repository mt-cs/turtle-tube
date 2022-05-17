package controllers.messagingframework;

import interfaces.FaultInjector;
import interfaces.Receiver;
import interfaces.Sender;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Logger;

/**
 * A connection is bi-directional
 *
 * @author marisatania
 */
public class ConnectionHandler implements Sender, Receiver {
  private Socket socket;
  private FaultInjector faultInjector;
  private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /**
   * Connection for listener
   * that takes in a socket
   *
   * @param socket connection socket
   */
  public ConnectionHandler(Socket socket, FaultInjector faultInjector) {
    this.socket = socket;
    this.faultInjector = faultInjector;
  }

  /**
   * Connection for initiator
   * that takes in host name and port number
   *
   * @param hostName host name
   * @param port     port number
   */
  public ConnectionHandler(String hostName, int port, FaultInjector faultInjector) {
    try {
      this.socket = new Socket(hostName, port);
      this.faultInjector = faultInjector;
    } catch (IOException ioe) {
      LOGGER.warning("IOException in creating : " + ioe);
    }
  }

  /**
   * Send byte array message
   *
   * @param message byte[]
   * @return true if sent
   */
  @Override
  public boolean send(byte[] message) {
    try {
      DataOutputStream outputStream =
          new DataOutputStream(this.socket.getOutputStream());
      if (!faultInjector.shouldFail()) {
        faultInjector.injectFailure();
        outputStream.writeInt(message.length);
        outputStream.write(message);
        outputStream.flush();
        return true;
      }
    } catch (Exception e) {
      LOGGER.warning(e.getMessage());
    }
    return false;
  }

  /**
   * Receive byte[] from connection
   *
   * @return byteArray
   */
  @Override
  public byte[] receive() throws IOException {
    byte[] byteArray;

    if (faultInjector.shouldFail()) {
      return new byte[0];
    }

    DataInputStream inputStream =
        new DataInputStream(this.socket.getInputStream());
    faultInjector.injectFailure();
    int byteLength = inputStream.readInt();
    byteArray = inputStream.readNBytes(byteLength);

    return byteArray;
  }

  /**
   * Receive byte[] from connection
   *
   * @return byteArray
   */
  public byte[] receiveInfo() {
    byte[] byteArray = null;

    if (faultInjector.shouldFail()) {
      return new byte[0];
    }

    try {
      DataInputStream inputStream =
          new DataInputStream(this.socket.getInputStream());
      faultInjector.injectFailure();
      int byteLength = inputStream.readInt();
      byteArray = inputStream.readNBytes(byteLength);
    } catch (IOException ioe) {
      LOGGER.warning("IOException in receive in stream message: " + ioe.getMessage());
    }
    return byteArray;
  }


  /**
   * Close connection
   */
  public void close() {
    try {
      socket.close();
    } catch (IOException e) {
      LOGGER.warning("Error while closing connection socket" + e.getMessage());
    }
  }
}

