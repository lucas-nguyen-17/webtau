/*
 * Copyright 2023 webtau maintainers
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

package org.testingisdocumenting.webtau.data.datanode;

import org.testingisdocumenting.webtau.data.traceable.CheckLevel;
import org.testingisdocumenting.webtau.data.traceable.TraceableValue;
import org.testingisdocumenting.webtau.data.ValuePath;
import org.testingisdocumenting.webtau.expectation.contain.ContainAnalyzer;
import org.testingisdocumenting.webtau.expectation.contain.ContainHandler;
import org.testingisdocumenting.webtau.expectation.contain.handlers.IndexedValue;
import org.testingisdocumenting.webtau.expectation.contain.handlers.IterableContainAnalyzer;
import org.testingisdocumenting.webtau.expectation.equality.CompareToComparator;

import java.util.List;

import static org.testingisdocumenting.webtau.WebTauCore.*;
import static org.testingisdocumenting.webtau.expectation.equality.CompareToComparator.AssertionMode;
import static org.testingisdocumenting.webtau.expectation.equality.CompareToComparator.comparator;

public class DataNodeListAndValueContainHandler implements ContainHandler {
    @Override
    public boolean handle(Object actual, Object expected) {
        return actual instanceof DataNode && ((DataNode) actual).isList();
    }

    @Override
    public void analyzeContain(ContainAnalyzer containAnalyzer, ValuePath actualPath, Object actual, Object expected) {
        List<DataNode> dataNodes = getDataNodes(actual);
        IterableContainAnalyzer analyzer = new IterableContainAnalyzer(actualPath, dataNodes, expected, false);
        List<IndexedValue> indexedValues = TraceableValue.withDisabledChecks(analyzer::findContainingIndexedValues);

        // earlier, traceable value is disabled and indexes of matches are found
        // it is done to avoid marking every mismatching entry as failed
        // now, for found entries we simulate comparison again but this time values will be properly marked as matched
        CompareToComparator comparator = comparator(AssertionMode.EQUAL);

        if (indexedValues.isEmpty()) {
            containAnalyzer.reportMismatchedValue(expected);

            dataNodes.forEach(n -> comparator.compareUsingEqualOnly(actualPath, n, expected));
        } else {
            indexedValues.forEach(iv -> comparator.compareUsingEqualOnly(actualPath, dataNodes.get(iv.idx()), expected));
        }
    }

    @Override
    public void analyzeNotContain(ContainAnalyzer containAnalyzer, ValuePath actualPath, Object actual, Object expected) {
        List<DataNode> dataNodes = getDataNodes(actual);
        IterableContainAnalyzer analyzer = new IterableContainAnalyzer(actualPath, dataNodes, expected, true);
        List<IndexedValue> indexedValues = TraceableValue.withDisabledChecks(analyzer::findContainingIndexedValues);

        if (indexedValues.isEmpty()) {
            dataNodes.forEach(n -> {
                if (n.getTraceableValue() != null) {
                    n.getTraceableValue().updateCheckLevel(CheckLevel.FuzzyPassed);
                }
            });
        } else {
            CompareToComparator comparator = comparator(AssertionMode.NOT_EQUAL);

            indexedValues.forEach(indexedValue -> {
                ValuePath indexedPath = actualPath.index(indexedValue.idx());

                containAnalyzer.reportMatch(this, indexedPath,
                        tokenizedMessage().error("equals").valueFirstLinesOnly(indexedValue.value()));
                comparator.compareUsingEqualOnly(indexedPath, dataNodes.get(indexedValue.idx()), expected);
            });
        }
    }

    private List<DataNode> getDataNodes(Object actual) {
        DataNode listNode = (DataNode) actual;
        return listNode.elements();
    }
}
