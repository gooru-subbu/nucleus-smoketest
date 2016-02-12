package org.gooru.nucleus.handlers.smoketest.constants;

public class TCStructureConstants {
  public static final String TC_LIST_HEAD       = "testcases";
  public static final String TC_ITEM            = "testcase";
  public static final String TC_ITEM_ID         = "id";
  public static final String TC_ITEM_TITLE      = "title";
  public static final String TC_ITEM_VERTICLE   = "verticle";
  public static final String TC_ITEM_METHOD     = "method";
  public static final String TC_ITEM_USERID     = "userId";
  public static final String TC_ITEM_PREFS      = "prefs";
  public static final String TC_ITEM_HEADERS    = "headers";
  public static final String TC_ITEM_BODY       = "body";
  public static final String TC_ITEM_OPERATION  = "operation";  
  
  public static final String SPECIAL_VERTICLE   = "org.gooru.nucleus.message.bus.publisher.event";
}

/*  
 * EXAMPLE STRUCTURE...
 
{
 "testcases" : [
    "testcase" : {
      "id"       : "tc1",
      "title"    : "resource create test case",
      "verticle" : "org.gooru.nucleus.message.bus.resource",
      "method"   : "POST",
      "operation": "resource.create",
      "userId"   : "",
      "prefs"    : {},
      "headers"  : null,
      "body"     : {
                    "title": "t-1",
                    "url": "www.t1.com",
                    "narration": "n-1 narration",
                    "description": "d-1 description",
                    "content_format": "resource",
                    "content_subformat": "webpage_resource",
                    "is_copyright_owner": false,
                    "visible_on_profile": false,
                    "metadata": { },
                   },
    },
    
    "testcase" : {
      "id"       : "tc2",
      "title"    : "resource get test case",
      "verticle" : "org.gooru.nucleus.message.bus.resource",
      "method"   : "GET",
      "operation": "resource.get",
      "userId"   : "",
      "prefs"    : {},
      "headers"  : {
                    "resourceId"    : ""                    
                   },
      "body"     : null,
    }
    ]
}
*/