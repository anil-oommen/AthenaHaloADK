package org.app.athena.web;

import org.app.athena.HaloChatAgent;
import org.app.athena.HaloResources;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.function.BiFunction;

@RestController
public class HaloWebController {


    private final HaloChatAgent chatAgent;

    public HaloWebController(HaloChatAgent chatAgent){
        this.chatAgent = chatAgent;
    }

    public record ChatRequest(String chatText,String location, String name){
        public ChatRequest(String chatText){
            this(chatText, HaloResources.DEFAULT_VALID_LOCATION,HaloResources.DEFAULT_NAME);
        }

    };
    public record ChatResponse(String chatText, String sessionStartTime, String code, String linkUrl){
        public ChatResponse(String chatText, String sessionStartTime, String code ){
            this(chatText,sessionStartTime,null,null);
        }

        public ChatResponse(String chatText){
            this(chatText,null,"SUCCESS",null);
        }

    };

    static final String SESSION_REGEX = "^[a-zA-Z0-9]{5,20}"; //Alpha Numeric 5 to 20 chars long
    static final BiFunction<String,Double,Boolean> validSessionIdAndAmount = (s, t)->
            s!=null && s.matches(SESSION_REGEX) && t!=null;
    static final BiFunction<String,String,Boolean> validSessionIdAndText = (s, t)->
            s!=null && s.matches(SESSION_REGEX) && t!=null && t.length()>5;



    @RequestMapping("/halo-agent/{sessionId}")
    public ResponseEntity<?> chatWithAgent(
            @PathVariable(value = "sessionId",required = true) String sessionId,
            @RequestBody ChatRequest chatRequest
            ) {
        if(!validSessionIdAndText.apply(sessionId,chatRequest.chatText)){
            return ResponseEntity.badRequest().body("Invalid SessionId(AlphaNum5) Or Empty/Incomplete chatText");
        }
        return ResponseEntity.ok(chatAgent.interact(sessionId,chatRequest));
    }

    @GetMapping("/halo-agent-get/{sessionId}")
    public ResponseEntity<?> chatWithAgentGet(
            @PathVariable(value = "sessionId",required = true) String sessionId,
            @RequestParam(value = "chatText",required = true) String chatText
    ) {
        if(!validSessionIdAndText.apply(sessionId,chatText)){
            return ResponseEntity.badRequest().body("Invalid SessionId(AlphaNum5) Or Empty/Incomplete chatText");
        }

        return ResponseEntity.ok(chatAgent.interact(sessionId,new ChatRequest(chatText)));
    }

    @RequestMapping("/halo-wallet/{sessionId}")
    public ResponseEntity<?> creditDebitAccount(
            @PathVariable(value = "sessionId",required = true) String sessionId,
            @RequestParam(value = "amount",defaultValue = "0.0") Double amount
    ) {
        if(!validSessionIdAndAmount.apply(sessionId,amount)){
            return ResponseEntity.badRequest().body("Invalid SessionId(AlphaNum5) Or Empty/Incomplete Amount");
        }
        return ResponseEntity.ok(chatAgent.creditDebit(sessionId,amount));
    }
}
