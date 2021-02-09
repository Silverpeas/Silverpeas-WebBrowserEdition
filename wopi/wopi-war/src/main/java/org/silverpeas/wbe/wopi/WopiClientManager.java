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
import org.silverpeas.core.util.security.SecuritySettings;
import org.silverpeas.core.wbe.WbeClientManager;
import org.silverpeas.core.wbe.WbeFile;
import org.silverpeas.core.wbe.WbeHostManager;
import org.silverpeas.core.wbe.WbeUser;
import org.silverpeas.wbe.wopi.discovery.WopiDiscovery;
import org.silverpeas.wbe.wopi.util.WopiSettings;

import javax.ws.rs.WebApplicationException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.net.http.HttpResponse.BodyHandlers.ofInputStream;
import static java.text.MessageFormat.format;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Optional.*;
import static javax.ws.rs.core.Response.Status.OK;
import static org.silverpeas.core.util.HttpUtil.httpClientTrustingAnySslContext;
import static org.silverpeas.core.util.HttpUtil.toUrl;
import static org.silverpeas.core.util.StringUtil.isDefined;
import static org.silverpeas.core.wbe.WbeLogger.logger;
import static org.silverpeas.wbe.wopi.util.WopiSettings.*;

/**
 * @author silveryocha
 */
@Service
public class WopiClientManager implements WbeClientManager {

  private final Map<String, String> baseUrlByMimeTypes = new ConcurrentHashMap<>();
  private final Map<String, String> baseUrlByExtension = new ConcurrentHashMap<>();
  private LocalDateTime lastDateTimeOfDiscoveryGet;
  private String lastDiscoveryUrl;

  @Override
  public boolean isEnabled() {
    return WopiSettings.isEnabled();
  }

  @Override
  public boolean isHandled(final WbeFile file) {
    return getClientBaseUrlFor(file).isPresent();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Optional<WopiEdition> prepareEditionWith(final WbeUser user, final WbeFile file) {
    return getClientBaseUrlFor(file).map(u -> new WopiEdition(file, user, u));
  }

  @Override
  public Optional<String> getAdministrationUrl() {
    return ofNullable(getWopiClientAdministrationUrl());
  }

  @Override
  public void clear() {
    baseUrlByMimeTypes.clear();
    baseUrlByExtension.clear();
  }

  /**
   * WOPI discovery is the process by which a WOPI host identifies Office for the web
   * capabilities and how to initialize Office for the web applications within a site. WOPI hosts
   * use the discovery XML to determine how to interact with Office for the web.
   * <p>
   *   The discovery is processed every {@link WopiSettings#getWopiClientDiscoveryTimeToLive}
   *   hours to ensures the most up-to-date capabilities.
   * </p>
   */
  private synchronized void discover() {
    final String discoveryUrl = getWopiClientDiscoveryUrl();
    if (lastDiscoveryUrl == null ||
        !lastDiscoveryUrl.equals(discoveryUrl) ||
        baseUrlByMimeTypes.isEmpty() ||
        HOURS.between(lastDateTimeOfDiscoveryGet, LocalDateTime.now()) >= getWopiClientDiscoveryTimeToLive()) {
      WbeHostManager.get().clear();
      logger().debug(() -> format("discovering WOPI client with URL {0}", discoveryUrl));
      try {
        final HttpResponse<InputStream> response = httpClientTrustingAnySslContext().send(toUrl(discoveryUrl)
            .timeout(Duration.of(2, SECONDS))
            .build(), ofInputStream());
        if (response.statusCode() != OK.getStatusCode()) {
          throw new WebApplicationException(response.statusCode());
        }
        final WopiDiscovery wopiDiscovery;
        try (final InputStream body = response.body()) {
          wopiDiscovery = WopiDiscovery.load(body);
        }
        wopiDiscovery.consumeBaseUrlMimeType((n, a) -> {
          baseUrlByMimeTypes.put(n, a.getUrlsrc());
          if (isDefined(a.getExt()) && "edit".equals(a.getName())) {
            baseUrlByExtension.put(a.getExt(), a.getUrlsrc());
          }
        });
      } catch (Exception e) {
        logger().error(e);
        if (e instanceof InterruptedException) {
          Thread.currentThread().interrupt();
        }
        throw new WebApplicationException(e);
      }
      registerSecurityDomains();
      lastDiscoveryUrl = discoveryUrl;
      lastDateTimeOfDiscoveryGet = LocalDateTime.now();
    }
  }

  private Optional<String> getClientBaseUrlFor(final WbeFile file) {
    if (isEnabled()) {
      discover();
      final String clientBaseUrl = baseUrlByMimeTypes.get(file.mimeType());
      if (isDefined(clientBaseUrl)) {
        return of(clientBaseUrl);
      }
      return ofNullable(baseUrlByExtension.get(file.ext()));
    } else if (!baseUrlByMimeTypes.isEmpty()){
      WbeHostManager.get().clear();
      logger().debug(() -> format("removing all discovered actions because of WOPI disabling"));
    }
    return empty();
  }

  private void registerSecurityDomains() {
    getWopiClientBaseUrl().stream()
        .flatMap(u -> Stream.of(u, u.replaceFirst("^http", "ws")))
        .forEach(u -> {
          final SecuritySettings.Registration registration = SecuritySettings.registration();
          registration.registerDefaultSourceInCSP(u);
          registration.registerDomainInCORS(u);
          logger().debug(() -> format("registering into security WOPI client base URL {0}", u));
        });
  }

}
