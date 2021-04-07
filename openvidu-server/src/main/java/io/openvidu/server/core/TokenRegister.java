package io.openvidu.server.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This service class represents all the tokens currently registered by an active sessions
 * Each time a token is created, it is registered by {@link SessionManager} using {@link #registerToken(String, Participant, Token)} (String, Participant, Token)}
 * All registered tokens will be present until {@link SessionManager} calls the method {@link #deregisterTokens(String)}
 *
 * The purpose of this service is to know when a token was registered into a session with its correspondent connectionId
 * All maps of this class are present to be able to verify this in the most optimal way
 */
public class TokenRegister {

    private final ConcurrentHashMap<String, Token> tokensRegistered = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Token>> tokensRegisteredBySession = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Participant> participantsByTokens = new ConcurrentHashMap<>();

    /**
     * Register a token of an specific active session
     * @param sessionId Id of the sessions where the token is generated
     * @param token Token to register
     */
    protected synchronized void registerToken(String sessionId, Participant participant, Token token) {
        this.tokensRegisteredBySession.putIfAbsent(sessionId, new ConcurrentHashMap<>());
        ConcurrentHashMap<String, Token> registeredTokensInSession = this.tokensRegisteredBySession.get(sessionId);
        this.tokensRegistered.put(token.getToken(), token);
        this.participantsByTokens.put(token.getToken(), participant);
        registeredTokensInSession.put(token.getToken(), token);
    }

    /**
     * Deregister all tokens of an specific session which is not active
     * @param sessionId Id of the session which is no longer active
     */
    protected synchronized void deregisterTokens(String sessionId) {
        if (this.tokensRegisteredBySession.containsKey(sessionId)) {
            for(Map.Entry<String, Token> tokenRegisteredInSession: tokensRegistered.entrySet()) {
                this.tokensRegistered.remove(tokenRegisteredInSession.getKey());
                this.participantsByTokens.remove(tokenRegisteredInSession.getKey());
            }
            this.tokensRegisteredBySession.remove(sessionId);
        }
    }

    /**
     * Check if the current token string was registered in an active session
     * @param token Token string to check if it is registered
     * @param connectionId Id of the connection to check
     * @param sessionId Id of session to check
     * @return <code>true</code> if token was registered. <code>false</code> otherwise
     */
    public boolean isTokenRegistered(String token, String connectionId, String sessionId) {
        if (!this.tokensRegistered.containsKey(token)) {
            // False because token is not registered
            return false;
        }

        if (!this.tokensRegisteredBySession.containsKey(sessionId)) {
            // False because session is not registered with the specified token
            return false;
        }

        if (!this.tokensRegisteredBySession.get(sessionId).containsKey(token)) {
            // Token is not registered in the existing session
            return false;
        }

        if (!this.participantsByTokens.containsKey(connectionId)) {
            // Participant is not registered for specific token
            return false;
        }

        // In this final state, if connectionId is equal to participant public id, the token is registered
        return participantsByTokens.get(connectionId).getParticipantPublicId().equals(connectionId);

    }

    /**
     * Get registered token from token string
     * @param tokenKey string key which represents the token
     * @return
     */
    public Token getRegisteredToken(String tokenKey) {
        Token token = this.tokensRegistered.get(tokenKey);
        if (token != null) {
            return token;
        }
        return null;
    }


}