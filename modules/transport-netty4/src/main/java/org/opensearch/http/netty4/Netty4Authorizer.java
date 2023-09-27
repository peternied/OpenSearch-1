/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.security.ssl.http.netty;

import io.netty.channel.ChannelHandler;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.core.xcontent.NamedXContentRegistry;
import org.opensearch.http.AbstractHttpServerTransport;

import io.netty.channel.ChannelHandlerContext;
import org.opensearch.http.netty4.Netty4DefaultHttpRequest;
import org.opensearch.http.netty4.Netty4HttpChannel;
import org.opensearch.http.netty4.Netty4HttpServerTransport;
import org.opensearch.rest.RestRequest;
import org.opensearch.security.filter.SecurityRestFilter;
import org.opensearch.security.http.InterceptingRestChannel;
import org.opensearch.threadpool.ThreadPool;

import static org.opensearch.http.netty4.Netty4HttpServerTransport.CONTEXT_TO_RESTORE;
import static org.opensearch.http.netty4.Netty4HttpServerTransport.EARLY_RESPONSE;
import static org.opensearch.http.netty4.Netty4HttpServerTransport.REQUEST_ID;

@ChannelHandler.Sharable
public class Netty4Authorizer extends SimpleChannelInboundHandler<DefaultHttpRequest> {
    private final ThreadPool threadPool;
    private final NamedXContentRegistry xContentRegistry;

    public Netty4HttpRequestHeaderVerifier(SecurityRestFilter restFilter, NamedXContentRegistry xContentRegistry, ThreadPool threadPool) {
        this.restFilter = restFilter;
        this.xContentRegistry = xContentRegistry;
        this.threadPool = threadPool;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, DefaultHttpRequest msg) throws Exception {
        // READ MORE: https://netty.io/wiki/user-guide-for-4.x.html#wiki-h3-7
        final boolean hasAuthorizationHeaders = msg.headers().entries().stream()
            .anyMatch(header -> header.getKey() == "Authorization");

        if (!hasAuthorizationHeaders) {
            ctx.writeAndFlush(null);
        }
        ctx.fireChannelRead(msg);
    }
}
