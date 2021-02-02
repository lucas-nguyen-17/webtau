/*
 * Copyright 2020 webtau maintainers
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

package org.testingisdocumenting.webtau.db

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.testingisdocumenting.webtau.reporter.StepReporter
import org.testingisdocumenting.webtau.reporter.StepReporters
import org.testingisdocumenting.webtau.reporter.WebTauStep

import static org.testingisdocumenting.webtau.Matchers.contain
import static org.testingisdocumenting.webtau.db.DatabaseFacade.db

class DatabaseStepReporterTest extends DatabaseBaseTest implements StepReporter {
    List<String> stepMessages = []

    @Before
    void initiateStepReporter() {
        stepMessages.clear()
        StepReporters.add(this)
    }

    @After
    void unregisterStepReporter() {
        StepReporters.remove(this)
    }

    @Test
    void "query result comparison step should capture query and params"() {
        setupPrices()

        def price = db.createQuery("select price from PRICES where id=:id", [id: 'id1'])
        price.should == 1000
        price.shouldNot == 2000

        def fullMessage = stepMessages.join('\n')
        fullMessage.should contain("select price from PRICES where id=:id with {id=id1} equals 1000")
        fullMessage.should contain("select price from PRICES where id=:id with {id=id1} doesn't equal 2000")
    }

    @Test
    void "query result comparison step should capture query and params in case of single param"() {
        setupPrices()

        def price = db.createQuery("select price from PRICES where id=:id", 'id1')
        price.should == 1000

        def fullMessage = stepMessages.join('\n')
        fullMessage.should contain("select price from PRICES where id=:id with {id=id1} equals 1000")
    }

    @Test
    void "query result comparison step should not capture params when no params ara passed"() {
        setupPrices()

        def price = db.createQuery("select price from PRICES where id='id1'")
        price.should == 1000

        def fullMessage = stepMessages.join('\n')
        fullMessage.should contain("select price from PRICES where id='id1' equals 1000")
    }

    @Override
    void onStepStart(WebTauStep step) {
    }

    @Override
    void onStepSuccess(WebTauStep step) {
        stepMessages << step.completionMessage.toString()
    }

    @Override
    void onStepFailure(WebTauStep step) {
        stepMessages << step.completionMessage.toString()
    }
}
