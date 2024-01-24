/*
 *  Copyright 2016-2024 Qameta Software Inc
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.qameta.allure.option;

import com.beust.jcommander.Parameter;

/**
 * Contains profile options.
 *
 * @since 2.0
 */
@SuppressWarnings("PMD.ImmutableField")
public class ReportNameOptions {

    @Parameter(
            names = {"--name", "--report-name"},
            description = "The report name."
    )
    private String reportName;

    public String getReportName() {
        return reportName;
    }

}