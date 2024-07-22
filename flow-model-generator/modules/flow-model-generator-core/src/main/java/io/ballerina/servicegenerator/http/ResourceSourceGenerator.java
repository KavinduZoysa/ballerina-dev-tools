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

package io.ballerina.servicegenerator.http;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.flowmodelgenerator.core.CommonUtils;
import io.ballerina.servicegenerator.http.model.Resource;
import io.ballerina.tools.text.LinePosition;
import io.ballerina.tools.text.LineRange;
import org.eclipse.lsp4j.TextEdit;

import java.util.List;

/**
 * Generates source code from the http resource.
 *
 * @since 1.4.0
 */
public class ResourceSourceGenerator {
    Gson gson = new Gson();

    public ResourceSourceGenerator() {

    }

    public JsonElement toSourceCode(JsonElement form) {
        Resource resource = gson.fromJson(form, Resource.class);
        LineRange lr = LineRange.from("file.bal", LinePosition.from(0,0), LinePosition.from(1,1));
        List<TextEdit> textEdits = List.of(
                new TextEdit(CommonUtils.toRange(resource.lineRange()), resource.toSource())
        );
        return gson.toJsonTree(textEdits);
    }
}
