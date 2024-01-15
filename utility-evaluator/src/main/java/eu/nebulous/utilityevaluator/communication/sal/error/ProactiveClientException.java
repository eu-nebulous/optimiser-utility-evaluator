package eu.nebulous.utilityevaluator.communication.sal.error;

import reactor.netty.http.client.HttpClientResponse;

public class ProactiveClientException extends RuntimeException {
    public ProactiveClientException(String message) {
        super(message);
    }

    public ProactiveClientException(HttpClientResponse clientResponse) {
        super(clientResponse.status().toString());
    }

    public ProactiveClientException(String message, Throwable cause) {
        super(message, cause);
    }
}

