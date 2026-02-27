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

import jakarta.validation.constraints.AssertTrue;
import se.curity.identityserver.sdk.web.Request;

import java.util.Objects;

public class RequestModel
{
    private static final String PERSONAL_NUMBER_PARAM = "personalNumber";

    private final String _personalNumber;
    private final boolean _isGetRequest;

    RequestModel(Request request)
    {
        _personalNumber = request.getFormParameterValueOrError(PERSONAL_NUMBER_PARAM);
        _isGetRequest = request.isGetRequest();
    }

    public String getPersonalNumber()
    {
        return _personalNumber;
    }

    @AssertTrue(message = "validation.error.personalNumber.required")
    public boolean isValid()
    {
        return _isGetRequest || (Objects.nonNull(_personalNumber) && !_personalNumber.isEmpty());
    }
}

