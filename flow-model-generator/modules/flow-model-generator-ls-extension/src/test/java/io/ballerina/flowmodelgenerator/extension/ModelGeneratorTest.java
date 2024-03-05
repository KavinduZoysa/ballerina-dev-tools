/*
 *  Copyright (c) 2023, WSO2 LLC. (http://www.wso2.com)
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

package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelGeneratorServiceRequest;
import io.ballerina.tools.text.LinePosition;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Test cases for the flow model generator service.
 *
 * @since 2201.9.0
 */
public class ModelGeneratorTest extends AbstractLSTest {

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = CONFIG_DIR.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);
        String response = getResponse(new FlowModelGeneratorServiceRequest(
                SOURCE_DIR.resolve(testConfig.source()).toAbsolutePath().toString(), testConfig.start(),
                testConfig.end()));

        JsonObject json = JsonParser.parseString(response).getAsJsonObject();
        JsonObject jsonModel = json.getAsJsonObject("result").getAsJsonObject("flowDesignModel");

        // Assert only the file name since the absolute path may vary depending on the machine
        String balFileName = Path.of(jsonModel.getAsJsonPrimitive("fileName").getAsString()).getFileName().toString();
        JsonPrimitive testFileName = testConfig.diagram().getAsJsonPrimitive("fileName");
        boolean fileNameEquality = testFileName != null && balFileName.equals(testFileName.getAsString());
        JsonObject modifiedDiagram = jsonModel.deepCopy();
        modifiedDiagram.addProperty("fileName", balFileName);

        boolean flowEquality = modifiedDiagram.equals(testConfig.diagram());
        if (!fileNameEquality || !flowEquality) {
            TestConfig updatedConfig = new TestConfig(testConfig.start(), testConfig.end(), testConfig.source(),
                    testConfig.description(), modifiedDiagram);
            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "diagram_generator";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ModelGeneratorTest.class;
    }

    @Override
    protected String getServiceName() {
        return "getFlowDesignModel";
    }

    /**
     * Represents the test configuration.
     */
    private record TestConfig(LinePosition start, LinePosition end, String source, String description,
                              JsonObject diagram) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}