/*
 * Copyright 2021 webtau maintainers
 * Copyright 2019 TWO SIGMA OPEN SOURCE, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testingisdocumenting.webtau.expectation.contain;

import org.testingisdocumenting.webtau.data.ValuePath;
import org.testingisdocumenting.webtau.data.converters.ValueConverter;
import org.testingisdocumenting.webtau.data.render.PrettyPrinter;
import org.testingisdocumenting.webtau.expectation.contain.handlers.IterableAndTableContainHandler;
import org.testingisdocumenting.webtau.expectation.contain.handlers.IterableAndSingleValueContainHandler;
import org.testingisdocumenting.webtau.expectation.contain.handlers.NullContainHandler;
import org.testingisdocumenting.webtau.expectation.equality.ValuePathMessage;
import org.testingisdocumenting.webtau.reporter.TokenizedMessage;
import org.testingisdocumenting.webtau.utils.ServiceLoaderUtils;
import org.testingisdocumenting.webtau.utils.TraceUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testingisdocumenting.webtau.WebTauCore.*;
import static org.testingisdocumenting.webtau.expectation.TokenizedReportUtils.*;

public class ContainAnalyzer {
    private static final List<ContainHandler> handlers = discoverHandlers();

    private final List<ValuePathMessage> matchMessages;
    private final List<ValuePathMessage> mismatchMessages;
    private final List<ValuePathMessage> missingMessages;

    private final Set<ValuePath> extraMismatchPaths;

    private final List<Object> mismatchedExpectedValues;

    private ValuePath topLevelActualPath;

    private final Map<ValuePath, Object> convertedActualByPath = new HashMap<>();

    public static ContainAnalyzer containAnalyzer() {
        return new ContainAnalyzer();
    }

    public List<Object> getMismatchedExpectedValues() {
        return mismatchedExpectedValues;
    }

    public boolean contains(ValuePath actualPath, Object actual, Object expected) {
        updateTopLevelActualPath(actualPath);

        return contains(actualPath, actual, expected, false,
                (handler, convertedActual, convertedExpected) -> handler.analyzeContain(this, actualPath, convertedActual, convertedExpected));
    }

    public boolean notContains(ValuePath actualPath, Object actual, Object expected) {
        updateTopLevelActualPath(actualPath);

        return contains(actualPath, actual, expected, true,
                (handler, convertedActual, convertedExpected) ->
                        handler.analyzeNotContain(this, actualPath, convertedActual, convertedExpected));
    }

    public ValueConverter createValueConverter() {
        return convertedActualByPath::getOrDefault;
    }

    public void reportMismatch(ContainHandler reporter, ValuePathMessage valuePathMessage) {
        mismatchMessages.add(valuePathMessage);
    }

    public void reportMismatches(ContainHandler reporter, List<ValuePathMessage> valuePathMessages) {
        mismatchMessages.addAll(valuePathMessages);
    }

    public void reportMismatch(ContainHandler reporter, ValuePath actualPath, TokenizedMessage mismatch) {
        reportMismatch(reporter, new ValuePathMessage(actualPath, mismatch));
    }

    public void reportMissing(ContainHandler reporter, ValuePath actualPath, Object value) {
        missingMessages.add(new ValuePathMessage(actualPath, tokenizedMessage().value(value)));
    }

    public void reportMissing(ContainHandler reporter, ValuePathMessage valuePathMessage) {
        missingMessages.add(valuePathMessage);
    }

    public void reportMissing(ContainHandler reporter, List<ValuePathMessage> valuePathMessages) {
        missingMessages.addAll(valuePathMessages);
    }

    public void reportMismatchedValue(Object oneOfExpectedValues) {
        mismatchedExpectedValues.add(oneOfExpectedValues);
    }

    public void reportMatch(ContainHandler reporter, ValuePath actualPath, TokenizedMessage mismatch) {
        matchMessages.add(new ValuePathMessage(actualPath, mismatch));
    }

    public Set<ValuePath> generateMatchPaths() {
        return extractActualPaths(matchMessages);
    }

    public Set<ValuePath> generateMismatchPaths() {
        HashSet<ValuePath> result = new HashSet<>(extraMismatchPaths);
        result.addAll(extractActualPaths(mismatchMessages));
        result.addAll(extractActualPaths(missingMessages));

        return result;
    }

    public TokenizedMessage generateMatchReport() {
        return TokenizedMessage.join("\n", matchMessages.stream().map(message ->
                message.getActualPath().equals(topLevelActualPath) ?
                        message.getMessage() :
                        message.getFullMessage()).collect(Collectors.toList()));
    }

    public TokenizedMessage generateMismatchReport() {
        TokenizedMessage reportDetails = generateMismatchReportDetails(mismatchedExpectedValues.isEmpty());

        return reportDetails.isEmpty() && mismatchedExpectedValues.isEmpty() ?
                tokenizedMessage().error("no match found") :
                reportDetails;
    }

    private TokenizedMessage generateMismatchReportDetails(boolean useStrictLabels) {
        if (missingMessages.isEmpty()) {
            return generateReportPartWithoutLabel(topLevelActualPath, Stream.of(mismatchMessages));
        }

        return combineReportParts(
                generateReportPart(topLevelActualPath, tokenizedMessage().matcher(useStrictLabels ?
                        "mismatches": "possible mismatches"),
                        Collections.singletonList(mismatchMessages)),
                generateReportPart(topLevelActualPath, tokenizedMessage().matcher(useStrictLabels ?
                        "missing values": "possible missing values"),
                        Collections.singletonList(missingMessages)));
    }

    public boolean noMismatches() {
        return mismatchMessages.isEmpty() && missingMessages.isEmpty() && mismatchedExpectedValues.isEmpty();
    }

    public boolean noMatches() {
        return matchMessages.isEmpty();
    }

    public void registerConvertedActualByPath(Map<ValuePath, Object> convertedActualByPath) {
        this.convertedActualByPath.putAll(convertedActualByPath);
    }

    public void registerExtraMismatchPaths(List<ValuePath> extraMismatchPaths) {
        this.extraMismatchPaths.addAll(extraMismatchPaths);
    }

    public void resetReportData() {
        mismatchMessages.clear();
        matchMessages.clear();
        mismatchedExpectedValues.clear();
        extraMismatchPaths.clear();
        missingMessages.clear();
    }

    private ContainAnalyzer() {
        this.matchMessages = new ArrayList<>();
        this.mismatchMessages = new ArrayList<>();
        this.missingMessages = new ArrayList<>();
        this.mismatchedExpectedValues = new ArrayList<>();
        this.extraMismatchPaths = new HashSet<>();
    }

    private boolean contains(ValuePath actualPath, Object actual, Object expected, boolean isNegative, ContainsLogic containsLogic) {
        ContainHandler handler = handlers.stream().
                filter(h -> h.handle(actual, expected)).findFirst().
                orElseThrow(() -> noHandlerFound(actual, expected));

        Object convertedActual = handler.convertedActual(actual, expected);
        recordConvertedActual(actualPath, actual, convertedActual);

        Object convertedExpected = handler.convertedExpected(actual, expected);

        int before = isNegative ? matchMessages.size() : (mismatchMessages.size() + missingMessages.size() + mismatchedExpectedValues.size());
        containsLogic.execute(handler, convertedActual, convertedExpected);
        int after = isNegative ? matchMessages.size() : (mismatchMessages.size() + missingMessages.size() + mismatchedExpectedValues.size());

        return after == before;
    }

    private Set<ValuePath> extractActualPaths(List<ValuePathMessage> notEqualMessages) {
        return notEqualMessages
                .stream()
                .map(ValuePathMessage::getActualPath)
                .collect(Collectors.toSet());
    }

    private void updateTopLevelActualPath(ValuePath actualPath) {
        if (topLevelActualPath == null) {
            topLevelActualPath = actualPath;
        }
    }

    private static List<ContainHandler> discoverHandlers() {
        List<ContainHandler> result = new ArrayList<>();
        result.add(new NullContainHandler());
        result.addAll(ServiceLoaderUtils.load(ContainHandler.class));
        result.add(new IterableAndTableContainHandler());
        result.add(new IterableAndSingleValueContainHandler());

        return result;
    }

    private void recordConvertedActual(ValuePath actualPath, Object actual, Object convertedActual) {
        if (actual == convertedActual) {
            return;
        }

        convertedActualByPath.put(actualPath, convertedActual);
    }

    private RuntimeException noHandlerFound(Object actual, Object expected) {
        return new RuntimeException(
                "no contains handler found for\nactual: " + PrettyPrinter.renderAsTextWithoutColors(actual) + " " + TraceUtils.renderType(actual) +
                        "\nexpected: " + PrettyPrinter.renderAsTextWithoutColors(expected) + " " + TraceUtils.renderType(expected));
    }

    interface ContainsLogic {
        void execute(ContainHandler handler, Object convertedActual, Object convertedExpected);
    }
}
