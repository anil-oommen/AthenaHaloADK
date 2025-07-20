package org.app.athena;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HaloResources {
    public static String LOCATION = "user:location";
    public static String NAME = "user:name";
    public static String CURRENCY = "currency";
    public static String BALANCE ="balance";
    public static String SESSION_ID_COPY = "session-id-copy";
    public static String SESSION_CREATED_TIME = "session-created-time";

    public static final String DEFAULT_VALID_LOCATION = "Singapore";
    public static final String DEFAULT_NAME = "James";
    private static final DateTimeFormatter dtFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    public static record Address(String city, String street, String building, String googlemap){
        boolean isCityMatch(String other){
            return StringUtils.equalsIgnoreCase(other,city);
        }
    };


    public static ConcurrentHashMap<String, Object> mapIt(String sessionId, String name, String location, String ccy, Double amount){
        ConcurrentHashMap<String, Object> sessionMap = new ConcurrentHashMap<String,Object>();
        sessionMap.put(NAME,name);
        sessionMap.put(LOCATION,location);
        sessionMap.put(CURRENCY,ccy);
        sessionMap.put(BALANCE,amount);
        sessionMap.put(SESSION_ID_COPY,sessionId);
        sessionMap.put(SESSION_CREATED_TIME, LocalDateTime.now().format(dtFormat));
        return sessionMap;
    }

    public static final Map<String, Address>  cash_collection_points = Map.of(
            "001", new Address("Singapore","Shenton Way","One Raffles Place","https://maps.app.goo.gl/Q6TNh7VUr21pxmJv8"),
            "002", new Address("Singapore","Raffles City","Shenton Way","https://maps.app.goo.gl/Q6TNh7VUr21pxmJv8"),
            "003", new Address("Jakarta","No. 80 Jalan Imam Bonjol","Deutsche Bank Building","https://maps.app.goo.gl/Q6TNh7VUr21pxmJv8"),
            "004", new Address("Manilla","Bonifacio Global City","Crescent Park West","https://maps.app.goo.gl/Q6TNh7VUr21pxmJv8")
    );


}
