/**
 * File: FrevoXmppConfiguration.java
 * 
 * Copyright (C) 2019 FREVO XMPP project contributors
 *
 * Universitaet Klagenfurt licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package at.aau.frevo.xmpp;

/**
 * Represents the configuration of {@link FrevoXmpp}.
 */
public class FrevoXmppConfiguration {

  protected String host;
  protected int port;
  protected String domain;
  protected String resource;
  protected String clientId;
  protected String clientPassword;
  protected boolean debug;
  protected String baseCandidateFilename;

  /**
   * Creates a new {@code FrevoXmppConfiguration} instance with the specified parameters.
   * 
   * @param host                  the host
   * @param port                  the port
   * @param domain                the domain
   * @param resource              the resource
   * @param clientId              the client user id
   * @param clientPassword        the client password
   * @param debug                 flag to enable debugging
   * @param baseCandidateFilename base file name to use for storing candidates after generations
   */
  public FrevoXmppConfiguration(String host, int port, String domain, String resource,
      String clientId, String clientPassword, boolean debug, String baseCandidateFilename) {
    this.host = host;
    this.port = port;
    this.domain = domain;
    this.resource = resource;
    this.clientId = clientId;
    this.clientPassword = clientPassword;
    this.debug = debug;
    this.baseCandidateFilename = baseCandidateFilename;
  }

  /**
   * Gets the host of the XMPP server.
   * 
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   * Gets the port of the XMPP server.
   * 
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * Gets the XMPP domain.
   * 
   * @return the domain
   */
  public String getDomain() {
    return domain;
  }

  /**
   * Gets the XMPP resource.
   * 
   * @return the resource
   */
  public String getResource() {
    return resource;
  }

  /**
   * Gets the client id.
   * 
   * @return the client id
   */
  public String getClientId() {
    return clientId;
  }

  /**
   * Gets the client password.
   * 
   * @return the client password
   */
  public String getClientPassword() {
    return clientPassword;
  }

  /**
   * Gets the debug flag.
   * 
   * @return the debug flag
   */
  public boolean isDebugging() {
    return debug;
  }

  /**
   * Gets the base candidate file name.
   * 
   * @return the base candidate file name
   */
  public String getBaseCandidateFilename() {
    return baseCandidateFilename;
  }

  @Override
  public String toString() {
    var stringBuilder = new StringBuilder();
    stringBuilder.append("host: ");
    stringBuilder.append(host);
    stringBuilder.append(", port: ");
    stringBuilder.append(port);
    stringBuilder.append(", domain: ");
    stringBuilder.append(domain);
    stringBuilder.append(", resource: ");
    stringBuilder.append(resource);
    stringBuilder.append(", clientId: ");
    stringBuilder.append(clientId);
    stringBuilder.append(", clientPassword: ");
    stringBuilder.append(clientPassword);
    stringBuilder.append(", debug: ");
    stringBuilder.append(debug);
    stringBuilder.append(", baseCandidateFilename: ");
    stringBuilder.append(baseCandidateFilename);
    return stringBuilder.toString();
  }
}
