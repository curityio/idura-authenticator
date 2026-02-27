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

package io.curity.identityserver.plugin.idura.config;

import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.config.OneOf;
import se.curity.identityserver.sdk.config.annotation.DefaultEnum;
import se.curity.identityserver.sdk.config.annotation.Description;
import se.curity.identityserver.sdk.service.ExceptionFactory;
import se.curity.identityserver.sdk.service.HttpClient;
import se.curity.identityserver.sdk.service.Json;
import se.curity.identityserver.sdk.service.SessionManager;
import se.curity.identityserver.sdk.service.UserPreferenceManager;
import se.curity.identityserver.sdk.service.WebServiceClientFactory;
import se.curity.identityserver.sdk.service.authentication.AuthenticatorInformationProvider;

import java.util.Optional;

@SuppressWarnings("InterfaceNeverImplemented")
public interface IduraAuthenticatorPluginConfig extends Configuration
{

    @Description("The Client ID/realm of the Curity Identity Server application configured in Idura Verify")
    String getClientId();

    @Description("The associated secret for the client configured in Idura")
    String getClientSecret();

    @Description("The HTTP client with any proxy and TLS settings that will be used to connect to Idura")
    Optional<HttpClient> getHttpClient();

    @Description("The domain or tenant ID at Idura")
    String getDomain();

    Country getCountry();

    interface Country extends OneOf
    {
        @Description("Login using Swedish BankID")
        Optional<Sweden> getSweden();

        @Description("Login using Norwegian BankID")
        Optional<Norway> getNorway();

        @Description("Login using NemID")
        Optional<Denmark> getDenmark();

        interface Sweden
        {
            @Description("How the user should log in -- either on the same device or another device")
            @DefaultEnum("SAME_DEVICE")
            LoginUsing getLogInUsing();

            enum LoginUsing
            {
                @Description("Login on the same device (sends 'urn:grn:authn:se:bankid:same-device' ACR value to Idura")
                SAME_DEVICE,

                @Description("Login on some other device (sends 'urn:grn:authn:se:bankid:another-device' ACR value Idura")
                OTHER_DEVICE
            }
        }

        interface Norway
        {
            @Description("How the user should log in -- either on a mobile device or a hardware token")
            @DefaultEnum("MOBILE_DEVICE")
            LoginUsing getLogInUsing();

            enum LoginUsing
            {
                @Description("Login on a mobile device (sends 'urn:grn:authn:no:bankid:mobile' ACR value to Idura)")
                MOBILE_DEVICE,

                @Description("Login using a hardware token (sends 'urn:grn:authn:no:bankid:central' ACR value to Idura")
                HARDWARE_TOKEN
            }
        }

        interface Denmark
        {
            @Description("How the user should log in -- either as a regular banking customer or an employee of a " +
                    "bank or a banking employee using an installed application")
            @DefaultEnum("PRIVATE_CITIZENS")
            UserType getUserType();

            enum UserType
            {
                @Description("Login private citizens, normal banking customers (sends 'urn:grn:authn:dk:nemid:poces' " +
                        "ACR value to Idura")
                PRIVATE_CITIZENS,

                @Description("Login banking employees (sends 'urn:grn:authn:dk:nemid:moces' ACR value to Idura)")
                EMPLOYEES,

                @Description("Login banking employees using an installed application (sends " +
                        "'urn:grn:authn:dk:nemid:moces:codefile' ACR value to Idura)")
                EMPLOYEES_WITH_APP
            }
        }
    }

    UserPreferenceManager getUserPreferenceManager();

    SessionManager getSessionManager();

    ExceptionFactory getExceptionFactory();

    AuthenticatorInformationProvider getAuthenticatorInformationProvider();

    WebServiceClientFactory getWebServiceClientFactory();

    Json getJson();
}
