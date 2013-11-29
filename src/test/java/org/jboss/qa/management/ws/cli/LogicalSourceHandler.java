/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.qa.management.ws.cli;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.handler.LogicalMessageContext;
import javax.xml.ws.handler.MessageContext;

import org.jboss.logging.Logger;
import org.jboss.ws.api.handler.GenericLogicalHandler;
import org.jboss.ws.api.util.DOMUtils;
import org.w3c.dom.Element;

public class LogicalSourceHandler extends GenericLogicalHandler<LogicalMessageContext> {
  static final String INBOUND_APPENDIX = ":Inbound:LogicalSourceHandler";
  static final String OUTBOUND_APPENDIX = ":Outbound:LogicalSourceHandler";
  // provide logging
  private static final Logger log = Logger.getLogger(LogicalSourceHandler.class);

  @Override
  public boolean handleOutbound(MessageContext msgContext) {
    return appendHandlerName(msgContext, OUTBOUND_APPENDIX);
  }

  @Override
  public boolean handleInbound(MessageContext msgContext) {
    return appendHandlerName(msgContext, INBOUND_APPENDIX);
  }

  private boolean appendHandlerName(MessageContext msgContext, String appendValue) {
    try {
      // Get the payload as Source
      LogicalMessageContext logicalContext = (LogicalMessageContext) msgContext;
      Source messagePayload = logicalContext.getMessage().getPayload();
      log.info("payload: " + sourceToString(messagePayload));

      Element root = getRootElement(messagePayload);

      log.info("root: " + root.toString());

      Element firstChild = DOMUtils.getFirstChildElement(root);

      if (firstChild != null) {
        updateFirstChild(firstChild, appendValue);

        updateMessagePayload(logicalContext, root);

      }

      return true;
    } catch (RuntimeException rte) {
      throw rte;
    } catch (Exception ex) {
      throw new WebServiceException(ex);
    }
  }

  private void updateMessagePayload(LogicalMessageContext logicalContext, Element root) {
    log.info("root: " + root.toString());
    logicalContext.getMessage().setPayload(new DOMSource(root));
  }

  private void updateFirstChild(Element firstChild, String appendValue) {
    String oldValue = DOMUtils.getTextContent(firstChild);
    String newValue = oldValue + appendValue;
    log.info("oldValue: " + oldValue);
    log.info("newValue: " + newValue);
    firstChild.setTextContent(newValue);
  }

  private Element getRootElement(Source messagePayload) throws TransformerFactoryConfigurationError,
      TransformerException, TransformerConfigurationException, IOException {
    TransformerFactory tf = TransformerFactory.newInstance();
    ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
    tf.newTransformer().transform(messagePayload, new StreamResult(baos));

    return DOMUtils.parse(new ByteArrayInputStream(baos.toByteArray()), getDocumentBuilder());
  }

  private String sourceToString(Source source) throws Exception {
    StringWriter stringWriter = new StringWriter();
    Result result = new StreamResult(stringWriter);
    TransformerFactory factory = TransformerFactory.newInstance();
    Transformer transformer = factory.newTransformer();
    transformer.transform(source, result);
    return stringWriter.getBuffer().toString();
  }

  private DocumentBuilder getDocumentBuilder() {
    DocumentBuilderFactory factory = null;
    try {
      factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);
      factory.setExpandEntityReferences(false);
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder;
    } catch (Exception e) {
      throw new RuntimeException("Unable to create document builder", e);
    }
  }
}
