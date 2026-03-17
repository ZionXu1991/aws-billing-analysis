package com.devops.billing.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.athena.AthenaClient;
import software.amazon.awssdk.services.athena.model.*;

import java.time.LocalDate;
import java.util.*;

@Service
@Slf4j
public class AthenaQueryService {

    private final AthenaClient athenaClient;

    @Value("${billing.athena.database}")
    private String database;

    @Value("${billing.athena.workgroup}")
    private String workgroup;

    @Value("${billing.athena.output-location}")
    private String outputLocation;

    public AthenaQueryService(AthenaClient athenaClient) {
        this.athenaClient = athenaClient;
    }

    public List<Map<String, String>> executeQuery(String sql) {
        log.info("Executing Athena query: {}", sql);

        StartQueryExecutionRequest startRequest = StartQueryExecutionRequest.builder()
                .queryString(sql)
                .queryExecutionContext(QueryExecutionContext.builder().database(database).build())
                .workGroup(workgroup)
                .resultConfiguration(ResultConfiguration.builder().outputLocation(outputLocation).build())
                .build();

        StartQueryExecutionResponse startResponse = athenaClient.startQueryExecution(startRequest);
        String queryExecutionId = startResponse.queryExecutionId();

        log.info("Athena query started with execution ID: {}", queryExecutionId);

        // Poll until query completes
        GetQueryExecutionRequest executionRequest = GetQueryExecutionRequest.builder()
                .queryExecutionId(queryExecutionId)
                .build();

        while (true) {
            GetQueryExecutionResponse executionResponse = athenaClient.getQueryExecution(executionRequest);
            QueryExecutionStatus status = executionResponse.queryExecution().status();
            QueryExecutionState state = status.state();

            if (state == QueryExecutionState.SUCCEEDED) {
                log.info("Athena query succeeded: {}", queryExecutionId);
                break;
            } else if (state == QueryExecutionState.FAILED) {
                String reason = status.stateChangeReason();
                log.error("Athena query failed: {}", reason);
                throw new RuntimeException("Athena query failed: " + reason);
            } else if (state == QueryExecutionState.CANCELLED) {
                throw new RuntimeException("Athena query was cancelled");
            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for Athena query", e);
            }
        }

        // Retrieve results
        GetQueryResultsRequest resultsRequest = GetQueryResultsRequest.builder()
                .queryExecutionId(queryExecutionId)
                .build();

        List<Map<String, String>> results = new ArrayList<>();
        List<String> columnNames = null;
        String nextToken = null;

        do {
            GetQueryResultsRequest.Builder requestBuilder = GetQueryResultsRequest.builder()
                    .queryExecutionId(queryExecutionId);
            if (nextToken != null) {
                requestBuilder.nextToken(nextToken);
            }

            GetQueryResultsResponse resultsResponse = athenaClient.getQueryResults(requestBuilder.build());
            List<Row> rows = resultsResponse.resultSet().rows();

            if (columnNames == null && !rows.isEmpty()) {
                columnNames = new ArrayList<>();
                for (Datum datum : rows.get(0).data()) {
                    columnNames.add(datum.varCharValue());
                }
                rows = rows.subList(1, rows.size());
            }

            for (Row row : rows) {
                Map<String, String> rowMap = new LinkedHashMap<>();
                List<Datum> data = row.data();
                for (int i = 0; i < columnNames.size() && i < data.size(); i++) {
                    rowMap.put(columnNames.get(i), data.get(i).varCharValue());
                }
                results.add(rowMap);
            }

            nextToken = resultsResponse.nextToken();
        } while (nextToken != null);

        log.info("Athena query returned {} rows", results.size());
        return results;
    }

    public String buildDailyCostQuery(LocalDate date) {
        String year = String.valueOf(date.getYear());
        String month = String.valueOf(date.getMonthValue());
        String dateStr = date.toString();

        return String.format(
                "SELECT line_item_usage_account_id, " +
                        "DATE(line_item_usage_start_date) AS usage_date, " +
                        "line_item_product_code, " +
                        "product_region, " +
                        "line_item_usage_type, " +
                        "SUM(line_item_unblended_cost) AS unblended_cost, " +
                        "SUM(line_item_blended_cost) AS blended_cost, " +
                        "SUM(line_item_usage_amount) AS usage_amount " +
                        "FROM %s.cur_data " +
                        "WHERE year='%s' AND month='%s' AND DATE(line_item_usage_start_date)=DATE'%s' " +
                        "GROUP BY 1,2,3,4,5",
                database, year, month, dateStr
        );
    }
}
