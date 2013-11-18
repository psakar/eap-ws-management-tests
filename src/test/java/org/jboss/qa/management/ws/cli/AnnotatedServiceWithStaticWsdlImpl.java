/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

import javax.jws.WebService;

import org.apache.cxf.annotations.EndpointProperties;

@WebService(
   portName = "AnnotatedSecurityServicePort",
   serviceName = "AnnotatedSecurityService",
   name = "AnnotatedServiceWithStaticWsdlIface",
   endpointInterface = "org.jboss.qa.management.ws.cli.AnnotatedServiceWithStaticWsdlIface",
   targetNamespace = Constants.NAMESPACE
)
@EndpointProperties(value={})
public class AnnotatedServiceWithStaticWsdlImpl implements AnnotatedServiceWithStaticWsdlIface
{
   static final String HELLO_WORLD = "Hello World!";

   @Override
   public String sayHello()
   {
      return HELLO_WORLD;
   }
}
