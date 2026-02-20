/*
 *  Copyright 2026 Curity AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.curity.identityserver.plugin.idura.authentication;

import io.curity.identityserver.plugin.idura.config.IduraAuthenticatorPluginConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.curity.identityserver.sdk.attribute.Attribute;
import se.curity.identityserver.sdk.authentication.AuthenticationResult;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.http.HttpStatus;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.UserPreferenceManager;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;
import se.curity.identityserver.sdk.web.Request;
import se.curity.identityserver.sdk.web.Response;
import se.curity.identityserver.sdk.web.alerts.ErrorMessage;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static io.curity.identityserver.plugin.idura.authentication.RedirectUriUtil.createRedirectUri;
import static io.curity.identityserver.plugin.idura.config.IduraAuthenticatorPluginConfig.Country.Norway.LoginUsing.MOBILE_DEVICE;
import static io.curity.identityserver.plugin.idura.config.IduraAuthenticatorPluginConfig.Country.Sweden.LoginUsing.OTHER_DEVICE;
import static io.curity.identityserver.plugin.idura.config.IduraAuthenticatorPluginConfig.Country.Sweden.LoginUsing.SAME_DEVICE;
import static io.curity.identityserver.plugin.idura.descriptor.IduraAuthenticatorPluginDescriptor.CANCEL;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static se.curity.identityserver.sdk.web.ResponseModel.templateResponseModel;

public class IduraAuthenticatorRequestHandler implements AuthenticatorRequestHandler<RequestModel>
{
    private static final Logger _logger = LoggerFactory.getLogger(IduraAuthenticatorRequestHandler.class);
    private final String _authorizationEndpoint;

    private final IduraAuthenticatorPluginConfig _config;
    private final AuthenticatorInformationProvider _authenticatorInformationProvider;
    private final ExceptionFactory _exceptionFactory;
    private final UserPreferenceManager _userPreferenceManager;

    public IduraAuthenticatorRequestHandler(IduraAuthenticatorPluginConfig config)
    {
        _config = config;
        _exceptionFactory = config.getExceptionFactory();
        _authenticatorInformationProvider = config.getAuthenticatorInformationProvider();
        _authorizationEndpoint = "https://" + _config.getDomain() + "/oauth2/authorize";
        _userPreferenceManager = config.getUserPreferenceManager();
    }

    @Override
    public Optional<AuthenticationResult> get(RequestModel requestModel, Response response)
    {
        _logger.debug("GET request received for authentication");

        boolean[] isRedirect = {true};

        _config.getCountry().getSweden().ifPresent(item ->
        {
            if (item.getLogInUsing() == OTHER_DEVICE)
            {
                isRedirect[0] = false;
            }
        });

        _config.getCountry().getNorway().ifPresent(item ->
        {
            if (item.getLogInUsing() == MOBILE_DEVICE)
            {
                isRedirect[0] = true;
            }
        });

        if (isRedirect[0])
        {
            redirectToAuthorization(requestModel, response);
        }

        return Optional.empty();
    }

    private String buildUrl(String endpoint, Map<String, Collection<String>> queryStringArguments)
    {
        final Set<String> query = new HashSet<>(queryStringArguments.size());

        queryStringArguments.forEach((key, item) ->
        {
            query.add(key + "=" + String.join("+", item));
        });

        return endpoint + "?" + String.join("&", query);
    }

    private void redirectToAuthorization(RequestModel requestModel, Response response)
    {
        String redirectUri = createRedirectUri(_authenticatorInformationProvider, _exceptionFactory);
        String state = UUID.randomUUID().toString();
        Map<String, Collection<String>> queryStringArguments = new LinkedHashMap<>(6);
        Set<String> scopes = new LinkedHashSet<>(2);
        Set<String> acrValues = new LinkedHashSet<>(1);
        Map<String, Object> viewData = new HashMap<>(4);

        scopes.add("openid");

        // Set the iframe settings for Denmark up front; they'll be overridden below if some other country was selected
        viewData.put("iframeHeight", 464);
        viewData.put("iframeWidth", 320);
        viewData.put("country", "danish");

        _config.getCountry().getSweden().ifPresent(item ->
        {
            if (item.getLogInUsing() == SAME_DEVICE) {
                // The app will be started automatically, so the iframe can stay hidden
                viewData.put("iframeHeight", 0);
                viewData.put("iframeWidth", 0);
            }

            viewData.put("country", "swedish");

            if (item.getLogInUsing() == OTHER_DEVICE)
            {
                scopes.add("sub:" + requestModel.getPersonalNumber());
            }
        });

        _config.getCountry().getNorway().ifPresent(item ->
        {
            viewData.put("country", "norwegian");
            if (item.getLogInUsing() == MOBILE_DEVICE)
            {
                viewData.put("iframeHeight", 240);
                viewData.put("iframeWidth", 388);
            }
            else
            {
                viewData.put("iframeHeight", 300);
                viewData.put("iframeWidth", 500);
            }
        });

        setAcrValues(acrValues);

        _config.getSessionManager().put(Attribute.of("state", state));

        queryStringArguments.put("client_id", Collections.singleton(_config.getClientId()));
        queryStringArguments.put("redirect_uri", Collections.singleton(redirectUri));
        queryStringArguments.put("state", Collections.singleton(state));
        queryStringArguments.put("response_type", Collections.singleton("code"));

        queryStringArguments.put("scope", Collections.singleton(String.join("+", scopes)));
        queryStringArguments.put("acr_values", Collections.singleton(String.join(" ", acrValues)));

        _logger.debug("Redirecting to {} with query string arguments {}", _authorizationEndpoint,
                queryStringArguments);

        String authorizeUrl = buildUrl(_authorizationEndpoint, queryStringArguments);

        viewData.put("authorizeUrl", authorizeUrl);
        viewData.put("domain", _config.getDomain());
        viewData.put("cancelAction", _authenticatorInformationProvider.getFullyQualifiedAuthenticationUri() + "/" + CANCEL);

        response.setResponseModel(templateResponseModel(viewData, "authenticate/authorize"),
                Response.ResponseModelScope.NOT_FAILURE);
    }

    private void setAcrValues(Set<String> acrValues)
    {
        _config.getCountry().getSweden().ifPresent(swedishOption ->
        {
            switch (swedishOption.getLogInUsing())
            {
                case SAME_DEVICE:
                {
                    String acr = "urn:grn:authn:se:bankid:same-device";

                    _logger.debug("Adding ACR ({}) that will cause Idura to perform Swedish BankID login on the " +
                            "same device", acr);

                    acrValues.add(acr);

                    break;
                }
                case OTHER_DEVICE:
                {
                    String acr = "urn:grn:authn:se:bankid:another-device";

                    _logger.debug("Adding ACR ({}) that will cause Idura to perform Swedish BankID login on a " +
                            "different device", acr);

                    acrValues.add(acr);

                    break;
                }
            }
        });

        _config.getCountry().getNorway().ifPresent(norwegianOption ->
        {
            switch (norwegianOption.getLogInUsing())
            {
                case MOBILE_DEVICE:
                {
                    String acr = "urn:grn:authn:no:bankid:mobile";

                    _logger.debug("Adding ACR ({}) that will cause Idura to perform Norwegian BankID login on a " +
                            "mobile device", acr);

                    acrValues.add(acr);

                    break;
                }
                case HARDWARE_TOKEN:
                {
                    String acr = "urn:grn:authn:no:bankid:central";

                    _logger.debug("Adding ACR ({}) that will cause Idura to perform Norwegian BankID login using " +
                            "a hardware device", acr);

                    acrValues.add(acr);

                    break;
                }
            }
        });

        _config.getCountry().getDenmark().ifPresent(danishOption ->
        {
            switch (danishOption.getUserType())
            {
                case PRIVATE_CITIZENS:
                {
                    String acr = "urn:grn:authn:dk:nemid:poces";

                    _logger.debug("Adding ACR ({}) that will cause Idura to perform Danish NemID login for " +
                            "private citizens (i.e., regular banking users)", acr);

                    acrValues.add(acr);

                    break;
                }
                case EMPLOYEES:
                {
                    String acr = "urn:grn:authn:dk:nemid:moces";

                    _logger.debug("Adding ACR ({}) that will cause Idura to perform Danish NemID login for " +
                            "banking employees", acr);

                    acrValues.add(acr);

                    break;
                }
                case EMPLOYEES_WITH_APP:
                {
                    String acr = "urn:grn:authn:dk:nemid:moces:codefile";

                    _logger.debug("Adding ACR ({}) that will cause Idura to perform Danish NemID login for banking " +
                            "employees using an installed application", acr);

                    acrValues.add(acr);

                    break;
                }
            }
        });
    }

    @Override
    public Optional<AuthenticationResult> post(RequestModel request, Response response)
    {
        if (request.getPersonalNumber() != null)
        {
            _userPreferenceManager.saveUsername(request.getPersonalNumber());

            redirectToAuthorization(request, response);
        }

        return Optional.empty();
    }

    @Override
    public RequestModel preProcess(Request request, Response response)
    {
        _config.getCountry().getSweden().ifPresent(item ->
        {
            if (item.getLogInUsing() == OTHER_DEVICE)
            {
                if (request.isGetRequest())
                {
                    response.setResponseModel(templateResponseModel(
                            singletonMap("personalNumber", _userPreferenceManager.getUsername()),
                            "authenticate/get"),
                            Response.ResponseModelScope.NOT_FAILURE);
                }
            }
        });

        // on request validation failure, we should use the same template as for NOT_FAILURE
        response.setResponseModel(templateResponseModel(emptyMap(),
                "authenticate/get"), HttpStatus.BAD_REQUEST);

        return new RequestModel(request);
    }

    @Override
    public void onRequestModelValidationFailure(Request request, Response response, Set<ErrorMessage> errorMessages)
    {
        response.addErrorMessages(errorMessages);
    }
}
