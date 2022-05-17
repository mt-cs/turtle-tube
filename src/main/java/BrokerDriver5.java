import controllers.applicationframework.ReplicationApp;
import model.config.ReplicationConfig;
import util.Constant;
import util.LoggerSetup;
import util.ReplicationAppUtils;


/**
 * ReplicationApp Driver
 *
 * @author marisatania
 */
public class BrokerDriver5 {
  /**
   * Cmd line arguments:
   * -type broker -config config/configBrokerReplication5.json -log broker5.log
   *
   * @param args arguments
   */
  public static void main(String[] args) {
    LoggerSetup.setup(args[Constant.CONFIG_LENGTH - 1]);

    ReplicationConfig config = ReplicationAppUtils.runConfig(args);
    ReplicationApp applicationHandler
        = new ReplicationApp(ReplicationAppUtils.getType(), config);

    applicationHandler.runApplication();
  }
}
