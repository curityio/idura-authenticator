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

package io.curity.identityserver.plugin.idura.descriptor;

import io.curity.identityserver.plugin.idura.authentication.CallbackRequestHandler;
import io.curity.identityserver.plugin.idura.authentication.CancelRequestHandler;
import io.curity.identityserver.plugin.idura.authentication.IduraAuthenticatorRequestHandler;
import io.curity.identityserver.plugin.idura.config.IduraAuthenticatorPluginConfig;
import se.curity.identityserver.sdk.authentication.AuthenticatorRequestHandler;
import se.curity.identityserver.sdk.plugin.descriptor.AuthenticatorPluginDescriptor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class IduraAuthenticatorPluginDescriptor
        implements AuthenticatorPluginDescriptor<IduraAuthenticatorPluginConfig>
{
    public final static String CALLBACK = "callback";
    public final static String CANCEL = "cancel";

    @Override
    public String getPluginImplementationType()
    {
        return "idura";
    }

    @Override
    public Class<? extends IduraAuthenticatorPluginConfig> getConfigurationType()
    {
        return IduraAuthenticatorPluginConfig.class;
    }

    @Override
    public Map<String, Class<? extends AuthenticatorRequestHandler<?>>> getAuthenticationRequestHandlerTypes()
    {
        Map<String, Class<? extends AuthenticatorRequestHandler<?>>> handlers = new LinkedHashMap<>(2);

        handlers.put("index", IduraAuthenticatorRequestHandler.class);
        handlers.put(CANCEL, CancelRequestHandler.class);
        handlers.put(CALLBACK, CallbackRequestHandler.class);

        return Collections.unmodifiableMap(handlers);
    }
}
