package pvzApp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.google.gson.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.document.internal.InternalUtils;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;



/**
 * Handler for requests to Lambda function.
 */
public class AllDevices implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");
        //cors here ???
        headers.put("Access-Control-Allow-Origin", "*");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        try {
            final String pageContents =
                    this.getPageContents("https://checkip.amazonaws.com");
            String s = pvzGetDevices();
            String output =
                    String.format(
                    "{ \"Devices inventory\": \n" + s + "\n}"
                    , pageContents);

            return response
                    .withStatusCode(200)
                    .withBody(output);
        } catch (IOException e) {
            return response
                    .withBody("{}")
                    .withStatusCode(500);
        }
    }

    private String getPageContents(String address) throws IOException{
        URL url = new URL(address);
        try(BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    private String pvzGetDevices(){

        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        //filter attr
        //Map<String, AttributeValue> expressionAttributeValues = new HashMap<String, AttributeValue>();
        //expressionAttributeValues.put(":val", new AttributeValue().withS(deviceStatusAttribute));

        ScanRequest scanRequest = new ScanRequest()
                .withTableName("pvzDeviceLoanDevices");
                //.withFilterExpression("deviceStatus = :val")
                //.withExpressionAttributeValues(expressionAttributeValues);
        ScanResult result = client.scan(scanRequest);

        List<HashMap<String, Object>> mapList = new ArrayList<>();
        //go thru list of <String, AttributeValue> map items and dig out key-value pairs for each entity
        //here navigate thru list of entities
        for (Map<String, AttributeValue> item : result.getItems()){
            Set<String> keys = item.keySet();
            HashMap<String, Object> jsonMap = new HashMap<String, Object>();
            //here navigate thru key-value pairs inside entity
            for (String key : keys) {
                jsonMap.put(key, item.get(key).getS());
            }
            //build it up into something JSONable
            mapList.add(jsonMap);
        }
        //now JSON it
        Gson gson = new Gson();
        String s = new Gson().toJson(mapList);

        //here we are
        return s;
    }
}
