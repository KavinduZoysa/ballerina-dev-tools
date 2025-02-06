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

package io.ballerina.flowmodelgenerator.core.model.node;

import io.ballerina.compiler.api.ModuleID;
import io.ballerina.compiler.api.SemanticModel;
import io.ballerina.compiler.api.symbols.ClassSymbol;
import io.ballerina.compiler.api.symbols.Documentation;
import io.ballerina.compiler.api.symbols.MethodSymbol;
import io.ballerina.compiler.api.symbols.ModuleSymbol;
import io.ballerina.compiler.api.symbols.ParameterKind;
import io.ballerina.compiler.api.symbols.ParameterSymbol;
import io.ballerina.compiler.api.symbols.Symbol;
import io.ballerina.compiler.api.symbols.SymbolKind;
import io.ballerina.compiler.api.symbols.TypeSymbol;
import io.ballerina.compiler.syntax.tree.SyntaxKind;
import io.ballerina.flowmodelgenerator.core.db.model.Function;
import io.ballerina.flowmodelgenerator.core.db.model.FunctionResult;
import io.ballerina.flowmodelgenerator.core.db.model.Parameter;
import io.ballerina.flowmodelgenerator.core.db.model.ParameterResult;
import io.ballerina.flowmodelgenerator.core.model.*;
import io.ballerina.flowmodelgenerator.core.utils.CommonUtils;
import io.ballerina.flowmodelgenerator.core.utils.PackageUtil;
import io.ballerina.flowmodelgenerator.core.utils.ParamUtils;
import io.ballerina.projects.PackageDescriptor;
import io.ballerina.projects.Project;
import org.ballerinalang.langserver.commons.eventsync.exceptions.EventSyncException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceDocumentException;
import org.ballerinalang.langserver.commons.workspace.WorkspaceManager;
import org.eclipse.lsp4j.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Represents a new connection node in the flow model.
 *
 * @since 2.0.0
 */
public class ClassInitializeBuilder extends NodeBuilder {

    private static final String CLASS_INIT_LABEL = "Class Initialization";

    public static final String INIT_SYMBOL = "init";
    public static final String CLASS_SYMBOL = "Class";
    public static final String CHECK_ERROR_DOC = "Terminate on error";
    public static final String CLASS_NAME_LABEL = "Class Name";
    protected static final Logger LOG = LoggerFactory.getLogger(ClassInitializeBuilder.class);

    @Override
    public void setConcreteConstData() {
        metadata().label(CLASS_INIT_LABEL);
        codedata().node(NodeKind.CLASS_INIT).symbol("init");
    }

    @Override
    public Map<Path, List<TextEdit>> toSource(SourceBuilder sourceBuilder) {
        sourceBuilder
                .token().keyword(SyntaxKind.FINAL_KEYWORD).stepOut()
                .newVariable();

        sourceBuilder.token()
                .keyword(SyntaxKind.CHECK_KEYWORD)
                .keyword(SyntaxKind.NEW_KEYWORD)
                .stepOut()
                .functionParameters(sourceBuilder.flowNode,
                        Set.of(Property.VARIABLE_KEY, Property.TYPE_KEY, Property.SCOPE_KEY,
                                Property.CHECK_ERROR_KEY));
        return sourceBuilder.textEdit(false).acceptImport().build();
    }

    @Override
    public void setConcreteTemplateData(TemplateContext context) {
        Codedata codedata = context.codedata();
        WorkspaceManager workspaceManager = context.workspaceManager();
        Path filePath = context.filePath();
        if (isLocal(workspaceManager, filePath, codedata)) {
            throw new IllegalStateException("Organization, module or version is not defined for the client init node");
        }

        PackageUtil.loadProject(workspaceManager, filePath);
        SemanticModel semanticModel = workspaceManager.semanticModel(filePath).orElseThrow();
        String org = codedata.org();
        String mod = codedata.module();
        String version = codedata.version();
        ClassSymbol classSymbol = null;

        for (Symbol symbol : semanticModel.moduleSymbols()) {
            if (symbol.kind() != SymbolKind.MODULE) {
                continue;
            }
            ModuleSymbol moduleSymbol = (ModuleSymbol) symbol;
            ModuleID id = moduleSymbol.id();
            if (!(id.moduleName() != null && id.moduleName().equals(mod)) ||
                    !(id.orgName() != null && id.orgName().equals(org)) ||
                    !(id.version() != null && id.version().equals(version))) {
                continue;
            }
            classSymbol = getClassSymbol(moduleSymbol.classes(), codedata);
            break;
        }

        if (classSymbol == null) {
            throw new IllegalStateException("Class symbol not found for the client init node");
        }

        Optional<MethodSymbol> optInitMethodSymbol = classSymbol.initMethod();
        if (optInitMethodSymbol.isEmpty()) {
            throw new IllegalStateException("Init method symbol not found for the client init node");
        }
        MethodSymbol methodSymbol = optInitMethodSymbol.get();
        FunctionResult function = convertMethodSymbolToFunctionResult(methodSymbol, codedata,
                classSymbol.getName().get());
        List<ParameterResult> functionParameters = getParametersFromMethodSymbol(workspaceManager, methodSymbol);

        metadata()
                .label(function.packageName())
                .description(function.description())
                .icon(CommonUtils.generateIcon(function.org(), function.packageName(), function.version()));
        codedata()
                .node(NodeKind.CLASS_INIT)
                .org(function.org())
                .module(function.packageName())
                .object(CLASS_SYMBOL)
                .symbol(INIT_SYMBOL)
                .id(function.functionId())
                .isGenerated(codedata.isGenerated());

        boolean hasOnlyRestParams = functionParameters.size() == 1;
        for (ParameterResult paramResult : functionParameters) {
            if (paramResult.kind().equals(Parameter.Kind.PARAM_FOR_TYPE_INFER)
                    || paramResult.kind().equals(Parameter.Kind.INCLUDED_RECORD)) {
                continue;
            }

            String unescapedParamName = ParamUtils.removeLeadingSingleQuote(paramResult.name());
            Property.Builder<FormBuilder<NodeBuilder>> customPropBuilder = properties().custom();
            customPropBuilder
                    .metadata()
                        .label(unescapedParamName)
                        .description(paramResult.description())
                        .stepOut()
                    .codedata()
                        .kind(paramResult.kind().name())
                        .originalName(paramResult.name())
                        .importStatements(paramResult.importStatements())
                        .stepOut()
                    .placeholder(paramResult.defaultValue())
                    .typeConstraint(paramResult.type())
                    .editable()
                    .defaultable(paramResult.optional());

            if (paramResult.kind() == Parameter.Kind.INCLUDED_RECORD_REST) {
                if (hasOnlyRestParams) {
                    customPropBuilder.defaultable(false);
                }
                unescapedParamName = "additionalValues";
                customPropBuilder.type(Property.ValueType.MAPPING_EXPRESSION_SET);
            } else if (paramResult.kind() == Parameter.Kind.REST_PARAMETER) {
                if (hasOnlyRestParams) {
                    customPropBuilder.defaultable(false);
                }
                customPropBuilder.type(Property.ValueType.EXPRESSION_SET);
            } else if (paramResult.kind() == Parameter.Kind.REQUIRED) {
                customPropBuilder.type(Property.ValueType.EXPRESSION).value(paramResult.defaultValue());
            } else {
                customPropBuilder.type(Property.ValueType.EXPRESSION);
            }
            customPropBuilder
                    .stepOut()
                    .addProperty(unescapedParamName);
        }

        if (CommonUtils.hasReturn(function.returnType())) {
            properties()
                    .type(function.returnType(), false)
                    .data(function.returnType(), context.getAllVisibleSymbolNames(), CLASS_NAME_LABEL);
        }
        properties()
                .scope(Property.GLOBAL_SCOPE)
                .checkError(true, CHECK_ERROR_DOC, false);
    }

    private ClassSymbol getClassSymbol(List<ClassSymbol> classSymbols, Codedata codedata) {
        for (ClassSymbol classSymbol : classSymbols) {
            Optional<String> className = classSymbol.getName();
            if (className.isPresent() && className.get().equals(codedata.symbol())) {
                return classSymbol;
            }
        }
        return null;
    }

    private FunctionResult convertMethodSymbolToFunctionResult(MethodSymbol methodSymbol, Codedata codedata, String name) {
        String retType = codedata.module() + ":" + name;
        String description = "";
        Optional<Documentation> documentation = methodSymbol.documentation();
        if (documentation.isPresent()) {
            Optional<String> optDescription = documentation.get().description();
            if (optDescription.isPresent()) {
                description = optDescription.get();
            }
        }

        return new FunctionResult(-1, methodSymbol.getName().orElse(""), description, retType, codedata.module(),
                codedata.org(), codedata.version(), "", Function.Kind.CONNECTOR, false, false);
    }

    private List<ParameterResult> getParametersFromMethodSymbol(WorkspaceManager workspaceManager,
                                                                MethodSymbol methodSymbol) {
        List<ParameterResult> parameterResults = new ArrayList<>();
        Optional<List<ParameterSymbol>> optParams = methodSymbol.typeDescriptor().params();
        if (optParams.isEmpty()) {
            return parameterResults;
        }

        List<ParameterSymbol> paramSymbols = optParams.get();
        for (int i = 0; i < paramSymbols.size(); i++) {
            ParameterSymbol paramSymbol = paramSymbols.get(i);
            Optional<String> optParamName = paramSymbol.getName();
            String paramName = optParamName.orElse("param" + i);
            TypeSymbol paramType = paramSymbol.typeDescriptor();
            String type = CommonUtils.getTypeSignature(semanticModel, paramType, true);
            parameterResults.add(new ParameterResult(i, paramName, type, getParamKind(paramSymbol.paramKind()), "", "",
                    false, ""));
        }
        return parameterResults;
    }

    private Parameter.Kind getParamKind(ParameterKind kind) {
        return switch (kind) {
            case DEFAULTABLE -> Parameter.Kind.DEFAULTABLE;
            case INCLUDED_RECORD -> Parameter.Kind.INCLUDED_RECORD;
            case REST -> Parameter.Kind.REST_PARAMETER;
            default -> Parameter.Kind.REQUIRED;
        };
    }

    private boolean isLocal(WorkspaceManager workspaceManager, Path filePath, Codedata codedata) {
        if (codedata.org() == null || codedata.module() == null || codedata.version() == null) {
            return true;
        }
        try {
            Project project = workspaceManager.loadProject(filePath);
            PackageDescriptor descriptor = project.currentPackage().descriptor();
            String packageOrg = descriptor.org().value();
            String packageName = descriptor.name().value();
            String packageVersion = descriptor.version().value().toString();

            return packageOrg.equals(codedata.org())
                    && packageName.equals(codedata.module())
                    && packageVersion.equals(codedata.version());
        } catch (WorkspaceDocumentException | EventSyncException e) {
            return false;
        }
    }
}
