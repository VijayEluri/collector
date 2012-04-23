/*
 * Copyright 2010-2011 Ning, Inc.
 *
 * Ning licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.ning.metrics.collector.endpoint.extractors;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.metrics.collector.endpoint.ParsedRequest;
import com.ning.metrics.collector.events.parsing.ThriftEnvelopeEventParser;
import com.ning.metrics.serialization.event.EventDeserializer;
import com.ning.metrics.serialization.event.ThriftEnvelopeEvent;

import java.io.IOException;

public class QueryParameterEventDeserializer implements EventDeserializer
{
    private static final Logger log = LoggerFactory.getLogger(QueryParameterEventDeserializer.class);

    private boolean hasNextEvent;
    private final ParsedRequest parsedRequest;
    private final ThriftEnvelopeEventParser thriftEventParser;

    @Inject
    public QueryParameterEventDeserializer(final ThriftEnvelopeEventParser thriftEventParser, final ParsedRequest parsedRequest)
    {
        this.hasNextEvent = true;
        this.parsedRequest = parsedRequest;
        this.thriftEventParser = thriftEventParser;
    }

    @Override
    public boolean hasNextEvent()
    {
        return hasNextEvent;
    }

    @Override
    public ThriftEnvelopeEvent getNextEvent() throws IOException
    {
        if (!hasNextEvent) {
            throw new IOException("No more events left to deserialize");
        }
        // can only extract one event
        hasNextEvent = false;

        final String eventName = parsedRequest.getEventName();

        if (eventName != null) {
            log.debug(String.format("Query parameter to process: %s", eventName));
            final String type = eventName.substring(0, eventName.indexOf(","));
            final String eventTypeString = eventName.substring(eventName.indexOf(",") + 1);

            log.debug(String.format("Event type [%s], event string [%s]", type, eventTypeString));

            try {
                // This API only supports sending one event at a time
                return thriftEventParser.parseThriftEvent(type, eventTypeString, parsedRequest);
            }
            catch (EventParsingException e) {
                throw new IOException("Unable to parse event from query string.", e);
            }
        }
        else {
            // TODO there's gotta be a better Exception to throw here. Maybe move EventParsingException
            // to the Serialization library so that we can throw it from EventDeserializers
            throw new IOException("Event name not specified");
        }
    }
}
