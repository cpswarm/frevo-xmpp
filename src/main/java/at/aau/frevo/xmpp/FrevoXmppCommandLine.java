/**
 * File: FrevoXmppCommandLine.java
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

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Parses and represents the command line for the {@link FrevoXmpp}.
 */
public class FrevoXmppCommandLine {

  protected final static String DEBUG_OPTION = "dbg";
  protected final static String BASE_CANDIDATE_FILE_OPTION = "c";
  protected final static String DOMAIN_OPTION = "d";
  protected final static String HOST_OPTION = "h";
  protected final static String PORT_OPTION = "p";
  protected final static String RESOURCE_OPTION = "r";
  protected final static String CLIENT_ID_OPTION = "cid";
  protected final static String CLIENT_PASSWORD_OPTION = "cp";

  protected final static Options options = new Options();

  /**
   * Initializes the static fields of {@code FrevoXmppCommandLine}.
   */
  static {
    options.addOption(DEBUG_OPTION, "debug", true, "set XMPP debug false");
    options.addOption(BASE_CANDIDATE_FILE_OPTION, "candidateFile", true,
        "base file to use for storing top candidates");
    options.addRequiredOption(DOMAIN_OPTION, "domain", true, "XMPP domain");
    options.addRequiredOption(HOST_OPTION, "host", true, "XMPP server host");
    options.addRequiredOption(PORT_OPTION, "serverPort", true, "XMPP server port");
    options.addRequiredOption(RESOURCE_OPTION, "resource", true, "XMPP resource");
    options.addRequiredOption(CLIENT_ID_OPTION, "clientId", true, "client id");
    options.addRequiredOption(CLIENT_PASSWORD_OPTION, "clientPassword", true, "client password");
  }

  /**
   * Creates an {@code FrevoXmppConfiguration} instance by parsing the string arguments provided.
   * 
   * @param args the arguments
   * @return the resulting {@code FrevoXmppConfiguration} instance
   * @throws ParseException if the arguments cannot be parsed
   */
  public static FrevoXmppConfiguration buildConfiguration(String[] args) throws ParseException {
    var commandLine = new DefaultParser().parse(options, args);

    return new FrevoXmppConfiguration(commandLine.getOptionValue(HOST_OPTION),
        Integer.parseInt(commandLine.getOptionValue(PORT_OPTION)),
        commandLine.getOptionValue(DOMAIN_OPTION), commandLine.getOptionValue(RESOURCE_OPTION),
        commandLine.getOptionValue(CLIENT_ID_OPTION),
        commandLine.getOptionValue(CLIENT_PASSWORD_OPTION),
        Boolean.parseBoolean(commandLine.getOptionValue(DEBUG_OPTION)),
        commandLine.getOptionValue(BASE_CANDIDATE_FILE_OPTION));
  }

  /**
   * Prints help text for command line usage
   */
  public static void printHelp() {
    new HelpFormatter().printHelp("frevo-xmpp", options);
  }
}
