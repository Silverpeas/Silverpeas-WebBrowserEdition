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

import org.silverpeas.core.annotation.Service;
import org.silverpeas.core.wbe.WbeEdition;
import org.silverpeas.core.wbe.WbeFile;
import org.silverpeas.core.wbe.WbeUser;
import org.silverpeas.core.webapi.wbe.WbeFileEdition;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;
import java.util.Optional;

import static java.text.MessageFormat.format;
import static java.util.Optional.of;
import static javax.ws.rs.core.UriBuilder.fromUri;
import static org.silverpeas.core.wbe.WbeLogger.logger;
import static org.silverpeas.core.webapi.wbe.WbeFileEdition.ACCESS_TOKEN_PARAM;
import static org.silverpeas.wbe.wopi.util.WopiSettings.getWopiHostServiceBaseUrl;

/**
 * @author silveryocha
 */
@Service
public class WopiRequestDispatcher implements WbeFileEdition.ClientRequestDispatcher {

  private static final String WOPI_SRC_PARAM = "WOPISrc";

  @Override
  public boolean canHandle(final WbeEdition edition) {
    return edition instanceof WopiEdition;
  }

  @Override
  public Optional<String> dispatch(final HttpServletRequest request, final WbeEdition edition) {
    final WopiEdition wopiEdition = (WopiEdition) edition;
    final WbeUser editionUser = wopiEdition.getUser();
    final WbeFile editionFile = wopiEdition.getFile();
    final String fileId = editionFile.id();
    final UriBuilder hostUriBuilder = fromUri(getWopiHostServiceBaseUrl()).path(fileId);
    final UriBuilder uriBuilder = fromUri(wopiEdition.getClientBaseUrl())
        .queryParam(WOPI_SRC_PARAM, hostUriBuilder.build())
        .queryParam(ACCESS_TOKEN_PARAM, editionUser.getAccessToken());
    final String clientUrl = uriBuilder.build().toString();
    logger().debug(() -> format(
        "from {0} initializing WOPI edition for {1} and for user {2} with WOPI client URL {3}",
        editionUser.getSilverpeasSessionId(), editionFile, editionUser, clientUrl));
    return of(clientUrl).map(u -> {
      request.setAttribute("WopiClientUrl", u);
      request.setAttribute("WopiUser", editionUser);
      request.setAttribute("WopiFile", editionFile);
      return "/wbe/wopi/editor.jsp";
    });
  }
}
