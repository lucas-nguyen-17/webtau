/*
 * Copyright 2022 webtau maintainers
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

package org.testingisdocumenting.webtau.expectation.equality

import org.junit.Test
import org.testingisdocumenting.webtau.data.ValuePath

import static org.testingisdocumenting.webtau.expectation.equality.ActualExpectedTestReportExpectations.simpleActualExpectedWithIntegers

class LessThanMatcherTest {
    private final int expected = 8
    private final ValuePath actualPath = new ValuePath('value')
    private final LessThanMatcher matcher = new LessThanMatcher(expected)

    @Test
    void "positive match"() {
        def actual = expected - 1

        assert matcher.matches(actualPath, actual)
        assert matcher.matchedMessage(actualPath, actual) == "less than $expected"

    }

    @Test
    void "positive mismatch"() {
        def actual = expected
        assert !matcher.matches(actualPath, actual)
        assert matcher.mismatchedMessage(actualPath, actual) == 'mismatches:\n\n' +
            simpleActualExpectedWithIntegers(actual, 'less than', expected)
    }

    @Test
    void "negative match"() {
        def actual = expected + 1
        assert matcher.negativeMatches(actualPath, actual)
        assert matcher.negativeMatchedMessage(actualPath, actual) == "greater than or equal to $expected"
    }

    @Test
    void "negative mismatch"() {
        def actual = expected - 1
        assert !matcher.negativeMatches(actualPath, actual)
        assert matcher.negativeMismatchedMessage(actualPath, actual) == 'mismatches:\n\n' +
            simpleActualExpectedWithIntegers(actual, 'greater than or equal to', expected)
    }

    @Test
    void "matching message"() {
        assert matcher.matchingMessage() == "to be less than $expected"
    }

    @Test
    void "negative matching message"() {
        assert matcher.negativeMatchingMessage() == "to be greater than or equal to $expected"
    }

    @Test
    void "equal comparison with matcher renders matching logic in case of comparison with null"() {
        CompareToComparator comparator = CompareToComparator.comparator()
        comparator.compareIsEqual(actualPath, null, matcher)
        assert comparator.generateEqualMismatchReport().contains('expected: <less than 8>')
    }
}
