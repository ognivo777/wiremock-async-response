package org.obiz.wiremock.async.response.interceptors;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.obiz.wiremock.async.response.AsyncResponseDefinition;

public interface AsyncResponseTransformer {

    AsyncResponseDefinition transform(ServeEvent serveEvent, AsyncResponseDefinition webhookDefinition);

}
