package org.app.athena;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.events.Event;
import com.google.adk.events.EventActions;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.adk.tools.FunctionTool;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import org.app.athena.web.HaloWebController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Component()
@Scope("singleton")
public class HaloChatAgent {
    private static final Logger LOGGER = LoggerFactory.getLogger(HaloChatAgent.class);

    private static final String AGENT_NAME = "athena-halo-chat-agent";
    private static final String MODEL_NAME = "gemini-2.5-flash";
    private static final String USER_ID = "user-id-001";

    public static BaseAgent ROOT_AGENT = initAgent();
    public static InMemoryRunner AGENT_RUNNER;

    public HaloChatAgent(){
        AGENT_RUNNER = new InMemoryRunner(ROOT_AGENT);
    }

    //Map<String, HaloResources> chatSessions = new ConcurrentHashMap<String, HaloResources>();

    public HaloWebController.ChatResponse interact(String sessionId, HaloWebController.ChatRequest chatRequest){

        Maybe<Session> mayBeSession = AGENT_RUNNER.sessionService().getSession(AGENT_NAME,USER_ID,sessionId, Optional.empty());
        try {
            Session sessionIfExisting = mayBeSession.toFuture().get(1, TimeUnit.SECONDS);
            Session userSession = Optional.ofNullable(sessionIfExisting)
                    .orElseGet(()->{
                        LOGGER.info("[+] Creating new Session for User '{}', Session '{}' , Location '{}'",chatRequest.name(), sessionId, chatRequest.location());
                        var sessionState = HaloResources.mapIt(sessionId,chatRequest.name(),chatRequest.location(),"SGD",0.0);
                        var newSession = AGENT_RUNNER.sessionService()
                                .createSession(ROOT_AGENT.name(), USER_ID,
                                        sessionState, sessionId).blockingGet();

                        /* Setting the UserProfile using EventAction */
                        EventActions actionsWithUpdate = EventActions.builder().stateDelta(sessionState).build();
                        Event profileEvent = Event.builder()
                                .invocationId("init_profile")
                                .author("system")
                                .actions(actionsWithUpdate)
                                .timestamp(System.currentTimeMillis())
                                .build();
                        AGENT_RUNNER.sessionService()
                                .appendEvent(newSession, profileEvent).blockingGet();
                        return newSession;

                    });

            LOGGER.info("[> {}] '{}'", sessionId, chatRequest.chatText());
            Content userMsgForHistory = Content.fromParts(Part.fromText(chatRequest.chatText()));
            Flowable<Event> events = AGENT_RUNNER.runAsync(userSession.userId() , userSession.id(), userMsgForHistory);

            //System.out.print("\\nAgent > ");
            final StringBuilder agentResponseBuilder = new StringBuilder();
            final AtomicBoolean toolCalledInTurn = new AtomicBoolean(false);
            final AtomicBoolean toolErroredInTurn = new AtomicBoolean(false);

            events.blockingForEach(event -> processAgentEvent(sessionId, event, agentResponseBuilder,
                    toolCalledInTurn, toolErroredInTurn));

            //System.out.println();

            if (toolCalledInTurn.get() && !toolErroredInTurn.get()
                    && agentResponseBuilder.length() == 0) {
                LOGGER.warn("Agent used a tool but provided no text response.");
            } else if (toolErroredInTurn.get()) {
                LOGGER.warn(
                        "An error occurred during tool execution or in the agent's response processing.");
            }

            return new HaloWebController.ChatResponse(agentResponseBuilder.toString());

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error("Error Chat Agent interact failed",e);
            throw new RuntimeException(e);
        }
    }
    public Double creditDebit(String sessionId, Double amount){
        Maybe<Session> mayBeSession = AGENT_RUNNER.sessionService().getSession(AGENT_NAME,USER_ID,sessionId, Optional.empty());
        try {
            Session sessionNow = mayBeSession.toFuture().get(1, TimeUnit.SECONDS);
            Optional.ofNullable(sessionNow).orElseThrow(()-> new RuntimeException("Agent Runner Session Not Found, Check if sessionId is correct." + sessionId));
            var sessionState = sessionNow.state();

            var currentBalance = sessionState.getOrDefault(HaloResources.BALANCE,0.0);
            var newBalance = Optional.of(currentBalance)
                            .filter(Double.class::isInstance)
                                    .map(Double.class::cast)
                    .map(c-> c +amount)
                                            .orElseGet(()->amount);
            sessionState.put(HaloResources.BALANCE,newBalance);

            /* Update back the agent using Events using Runner Session Service
             */
            EventActions actionsWithUpdate = EventActions.builder().stateDelta(sessionState).build();
            Event systemEvent = Event.builder()
                    //.invocationId("invocation_id")
                    .author("system")
                    .actions(actionsWithUpdate)
                    .timestamp(System.currentTimeMillis())
                    .build();
            AGENT_RUNNER.sessionService().appendEvent(sessionNow,systemEvent).blockingGet();

            LOGGER.info("[$ {}] Wallet Old/Change/New {}/{}/{}", sessionId, currentBalance,amount,newBalance);
            return newBalance;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOGGER.error("Error Chat Agent interact failed",e);
            throw new RuntimeException(e);
        }
    }


    private static BaseAgent initAgent() {
        var tools = List.of(
                FunctionTool.create(HaloFunctions.class, HaloFunctions.FUNC_GET_COLLECTION_POINTS),
                FunctionTool.create(HaloFunctions.class, HaloFunctions.FUNC_GET_ACTIVE_RELIEF_FUND_REGISTRAR),
                FunctionTool.create(HaloFunctions.class, HaloFunctions.FUNC_GET_WALLET_BALANCE)
        );
        return LlmAgent.builder().name(AGENT_NAME).description(HaloFunctions.AGENT_DESCRIPTION)
                .model(MODEL_NAME)
                .instruction(HaloFunctions.AGENT_INSTRUCTION)
                .tools(tools).build();
    }

    private static void processAgentEvent(String sessionId, Event event, StringBuilder agentResponseBuilder,
                                          AtomicBoolean toolCalledInTurn, AtomicBoolean toolErroredInTurn) {
        if (event.content().isPresent()) {
            event.content().get().parts().ifPresent(parts -> {
                for (Part part : parts) {
                    if (part.text().isPresent()) {
                        //System.out.print(part.text().get());
                        LOGGER.info("[< {}] '{}'", sessionId, part.text().get());
                        agentResponseBuilder.append(part.text().get());
                    }
                    if (part.functionCall().isPresent()) {
                        toolCalledInTurn.set(true);
                    }
                    if (part.functionResponse().isPresent()) {
                        FunctionResponse fr = part.functionResponse().get();
                        fr.response().ifPresent(responseMap -> {
                            if (responseMap.containsKey("error")
                                    || (responseMap.containsKey("status")
                                    && "error".equalsIgnoreCase(
                                    String.valueOf(responseMap.get("status"))))) {
                                toolErroredInTurn.set(true);
                            }
                        });
                    }
                }
            });
        }
        if (event.errorCode().isPresent() || event.errorMessage().isPresent()) {
            toolErroredInTurn.set(true);
        }
    }


}
