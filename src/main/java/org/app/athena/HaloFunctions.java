package org.app.athena;

import com.google.adk.tools.Annotations;
import com.google.adk.tools.ToolContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.app.athena.HaloResources.Address;

public class HaloFunctions {

    private static final Logger LOGGER = LoggerFactory.getLogger(HaloFunctions.class);
    private static void note(String functionName, Object sessionId, Object location){
        LOGGER.info("[f {}] Location '{}', SessionId '{}'",functionName,location,sessionId);
    }


    public static final String FUNC_GET_COLLECTION_POINTS = "getCashCollectionPoints";
    public static final String FUNC_GET_ACTIVE_RELIEF_FUND_REGISTRAR = "getActiveReliefFundRegistrar";
    public static final String FUNC_GET_WALLET_BALANCE = "getWalletBalance";
    public static final String AGENT_DESCRIPTION = "A Agent to help and assist users to locate and help with banking and cash transfer";
    public static final String AGENT_INSTRUCTION = """
                                As an Chat Agent providing cash and banking assistance for People who might not have access to bank accounts, cash & any atm or credit cards. 
                                You Would be assisting People my providing the below services. 
                                1.  Find the addresses of the Closest Halo Cash Collection Points based on the current location of the person. 
                                2.  If they ask how can i get some emergency cash you can provide 2 options.
                                        a) check if there are any Relief fund registrar close by and provide them the details else if none available inform them there are none close by.
                                        b) Let the person know they can create a shareable 'HaloContactCode' which can be shared with contacts by message or chat to transfer emergency funds.
                                3.  If the Person would like to generate a new 'HaloContactCode' use the tools to create an URL and Code and inform them they have a limit of only transferring 100$ per day.
                                4.  If the person asks to check the wallet, Show user the current wallet balance and if the balance is more than 1$ You should check if there are any open bank branches with Quick Account Opening, get the addresses and inform person they can open an account with minimal documents at those location.
                                5.  Address the person by name when replying.

                                """;

    @Annotations.Schema(name = "get_address_locations_for_halo_cash_collection_points",
            description = "Retrieve the Available Addresses for Halo Cash Collection Points closest to the place where the person is.")
    public static Map<String, Address> getCashCollectionPoints(
            @Annotations.Schema(name = "toolContext")
            ToolContext toolContext
    ) {
        final var sessionIdCopy = toolContext.state().computeIfAbsent(HaloResources.SESSION_ID_COPY,(a)->"Not Found!");
        final var location = toolContext.state().computeIfAbsent(HaloResources.LOCATION,(a)->HaloResources.DEFAULT_VALID_LOCATION);
        note(FUNC_GET_COLLECTION_POINTS,sessionIdCopy,location);
        return HaloResources.cash_collection_points.entrySet().stream()
                .filter((e)-> e.getValue().isCityMatch(location.toString()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Annotations.Schema(name = "get_address_for_active_relief_fund_registrar",
            description = "Retrieve the Available Addresses of the active Relief Fund Registrar closest to the place where the person is.")
    public static Map<String, Address> getActiveReliefFundRegistrar(
            @Annotations.Schema(name = "toolContext")
            ToolContext toolContext
    ) {
        final var sessionIdCopy = toolContext.state().computeIfAbsent(HaloResources.SESSION_ID_COPY,(a)->"Not Found!");
        final var location = toolContext.state().computeIfAbsent(HaloResources.LOCATION,(a)->HaloResources.DEFAULT_VALID_LOCATION);
        note(FUNC_GET_ACTIVE_RELIEF_FUND_REGISTRAR,sessionIdCopy,location);

        return Map.of();
    }


    @Annotations.Schema(name = "get_balance_in_wallet",
            description = "Retrieve the balance available in the persons wallet.")
    public static Map<String, Object> getWalletBalance(
            @Annotations.Schema(name = "toolContext")
            ToolContext toolContext
    ) {
        final var sessionIdCopy = toolContext.state().computeIfAbsent(HaloResources.SESSION_ID_COPY,(a)->"Not Found!");
        final var location = toolContext.state().computeIfAbsent(HaloResources.LOCATION,(a)->HaloResources.DEFAULT_VALID_LOCATION);
        note(FUNC_GET_WALLET_BALANCE,sessionIdCopy,location);

        var ccy = toolContext.state().computeIfAbsent(HaloResources.CURRENCY,(a)->"SGD");
        var bal = toolContext.state().computeIfAbsent(HaloResources.BALANCE,(a)->0.0);
        return Map.of(
                "Currency", ccy,
                "Balance", bal
        );}
}
