/*
 * Copyright 2021 webtau maintainers
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

package org.testingisdocumenting.webtau

import org.junit.Test
import org.testingisdocumenting.webtau.data.table.TableData

import java.time.LocalDate

import static org.testingisdocumenting.webtau.WebTauCore.*
import static org.testingisdocumenting.webtau.testutils.TestConsoleOutput.runExpectExceptionAndValidateOutput

class MatchersGroovyTest {
    private final List<String> messages = Arrays.asList("message one", "message two", "message we wait for")
    private int messagesIdx = 0
    private int numberOfRecords = 0

    @Test
    void "list of strings"() {
        def list = ["hello", "world"]
        list.shouldNot == ["help", "what"]
    }

    @Test
    void "string comparison example"() {
        // string-string-example
        def errorMessage = generateErrorMessage()
        errorMessage.should == "insufficient disk space" // string and string equality comparison
        // string-string-example
    }

    @Test
    void "string wait example"() {
        // wait-consume-message
        actual(liveValue(this.&consumeMessage)).waitTo == "message we wait for"
        // wait-consume-message
    }

    @Test
    void "number wait example"() {
        // wait-number-records
        actual(liveValue(this.&countRecords)).waitToBe >= 5
        // wait-number-records
    }

    private String consumeMessage() {
        return messages.get(messagesIdx++)
    }

    @Test
    void "number and string example"() {
        // string-number-example
        def numberAsText = "200"
        numberAsText.shouldBe > 150 // text and number relative comparison
        // string-number-example
    }

    @Test
    void "number comparison example"() {
        // number-number-example
        double price = calculatePrice()
        actual(price, "price").shouldBe > 10 // explict name to use in reporting
        // number-number-example
    }

    @Test
    void "bean and map equality example"() {
        runExpectExceptionAndValidateOutput(AssertionError, ~/expected: "My Second Account"/) {
            // bean-map-example
            def account = new Account("ac1", "My Account", "test account", new Address("TestingCity", "88888888"))
            account.should == [
                    id: "ac1",
                    name: "My Second Account",
                    address: [zipCode: "7777777"]] // only specified properties will be compared
            // bean-map-example
        }
    }

    @Test
    void "java record and map equality example"() {
        runExpectExceptionAndValidateOutput(AssertionError, ~/expected: "tea sets"/) {
            // java-record-map-example
            def wishListItem = new WishLitItem("id1", "tea set", true,
                    [new WishLitItem("id-nested", "tea", true, [])])
            wishListItem.should == [
                    id: "id1",
                    description: "tea sets",
                    favorite: true,
                    related: [
                            [id: "id-nested",
                             description: "juice"]]] // only specified properties will be compared
            // java-record-map-example
        }
    }

    @Test
    void "any value table matcher example"() {
        // any-value-table-example
        TableData summaryTable = loadFromCsv("summary.csv")
        summaryTable.should == ["ColumnA" | "ColumnB" ] {
                                _________________________
                                        10| anyValue
                                        30| 40 }
        // any-value-table-example
    }

    @Test
    void "java records and table data equality"() {
        def wishListItem1 = new WishLitItem("id1", "tea set", true, [])
        def wishListItem2 = new WishLitItem("id2", "book set", false, [])
        def wishList = [wishListItem1, wishListItem2]

        wishList.should == ["id"  | "favorite"] {
                           ______________________
                            "id1" | true
                            "id2" | false }
    }

    @Test
    void "bean and map contains example"() {
        runExpectExceptionAndValidateOutput(AssertionError, ~/expected: not "ac2"/) {
            // bean-map-contains-example
            def accounts = fetchAccounts()
            accounts.shouldNot contain([id: "ac2", name: "Work"]) // only check specified properties
            // bean-map-contains-example
        }
    }

    @Test
    void "beans and table equality"() {
        runExpectExceptionAndValidateOutput(AssertionError, ~/expected: "zip8"/) {
            // beans-table-example
            List<Account> accounts = fetchAccounts()
            TableData expected = ["*id" | "name"       | "address"] { // id is a key column
                                 _________________________________________
                                  "ac2" | "Works"      | [zipCode: "zip2"] // when key is present, comparison is order agnostic
                                  "ac1" | "Home"       | [zipCode: "zip1"]
                                  "ac3" | "My Account" | [zipCode: "zip8"] }

            accounts.should == expected
            // beans-table-example
        }
    }

    @Test
    void "beans and table contain"() {
        runExpectExceptionAndValidateOutput(AssertionError, ~/no matches found for: \[\{"id": "ac2",/) {
            // beans-table-contain-example
            List<Account> accounts = fetchAccounts()
            TableData expected = ["*id" | "name"  | "address"] {
                                  ____________________________________
                                  "ac2" | "Works" | [zipCode: "zip2"]
                                  "ac1" | "Home"  | [zipCode: "zip1"] }

            accounts.should contain(expected)
            // beans-table-contain-example
        }
    }

    private static List<Account> fetchAccounts() {
        return [new Account("ac1", "Home", "test account", new Address("TC1", "zip1")),
                new Account("ac2", "Work", "test account", new Address("TC2", "zip2")),
                new Account("ac3", "My Account", "test account", new Address("TC3", "zip3"))]
    }

    @Test
    void "any of matcher example"() {
        def dateAsText = "2018-06-10"
        dateAsText.should == anyOf("2018-06-11", LocalDate.of(2018, 6, 10))
    }

    @Test
    void "any of matcher with other matcher example"() {
        def dateAsText = "2018-06-10"
        dateAsText.should == anyOf("2018-06-11", greaterThan(LocalDate.of(2018, 1, 1)))

        def message = "hello world"
        message.shouldNot == anyOf("hello", contain("super"))
    }

    @Test
    void "list failure example"() {
        code {
            def values = [1,
                          "testing",
                          [key1: "hello", key2: "world"]]
            // failed-list
            values.should == [
                    1,
                    "teasing",
                    [key1: "hello", key2: "work"]]
            // failed-list
        } should throwException(AssertionError)
    }

    private static String generateErrorMessage() {
        return "insufficient disk space"
    }

    private static double calculatePrice() {
        return 10.5
    }

    private static TableData loadFromCsv(String fileName) {
        return ["ColumnA" | "ColumnB" ] {
              _________________________
                        10| 20
                        30| 40 }
    }

    private int countRecords() {
        numberOfRecords += 1
        return numberOfRecords
    }
}
