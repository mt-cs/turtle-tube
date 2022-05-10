package util;

import java.nio.charset.StandardCharsets;

/**
 * String constants
 *
 * @author marisatania
 * ?
 */
public class Constant {
  public static final int POLL_TIMEOUT = 4000;
  public static final int NUM_THREADS = 100;
  public static final int NUM_QUEUE = 1000;
  public static final int POLL_FREQ = 3000;
  public static final int MAX_PULL = 100;
  public static final int MAX_BYTES = 30000;
  public static final int MAX_OFFSET_SIZE = 500;
  public static final int TIMEOUT = 10000;
  public static final int DELAY = 7000;
  public static final int MAX_BOUND = 3;
  public static final int SECOND = 1000000;
  public static final int RT = 3000;
  public static final int LB_PORT = 1061;
  public static final double LOSS_RT = 0.2;


  public static final String TYPE_FLAG = "-type";
  public static final String LOG_FLAG = "-log";
  public static final String CONFIG_FLAG = "-config";
  public static final String LOCATION_FLAG = "-location";
  public static final String FILE_FLAG = "-file";
  public static final int ARGS_LENGTH = 7;
  public static final int CONFIG_LENGTH = 6;

  public static final String PRODUCER = "producer";
  public static final String BROKER = "broker";
  public static final String CONSUMER = "consumer";
  public static final String LOADBALANCER = "loadbalancer";

  public static final String CLOSE = "close";
  public static final String SNAPSHOT = "snapshot";
  public static final String LAST_SNAPSHOT =  "last snapshot";

  public static final String ALIVE = "ALIVE";
  public static final String CONNECT = "CONNECT";
  public static final String ELECTION = "ELECTION";
  public static final String VICTORY = "VICTORY";
  public static final String CANDIDATE = "CANDIDATE";

  public static final String LOCALHOST = "localhost";
  public static final String LB = "localhost:1061";
  public static final String listener = "Listener ";
  public static final String USER_DIR = "user.dir";
  public static final String SEPARATOR = ":";
  public static final String SLASH = "/";
  public static final String SPACE = " ";
  public static final byte[] NL = "\n".getBytes(StandardCharsets.UTF_8);

}
