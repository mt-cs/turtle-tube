package interfaces;

import java.io.IOException;

/**
 * Receiver interface
 *
 * @author marisatania
 */
public interface Receiver {
  byte[] receive() throws IOException;
}
