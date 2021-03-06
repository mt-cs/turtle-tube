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
  public static final int TIMER_COUNT = 30000;
  public static final int PRODUCER_TIMER_COUNTER = 20000;
  public static final int MAX_BYTES = 8000;
  public static final int MAX_OFFSET_SIZE = 100;
  public static final int TIMEOUT = 10000;
  public static final int DELAY = 7000;
  public static final int MAX_BOUND = 3;
  public static final int SECOND = 1000000;
  public static final int RT = 3000;
  public static final int RF = 2;
  public static final int LB_PORT = 1061;
  public static final double LOSS_RT = 0.2;
  public static final long HEARTBEAT_INTERVAL = 1000L;

  public static final String TYPE_FLAG = "-type";
  public static final String CONFIG_FLAG = "-config";
  public static final int CONFIG_LENGTH = 6;

  public static final int PRODUCER_TYPE = 0;
  public static final int BROKER_TYPE = 1;
  public static final int CONSUMER_TYPE = 2;
  public static final int LOADBALANCER_TYPE = 3;
  public static final int PUSH_BASED_CONSUMER_TYPE = 4;
  public static final int SUBSCRIBER = 5;

  public static final String PRODUCER = "producer";
  public static final String BROKER = "broker";
  public static final String CONSUMER = "consumer";
  public static final String LOADBALANCER = "loadbalancer";
  public static final String PULL = "pull";
  public static final String PUSH = "push";
  public static final String FOLLOWER = "follower";
  public static final String LEADER = "leader";

  public static final String CLOSE = "close";
  public static final String SNAPSHOT = "snapshot";
  public static final String LAST_SNAPSHOT =  "last snapshot";

  public static final String ALIVE = "ALIVE";
  public static final String CONNECT = "CONNECT";
  public static final String ELECTION = "ELECTION";
  public static final String VICTORY = "VICTORY";
  public static final String CANDIDATE = "CANDIDATE";

  public static final String COPY_LOG = "_copy.log";
  public static final String OFFSET_LOG = "_offset.log";
  public static final String LOCALHOST = "localhost";
  public static final String LB = "localhost:1061";
  public static final String listener = "Listener ";
  public static final String USER_DIR = "user.dir";
  public static final String SEPARATOR = ":";
  public static final String SLASH = "/";
  public static final String UNDERSCORE = "_";
  public static final String SPACE = " ";
}
