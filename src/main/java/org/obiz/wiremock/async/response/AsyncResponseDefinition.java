package org.obiz.wiremock.async.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.tomakehurst.wiremock.http.Body;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.HttpHeaders;
import com.github.tomakehurst.wiremock.http.RequestMethod;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class AsyncResponseDefinition {
    private RequestMethod method;
    private URI url;
    private List<HttpHeader> headers;
    private Body body = Body.none();

    @JsonCreator
    public AsyncResponseDefinition(@JsonProperty("method") RequestMethod method,
                             @JsonProperty("url") URI url,
                             @JsonProperty("headers") HttpHeaders headers,
                             @JsonProperty("body") String body,
                             @JsonProperty("base64Body") String base64Body,
                             @JsonProperty("transformers") List<String> transformers) {
        this.method = method;
        this.url = url;
        this.headers = new ArrayList(headers.all());
        this.body = Body.fromOneOf(null, body, null, base64Body);
    }

    public AsyncResponseDefinition() {
    }

    public RequestMethod getMethod() {
        return method;
    }

    public URI getUrl() {
        return url;
    }

    public HttpHeaders getHeaders() {
        return new HttpHeaders(headers);
    }

    public String getBase64Body() {
        return body.isBinary() ? body.asBase64() : null;
    }

    public String getBody() {
        return body.isBinary() ? null : body.asString();
    }

    @JsonIgnore
    public byte[] getBinaryBody() {
        return body.asBytes();
    }

    public AsyncResponseDefinition withMethod(RequestMethod method) {
        this.method = method;
        return this;
    }

    public AsyncResponseDefinition withUrl(URI url) {
        this.url = url;
        return this;
    }

    public AsyncResponseDefinition withUrl(String url) {
        withUrl(URI.create(url));
        return this;
    }

    public AsyncResponseDefinition withHeaders(List<HttpHeader> headers) {
        this.headers = headers;
        return this;
    }

    public AsyncResponseDefinition withHeader(String key, String... values) {
        if (headers == null) {
            headers = new ArrayList();
        }

        headers.add(new HttpHeader(key, values));
        return this;
    }

    public AsyncResponseDefinition withBody(String body) {
        this.body = new Body(body);
        return this;
    }

    public AsyncResponseDefinition withBinaryBody(byte[] body) {
        this.body = new Body(body);
        return this;
    }
}
