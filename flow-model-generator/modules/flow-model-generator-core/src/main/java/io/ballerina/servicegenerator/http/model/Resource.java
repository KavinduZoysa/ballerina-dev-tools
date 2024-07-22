/*
 *  Copyright (c) 2024, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.servicegenerator.http.model;

import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.model.ExpressionAttributes;
import io.ballerina.flowmodelgenerator.core.model.FlowNode;
import io.ballerina.tools.text.LineRange;

import java.util.List;

/**
 * Represents the generalized http service.
 *
 * @since 1.4.0
 */
public class Resource {
    private static final String WHITE_SPACE = " ";
    private LineRange lineRange;
    private String method;
    // TODO: This should have a separate class to cover all combinations
    private String path;
    private String payloadType;
    private String payloadName;
    private String queryParams;
    private String headers;
    private ResponseType[] responseTypes;

    private record ResponseType(String name, int code, String messageType, String contentType) {
    }

    public String toSource() {
        StringBuilder sb = new StringBuilder("resource function ");
        sb.append(method).append(WHITE_SPACE);
        sb.append(path);
        sb.append(SyntaxKind.OPEN_PAREN_TOKEN.stringValue());
        sb.append(payloadType).append(WHITE_SPACE);
        sb.append(payloadName);
        sb.append(SyntaxKind.CLOSE_PAREN_TOKEN.stringValue()).append(WHITE_SPACE);
        sb.append("returns").append(WHITE_SPACE);
        appendReturnType(sb);
        sb.append(SyntaxKind.OPEN_BRACE_TOKEN.stringValue());
        sb.append(SyntaxKind.CLOSE_BRACE_TOKEN.stringValue());
        return sb.toString();
    }

    public LineRange lineRange() {
        return lineRange;
    }

    private void appendReturnType(StringBuilder sb) {
        int responseTypesLen = responseTypes.length;
        if (responseTypesLen == 0) {
            return;
        }
        sb.append(responseTypes[0].name);
        for (int i = 1; i < responseTypesLen; i ++) {
            sb.append(SyntaxKind.PIPE_TOKEN.stringValue()).append(responseTypes[i].name);
        }
        sb.append(WHITE_SPACE);
    }
}
