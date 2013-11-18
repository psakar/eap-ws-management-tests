package org.jboss.qa.management.ws;

import javax.net.ssl.TrustManager;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.jboss.qa.management.common.WebUtils;

public class WebServiceUtils
{

   public static void setupWebServiceClienProxyToConnectWithoutValidatingCertificateAndServerName(Object proxy)
   {
      Client client = ClientProxy.getClient(proxy);
      HTTPConduit conduit = (HTTPConduit) client.getConduit();
      TLSClientParameters tcp = new TLSClientParameters();
      tcp.setDisableCNCheck(true);
      tcp.setTrustManagers(new TrustManager[] {WebUtils.createTrustManagerNotValidatingCertificateChains()});
      conduit.setTlsClientParameters(tcp);
   }

}
