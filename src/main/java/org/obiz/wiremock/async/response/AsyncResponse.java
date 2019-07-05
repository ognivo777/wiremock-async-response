package org.obiz.wiremock.async.response;

import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.common.Notifier;
import com.github.tomakehurst.wiremock.common.SingleRootFileSource;
import com.github.tomakehurst.wiremock.core.Admin;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.PostServeAction;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.http.HttpClientFactory;
import com.github.tomakehurst.wiremock.http.HttpHeader;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.obiz.wiremock.async.response.interceptors.AsyncResponseTransformer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static com.github.tomakehurst.wiremock.common.Exceptions.throwUnchecked;
import static com.github.tomakehurst.wiremock.common.LocalNotifier.notifier;
import static com.github.tomakehurst.wiremock.core.WireMockApp.FILES_ROOT;
import static com.github.tomakehurst.wiremock.http.HttpClientFactory.getHttpRequestFor;
import static java.util.concurrent.TimeUnit.SECONDS;

public class AsyncResponse extends PostServeAction {
    private ScheduledExecutorService scheduler;
    private HttpClient httpClient;
    private List<AsyncResponseTransformer> transformers;

    private AsyncResponse(
            ScheduledExecutorService scheduler,
            HttpClient httpClient,
            List<AsyncResponseTransformer> transformers) {
        this.scheduler = scheduler;
        this.httpClient = httpClient;
        this.transformers = transformers;
    }

    public AsyncResponse() {
        this(Executors.newScheduledThreadPool(10), HttpClientFactory.createClient(), new ArrayList<AsyncResponseTransformer>());
    }

//    public AsyncResponse(AsyncResponseTransformer... transformers) {
//        this(Executors.newScheduledThreadPool(10), HttpClientFactory.createClient(), Arrays.asList(transformers));
//    }

    @Override
    public String getName() {
        return "async-response";
    }

    @Override
    public void doAction(final ServeEvent serveEvent, final Admin admin, final Parameters parameters) {
        final Notifier notifier = notifier();

        scheduler.schedule(
                new Runnable() {
                    @Override
                    public void run() {

                        Parameters asyncParam = new Parameters();

                        //copy valid AsyncResponseDefinition params
                        for (String key : Arrays.asList("method", "url", "headers", "body", "base64Body", "transformers")) {
                            if (parameters.containsKey(key)) {
                                asyncParam.put(key, parameters.get(key));
                            }
                        }

                        AsyncResponseDefinition definition = asyncParam.as(AsyncResponseDefinition.class);

                        try {
                            Parameters p = new Parameters();
                            p = p.merge(parameters);
                            p.remove("url");
                            p.remove("method");
                            p.remove("delay");
                            ResponseDefinition responseDefinition = p.as(ResponseDefinition.class);
                            ResponseTemplateTransformer t = new ResponseTemplateTransformer(false);
                            FileSource source = new SingleRootFileSource(".").child(FILES_ROOT);
                            ResponseDefinition rd2 = t.transform(serveEvent.getRequest(), responseDefinition, source, parameters);
                            definition.withBody(rd2.getBody());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                        HttpUriRequest request = buildRequest(definition);

                        try {
                            HttpResponse response = httpClient.execute(request);
                            notifier.info(
                                    String.format("AsyncResponse %s request to %s returned status %s\n\n%s",
                                            definition.getMethod(),
                                            definition.getUrl(),
                                            response.getStatusLine(),
                                            EntityUtils.toString(response.getEntity())
                                    )
                            );
                        } catch (IOException e) {
                            throwUnchecked(e);
                        }
                    }
                },
                parameters.getInt("delay"),
                SECONDS
        );
    }

    private static HttpUriRequest buildRequest(AsyncResponseDefinition definition) {
        HttpUriRequest request = getHttpRequestFor(
                definition.getMethod(),
                definition.getUrl().toString()
        );

        for (HttpHeader header: definition.getHeaders().all()) {
            request.addHeader(header.key(), header.firstValue());
        }

        if (definition.getMethod().hasEntity()) {
            HttpEntityEnclosingRequestBase entityRequest = (HttpEntityEnclosingRequestBase) request;
            entityRequest.setEntity(new ByteArrayEntity(definition.getBinaryBody()));
        }

        return request;
    }

    public static AsyncResponseDefinition AsyncResponse() {
        return new AsyncResponseDefinition();
    }
}
