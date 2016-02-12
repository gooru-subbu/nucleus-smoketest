package org.gooru.nucleus.handlers.smoketest.app.components;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.gooru.nucleus.handlers.smoketest.constants.TCStructureConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmokeTestRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(SmokeTestRegistry.class);
  private volatile boolean initialized = false;
  private JsonObject configObject;
  private JsonArray testCaseList;

  private SmokeTestRegistry() {
    // TODO Auto-generated constructor stub
  }

  public static SmokeTestRegistry getInstance() {
    return Holder.INSTANCE;
  }

  public void initializeComponent(Vertx vertx, JsonObject config) {
    // Skip if we are already initialized
    LOGGER.debug("Initialization called upon.");
    if (!initialized) {
      LOGGER.debug("May have to do initialization");
      // We need to do initialization, however, we are running it via verticle instance which is going to run in
      // multiple threads hence we need to be safe for this operation
      synchronized (Holder.INSTANCE) {
        LOGGER.debug("Will initialize after double checking");
        if (!initialized) {
          this.configObject = config.copy();
          this.testCaseList = this.configObject.getJsonArray(TCStructureConstants.TC_LIST_HEAD);
          initialized = true;
          LOGGER.debug("Initialization done.");
        }
      }
    }
  }
  
  public void finalizeComponent() {
  }
  
  public JsonArray getTestCaseList() {
    if (initialized)
      return testCaseList;
    else
      return null;
  }

  private static class Holder {
    private static final SmokeTestRegistry INSTANCE = new SmokeTestRegistry();
  }

}
