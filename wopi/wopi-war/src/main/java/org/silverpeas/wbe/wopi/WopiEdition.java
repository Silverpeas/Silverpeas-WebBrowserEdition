/*
 * Copyright (C) 2000 - 2021 Silverpeas
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * As a special exception to the terms and conditions of version 3.0 of
 * the GPL, you may redistribute this Program in connection with Free/Libre
 * Open Source Software ("FLOSS") applications as described in Silverpeas's
 * FLOSS exception.  You should have received a copy of the text describing
 * the FLOSS exception, and it is also available here:
 * "https://www.silverpeas.org/legal/floss_exception.html"
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.silverpeas.wbe.wopi;

import org.silverpeas.core.wbe.WbeEdition;
import org.silverpeas.core.wbe.WbeFile;
import org.silverpeas.core.wbe.WbeUser;

/**
 * Represents the preparation of a Web Browser Edition for WOPI exchanges.
 * <p>
 *   This object provides in addition to existing elements:
 *   <ul>
 *     <li>a client base URL that permits to access the online editor that takes in charge de
 *     {@link WbeFile#mimeType()}</li>
 *   </ul>
 * </p>
 * @author silveryocha
 */
public class WopiEdition extends WbeEdition {

  private final String clientBaseUrl;

  protected WopiEdition(final WbeFile file, final WbeUser user, final String clientBaseUrl) {
    super(file, user);
    this.clientBaseUrl = clientBaseUrl;
  }

  /**
   * The client base URL is the base URL which permits to load the WBE editor which takes in
   * charge the {@link WbeFile} into the WEB browser.
   * @return an URL as string.
   */
  String getClientBaseUrl() {
    return clientBaseUrl;
  }
}
