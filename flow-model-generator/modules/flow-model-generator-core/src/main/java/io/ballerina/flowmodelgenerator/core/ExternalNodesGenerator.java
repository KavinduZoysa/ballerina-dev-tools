/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.flowmodelgenerator.core.model.AvailableNode;
import io.ballerina.flowmodelgenerator.core.model.Codedata;
import io.ballerina.flowmodelgenerator.core.model.Metadata;
import io.ballerina.flowmodelgenerator.core.model.NodeKind;
import io.ballerina.flowmodelgenerator.core.model.node.NewConnectionBuilder;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates the connectors for the provided parameters.
 *
 * @since 2.0.0
 */
public class ExternalNodesGenerator {

    private final Gson gson = new Gson();
    private final SemanticModel semanticModel;

    public ExternalNodesGenerator(SemanticModel semanticModel) {
        this.semanticModel = semanticModel;
    }

    public JsonElement getExternalNodes() {
        Map<String, List<AvailableNode>> externalNodes = new HashMap<>();
        for (Symbol symbol : this.semanticModel.moduleSymbols()) {
            if (symbol.kind() == SymbolKind.MODULE) {
                ModuleSymbol moduleSymbol = (ModuleSymbol) symbol;
                ModuleID id = moduleSymbol.id();
                String org = id.orgName();
                String packageName = id.packageName();
                String moduleName = id.moduleName();
                String version = id.version();
                List<AvailableNode> classNodes = new ArrayList<>();
                for (ClassSymbol classSymbol : moduleSymbol.classes()) {
                    // TODO: Symbols should be filtered based on the annotation
                    Metadata metadata = new Metadata.Builder<>(null)
                            .label("")
                            .description("")
                            .icon(CommonUtils.generateIcon(org, packageName, version))
                            .build();
                    Codedata codedata = new Codedata.Builder<>(null)
                            // TODO: Node should be determined based on the annotation
                            // 1. Agent
                            // 2. General client class
                            .node(NodeKind.NEW_CONNECTION)
                            .org(org)
                            .module(packageName)
                            .object(NewConnectionBuilder.CLIENT_SYMBOL)
                            .symbol(classSymbol.getName().orElse(""))
                            .build();
                    classNodes.add(new AvailableNode(metadata, codedata, true));
                }
                if (!classNodes.isEmpty()) {
                    externalNodes.put(moduleName, classNodes);
                }
            }
        }
        return gson.toJsonTree(externalNodes);
    }
}
