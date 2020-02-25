/**
 * File: FrevoXmpp.java
 * 
 * Copyright (C) 2020 FREVO XMPP project contributors
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

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Main class for FREVO XMPP module for the CPSwarm project.
 */
public class FrevoXmpp {

  protected final static String DOMAIN_OPTION = "d";
  protected final static String HOST_OPTION = "h";
  protected final static String PORT_OPTION = "po";
  protected final static String RESOURCE_OPTION = "r";
  protected final static String USERNAME_OPTION = "u";
  protected final static String PASSWORD_OPTION = "p";

  /**
   * Entry point for application.
   * 
   * @param args command line arguments
   */
  public static void main(String[] args) {

    System.out.println("FREVO XMPP");
    System.out.println("Copyright 2019 CPSwarm Project");

    Options options = new Options();
    options.addRequiredOption(DOMAIN_OPTION, "domain", true, "XMPP domain");
    options.addRequiredOption(HOST_OPTION, "host", true, "XMPP server host");
    options.addOption(PORT_OPTION, "port", true, "XMPP server port");
    options.addRequiredOption(RESOURCE_OPTION, "resource", true, "XMPP resource");
    options.addRequiredOption(USERNAME_OPTION, "username", true, "username");
    options.addRequiredOption(PASSWORD_OPTION, "password", true, "password");

    try {
      var commandLine = new DefaultParser().parse(options, args);
      try {

        // build the connection configuration
        var connectionConfigurationBuilder = XMPPTCPConnectionConfiguration.builder()
            .setHost(commandLine.getOptionValue(HOST_OPTION))
            .setXmppDomain(commandLine.getOptionValue(DOMAIN_OPTION))
            .setUsernameAndPassword(commandLine.getOptionValue(USERNAME_OPTION),
                commandLine.getOptionValue(PASSWORD_OPTION))
            .setResource(commandLine.getOptionValue(RESOURCE_OPTION));

        if (commandLine.hasOption(PORT_OPTION)) {
          connectionConfigurationBuilder
              .setPort(Integer.parseInt(commandLine.getOptionValue(PORT_OPTION)));
        }

        new FrevoWrapper(connectionConfigurationBuilder.build());
      } catch (XmppStringprepException e) {
        e.printStackTrace();
      }
    } catch (ParseException e) {
      System.out.println("Error parsing arguments: " + e.getMessage());
      new HelpFormatter().printHelp("frevo-xmpp", options);
    }
  }
}
