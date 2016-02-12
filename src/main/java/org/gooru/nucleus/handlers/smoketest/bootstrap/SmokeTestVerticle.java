package org.gooru.nucleus.handlers.smoketest.bootstrap;

import java.util.Map;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.gooru.nucleus.handlers.smoketest.app.components.SmokeTestRegistry;
import org.gooru.nucleus.handlers.smoketest.constants.MessageConstants;
import org.gooru.nucleus.handlers.smoketest.constants.TCStructureConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmokeTestVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(SmokeTestVerticle.class);
  private static final Logger SUMMARYLOG = LoggerFactory.getLogger("org.gooru.nucleus.tcsummary");
  private EventBus    eventBus;

  @Override
  public void start(Future<Void> voidFuture) throws Exception {
    
    LOGGER.debug("SmokeTestVerticle : Start ...");

    eventBus = vertx.eventBus();

    vertx.executeBlocking(blockingFuture -> startApplication(), future -> {
      if (future.succeeded()) {
        LOGGER.debug("SmokeTestVerticle : Start application succeeded ...");
                
        voidFuture.complete();
      } else {
        LOGGER.debug("SmokeTestVerticle : Start application failed ...");
        
        eventBus = null;
        voidFuture.fail("Not able to initialize the Smoketest machinery properly");
      }
    });
        
  }

  @Override
  public void stop() throws Exception {
    shutDownApplication();
    super.stop();
  }

  private void startApplication() {
    
    try {
      SmokeTestRegistry.getInstance().initializeComponent(vertx, config());
    } catch (IllegalStateException ie) {
      LOGGER.error("Error initializing application", ie);
      Runtime.getRuntime().halt(1);
    }

    LOGGER.debug("SmokeTestVerticle : Run tests after 5 sec delay.");
    // set a 5 sec timer to allow for other verticles to initialize properly....before firing smoke tests
    vertx.setTimer( 5* 1000, handler -> {
      try {
        LOGGER.debug("SmokeTestVerticle : About to run tests...");
        runTests();
        
      } catch (Exception e) {
        LOGGER.error("SmokeTestVerticle : runTests : Exception - ", e);
      }      
    });
        
  }

  private void shutDownApplication() {
    SmokeTestRegistry.getInstance().finalizeComponent();
  }
  
  private void runTests() {
    LOGGER.debug("SmokeTestVerticle : runTests : About to run tests ...");
    
    if (eventBus == null) {
      LOGGER.error("Unable to run smoke tests!! Verticles not setup correctly!");
      return;
    }
    
    // run our tests here....
    JsonArray testCaseList = SmokeTestRegistry.getInstance().getTestCaseList();
    LOGGER.debug("SmokeTestVerticle : runTests : Number of testcases: " + testCaseList.size() );
    
    for(int i = 0; i < testCaseList.size(); i++) {
      LOGGER.debug("SmokeTestVerticle : runTests : TC : " +  i );
      JsonObject tcSetup = testCaseList.getJsonObject(i);
      runTest(tcSetup);
    }    
    
    return;
  }
  
  private void runTest(JsonObject tc) {
    LOGGER.debug("SmokeTestVerticle : runTest : TC : " +  tc.getString(TCStructureConstants.TC_ITEM_ID) );
    
    JsonObject bodyForMsg = getBodyForMessage(tc);
    LOGGER.debug("SmokeTestVerticle : runTest : Body : " +  bodyForMsg.toString() );

    DeliveryOptions options = getDeliveryOptions(tc);
    LOGGER.debug("SmokeTestVerticle : runTest : Header : " +  options.getHeaders().toString() );
    
    eventBus.send(tc.getString(TCStructureConstants.TC_ITEM_VERTICLE), bodyForMsg, options, reply -> responseHandler(reply, tc));
  }
  
  private DeliveryOptions getDeliveryOptions(JsonObject tc) {
    LOGGER.debug("SmokeTestVerticle : getDeliveryOptions : ... ");
    DeliveryOptions options = new DeliveryOptions().setSendTimeout(5000);
    options.addHeader(MessageConstants.MSG_HEADER_OP, tc.getString(TCStructureConstants.TC_ITEM_OPERATION));
    
    JsonObject headerObj = tc.getJsonObject(TCStructureConstants.TC_ITEM_HEADERS);
    if (headerObj != null) {
      for (Map.Entry<String, Object> entry : headerObj) {
        LOGGER.debug("SmokeTestVerticle : getDeliveryOptions : Key : " + entry.getKey() + " Value : " + entry.getValue().toString());
        options.addHeader(entry.getKey(), entry.getValue().toString());
      }
    }
    
    return options;
  }

  private JsonObject getBodyForMessage(JsonObject tc) {
    JsonObject result = new JsonObject();
    JsonObject tcBody = tc.getJsonObject(TCStructureConstants.TC_ITEM_BODY);
    if (tcBody == null) tcBody = new JsonObject();
    
    if (TCStructureConstants.SPECIAL_VERTICLE.equalsIgnoreCase(tc.getString(TCStructureConstants.TC_ITEM_VERTICLE))) {
      result.put(MessageConstants.MSG_EVENT_NAME, tc.getString(TCStructureConstants.TC_ITEM_OPERATION));
      result.put(MessageConstants.MSG_EVENT_BODY, tc.getJsonObject(TCStructureConstants.TC_ITEM_BODY));
    } else {
      result.put(MessageConstants.MSG_HTTP_BODY, tcBody);   
      result.put(MessageConstants.MSG_KEY_PREFS, (JsonObject) tc.getJsonObject(TCStructureConstants.TC_ITEM_PREFS));
      result.put(MessageConstants.MSG_USER_ID, tc.getString(TCStructureConstants.TC_ITEM_USERID) );      
    }
    return result;
  }
    
  private void responseHandler(AsyncResult<Message<Object>> reply, JsonObject tc) {
    if (reply.succeeded()) {
      printTestCaseSuccessSummary(tc, (JsonObject) reply.result().body());
      LOGGER.debug("SmokeTestVerticle : runTest : Reply : " + reply.result().body() );
      LOGGER.debug("Successfully done.");
    } else {
      LOGGER.error("SmokeTestVerticle : runTest : Reply : Failed. 500 : " + reply.cause() );
      printTestCaseFailureSummary(tc, reply.cause());
    }
  }
  
  private void printTestCaseSuccessSummary(JsonObject tc, JsonObject body) {
    SUMMARYLOG.error("* TestCase : ID     : " + tc.getString(TCStructureConstants.TC_ITEM_ID) );    
    SUMMARYLOG.error("* TestCase : NAME   : " + tc.getString(TCStructureConstants.TC_ITEM_TITLE) );    
    SUMMARYLOG.error("* TestCase : HEADER : " + tc.getJsonObject(TCStructureConstants.TC_ITEM_HEADERS) );    
    SUMMARYLOG.error("* TestCase : BODY   : " + tc.getJsonObject(TCStructureConstants.TC_ITEM_BODY) );    
    
    if (TCStructureConstants.SPECIAL_VERTICLE.equalsIgnoreCase(tc.getString(TCStructureConstants.TC_ITEM_VERTICLE))) {
      // for event verticle there is no response back...so we just report that event is sent.
      SUMMARYLOG.error("* TestCase : REPLY  : NA");    
      SUMMARYLOG.error("* TestCase : STATUS : OK");    
    } else {
      SUMMARYLOG.error("* TestCase : REPLY  : " + ((body == null) ? "null" : body.getJsonObject(MessageConstants.MSG_HTTP_BODY) ) );    
      SUMMARYLOG.error("* TestCase : STATUS : " + ((body == null) ? "null" : body.getInteger(MessageConstants.MSG_HTTP_STATUS) ) );    
    }
    
    SUMMARYLOG.error("*********************************************************************");                
  }
  
  private void printTestCaseFailureSummary(JsonObject tc, Throwable cause) {
    SUMMARYLOG.error("* TestCase : ID     : " + tc.getString(TCStructureConstants.TC_ITEM_ID) );    
    SUMMARYLOG.error("* TestCase : NAME   : " + tc.getString(TCStructureConstants.TC_ITEM_TITLE) );    
    SUMMARYLOG.error("* TestCase : HEADER : " + tc.getJsonObject(TCStructureConstants.TC_ITEM_HEADERS) );    
    SUMMARYLOG.error("* TestCase : BODY   : " + tc.getJsonObject(TCStructureConstants.TC_ITEM_BODY) );    
    
    String strCause = cause.toString();
    if ( TCStructureConstants.SPECIAL_VERTICLE.equalsIgnoreCase(tc.getString(TCStructureConstants.TC_ITEM_VERTICLE))
         && strCause.contains("TIMEOUT")  ) {
        // we are possibly looking at: "(TIMEOUT,-1) Timed out waiting for reply"
        // for event verticle there is no response back...so we just report that event is sent.
        SUMMARYLOG.error("* TestCase : REPLY  : Message sent successfully.");    
        SUMMARYLOG.error("* TestCase : STATUS : OK" );    
    } else {
      SUMMARYLOG.error("* TestCase : REPLY  : " + cause );    
      SUMMARYLOG.error("* TestCase : STATUS : 500" );    
    }

    SUMMARYLOG.error("*********************************************************************");
  }
    
}
