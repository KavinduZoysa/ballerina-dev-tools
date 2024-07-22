package io.ballerina.flowmodelgenerator.extension;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import io.ballerina.flowmodelgenerator.extension.request.FlowModelSourceGeneratorRequest;
import org.eclipse.lsp4j.TextEdit;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ResourceSourceGeneratorTest extends AbstractLSTest {

    private static final Type textEditListType = new TypeToken<List<TextEdit>>() {
    }.getType();

    @Override
    @Test(dataProvider = "data-provider")
    public void test(Path config) throws IOException {
        Path configJsonPath = resDir.resolve(config);
        TestConfig testConfig = gson.fromJson(Files.newBufferedReader(configJsonPath), TestConfig.class);

        FlowModelSourceGeneratorRequest request = new FlowModelSourceGeneratorRequest(testConfig.diagram());
        JsonArray jsonArray = getResponse(request).getAsJsonArray("textEdits");

        List<TextEdit> actualTextEdits = gson.fromJson(jsonArray, textEditListType);
        if (!assertArray("text edits", actualTextEdits, testConfig.output())) {
            TestConfig updatedConfig = new TestConfig(testConfig.description(), testConfig.diagram(), actualTextEdits);
//            updateConfig(configJsonPath, updatedConfig);
            Assert.fail(String.format("Failed test: '%s' (%s)", testConfig.description(), configJsonPath));
        }
    }

    @Override
    protected String getResourceDir() {
        return "resource_to_source";
    }

    @Override
    protected Class<? extends AbstractLSTest> clazz() {
        return ResourceSourceGeneratorTest.class;
    }

    @Override
    protected String getApiName() {
        return "getHttpResourceSourceCode";
    }

    /**
     * Represents the test configuration for the source generator test.
     *
     * @param description The description of the test
     * @param diagram     The diagram to generate the source code
     * @param output      The expected output source code
     */
    private record TestConfig(String description, JsonElement diagram, List<TextEdit> output) {

        public String description() {
            return description == null ? "" : description;
        }
    }
}
