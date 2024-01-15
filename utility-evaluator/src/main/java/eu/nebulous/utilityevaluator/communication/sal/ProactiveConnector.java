package eu.nebulous.utilityevaluator.communication.sal;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.nebulous.utilityevaluator.communication.sal.error.ProactiveClientException;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.logging.LogLevel;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.ow2.proactive.sal.model.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import reactor.netty.ByteBufMono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;


@Slf4j
@Service
public class ProactiveConnector {

    private static final String SESSION_HEADER = "sessionid";
    private static final int RETRIES_NUMBER = 20;
    private static final String PA_GATEWAY = "/pagateway";
    private static final String PA_GATEWAY_CONNECT = PA_GATEWAY + "/connect";
    private static final String NODECANDIDATES = "/nodecandidates";
    public static final String CLOUD = "/cloud";
    public static final String CLOUD_NODE_CANDIDATES_FETCH_CHECK = CLOUD + "/async";

    private final HttpClient httpClient;
    private String sessionId;

    private final ObjectMapper objectMapper;

    public ProactiveConnector(ProactiveClientProperties properties) {
        log.info("Properties: login: {}, pass: {}, url: {}", properties.getLogin(), properties.getPassword(), properties.getUrl());
        this.connect(properties.getLogin(), properties.getPassword(), properties.getUrl());
        //sessionId = "blablabla";
        this.httpClient = HttpClient.create()
                .baseUrl(properties.getUrl())
                .headers(headers -> headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .headers(headers -> headers.add(SESSION_HEADER, sessionId))
                .responseTimeout(Duration.of(80, ChronoUnit.SECONDS))
                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL, StandardCharsets.UTF_8);
        this.httpClient.warmup().block();
        this.objectMapper = new ObjectMapper();
        this.objectMapper
                .configOverride(List.class)
                .setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
                .setSetterInfo(JsonSetter.Value.forContentNulls(Nulls.AS_EMPTY));
    }

    public void connect(String login, String password, String schedulerUrl) {
        log.info("Connecting to SAL as a service");

        this.sessionId = HttpClient.create()
                .headers(headers -> headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED))
                .post()
                .uri(schedulerUrl + PA_GATEWAY_CONNECT)
                .sendForm((req, form) -> form
                        //.attr("username", login)
                        .attr("name", login)
                        .attr("password", password))
                .responseContent()
                .aggregate()
                .asString()
                .retry(RETRIES_NUMBER)
                .block();
        log.info("Connected with sessionId: {}...", sessionId.substring(0,10));
    }


    //nodeCandidates
    public List<NodeCandidate> fetchNodeCandidates(List<Requirement> requirements) {
        return httpClient.post()
                .uri(NODECANDIDATES)
                .send(bodyMonoPublisher(requirements))
                .responseSingle((resp, bytes) -> {
                    if (!resp.status().equals(HttpResponseStatus.OK)) {
                        return bytes.asString().flatMap(body -> Mono.error(new ProactiveClientException(body)));
                    } else {
                        return bytes.asString().mapNotNull(s -> {
                            try {
                                log.info("Received message: {}", s);
                                return objectMapper.readValue(s, NodeCandidate[].class);
                            } catch (IOException e) {
                                log.error(e.getMessage(), e);;
                                return null;
                            }
                        });
                    }
                })
                .doOnError(Throwable::printStackTrace)
                .blockOptional()
                .map(Arrays::asList)
                .orElseGet(Collections::emptyList);
    }

    private Mono<ByteBuf> bodyMonoPublisher(Object body) {
        if ((body instanceof JSONArray) || (body instanceof JSONObject)) {
            return ByteBufMono.fromString(Mono.just(body.toString()));
        }
        String json = null;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(), e);;
        }
        log.info("Sending body json: {}", json);
        return ByteBufMono.fromString(Mono.just(json));
    }


        public Boolean isAnyAsyncNodeCandidatesProcessesInProgress() {
        return httpClient.get()
                .uri(CLOUD_NODE_CANDIDATES_FETCH_CHECK)
                .responseSingle((resp, bytes) -> {
                    if (!resp.status().equals(HttpResponseStatus.OK)) {
                        return bytes.asString().flatMap(body -> Mono.error(new ProactiveClientException(body)));
                    } else {
                        return bytes.asString().map(Boolean::new);
                    }
                })
                .doOnError(Throwable::printStackTrace)
                .block();
    }
    
}
