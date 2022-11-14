package org.vexelon.net.kraken.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.vexelon.net.kraken.websocket.model.BookValue;
import org.vexelon.net.kraken.websocket.model.MarketValue;
import org.vexelon.net.kraken.websocket.model.MarketValueUpdate;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

public class App {
    static List<BookValue> btc_ask = new ArrayList<>();
    static List<BookValue> btc_bis = new ArrayList<>();
    static List<BookValue> eth_ask = new ArrayList<>();
    static List<BookValue> eth_bis = new ArrayList<>();

    static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public static void main(String[] args) {

        String publicWebSocketURL = "wss://ws.kraken.com/";
        String publicWebSocketSubscriptionMsg =
                "{ \"event\": \"subscribe\", \"subscription\": { \"name\": \"book\"}, \"pair\": [ \"ETH/USD\",\"BTC/USD\" ]}";
        OpenAndStreamWebSocketSubscription(publicWebSocketURL, publicWebSocketSubscriptionMsg);
    }

    public static void OpenAndStreamWebSocketSubscription(String connectionURL, String webSocketSubscription) {
        try {

            CountDownLatch latch = new CountDownLatch(1);
            WebSocket ws = HttpClient
                    .newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(URI.create(connectionURL), new WebSocketClient(latch))
                    .join();
            ws.sendText(webSocketSubscription, true);
            latch.await();

        } catch (

                Exception e) {
            System.out.println();
            System.out.println("AN EXCEPTION OCCURED :(");
            System.out.println(e);
        }
    }

    private static class WebSocketClient implements WebSocket.Listener {
        private final CountDownLatch latch;

        public WebSocketClient(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();
            System.out.println(dtf.format(now) + " -> onOpen: " + webSocket.getSubprotocol());
            WebSocket.Listener.super.onOpen(webSocket);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence charData, boolean last) {
            String data = charData.toString();
            System.out.println(data);
            if (data.contains("\"as\"") || data.contains("\"bs\"")) {
                fetchExchangeInitData(data);
                if (data.contains("XBT/USD")) {
                    System.out.println("XBT/USD");
                    printExchangeData(btc_ask, btc_bis);
                } else if (data.contains("ETH/USD")) {
                    System.out.println("ETH/USD");
                    printExchangeData(eth_ask, eth_bis);
                }
            }

            if (data.contains("\"a\"") || data.contains("\"b\"")) {
                updateExchangeData(data);
                if (data.contains("XBT/USD")) {
                    System.out.println("BTC/USD");
                    printExchangeData(btc_ask, btc_bis);
                } else if (data.contains("ETH/USD")) {
                    System.out.println("ETH/USD");
                    printExchangeData(eth_ask, eth_bis);
                }
            }

            return WebSocket.Listener.super.onText(webSocket, data, false);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            System.out.println("ERROR OCCURED: " + webSocket.toString());
            WebSocket.Listener.super.onError(webSocket, error);
        }
    }

    public static void updateExchangeData(String data) {
        String bookData = data.substring(data.indexOf("{"), data.indexOf("}") + 1);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            MarketValueUpdate marketValue = objectMapper.readValue(bookData, MarketValueUpdate.class);
            if (marketValue.getA() != null) {
                for (List<String> values : marketValue.getA()) {
                    if (data.contains("XBT/USD")) {
                        fetchData(values, btc_ask);
                        btc_ask = new ArrayList<>(btc_ask.subList(btc_ask.size() - 10, btc_ask.size()));
                    } else if (data.contains("ETH/USD")) {
                        fetchData(values, eth_ask);
                        eth_ask = new ArrayList<>(eth_ask.subList(eth_ask.size() - 10, eth_ask.size()));
                    }

                }
            }

            if (marketValue.getB() != null) {
                for (List<String> values : marketValue.getB()) {
                    if (data.contains("XBT/USD")) {
                        fetchData(values, btc_bis);
                        btc_bis = new ArrayList<>(btc_bis.subList(0, 10));
                    } else if (data.contains("ETH/USD")) {
                        fetchData(values, eth_bis);
                        eth_bis = new ArrayList<>(eth_bis.subList(0, 10));
                    }
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void fetchExchangeInitData(String data) {
        String bookData = data.substring(data.indexOf("{"), data.indexOf("}") + 1);

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            MarketValue marketValue = objectMapper.readValue(bookData, MarketValue.class);
            for (List<String> values : marketValue.getAs()) {
                if (data.contains("XBT/USD")) {
                    fetchData(values, btc_ask);
                } else if (data.contains("ETH/USD")) {
                    fetchData(values, eth_ask);
                }
            }
            for (List<String> values : marketValue.getBs()) {
                if (data.contains("XBT/USD")) {
                    fetchData(values, btc_bis);
                } else if (data.contains("ETH/USD")) {
                    fetchData(values, eth_bis);
                }
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static void fetchData(List<String> values, List<BookValue> list_date) {
        list_date.add(BookValue.builder()
                .price(new BigDecimal(values.get(0)))
                .volume(new BigDecimal(values.get(1)))
                .build());
        Collections.sort(list_date);
    }

    private static void printExchangeData(List<BookValue> ask_data, List<BookValue> bis_data) {
        System.out.println("ask");
        ask_data.forEach(data -> System.out.println(data.getPrice().setScale(2, RoundingMode.HALF_EVEN) + ", " + data.getPrice().multiply(data.getVolume()).setScale(2, RoundingMode.HALF_EVEN)));
        System.out.println("bids:");
        bis_data.forEach(data -> System.out.println(data.getPrice().setScale(2, RoundingMode.HALF_EVEN) + ", " + data.getPrice().multiply(data.getVolume()).setScale(2, RoundingMode.HALF_EVEN)));
        System.out.println("Best ask: " + ask_data.get(ask_data.size() - 1).getPrice().setScale(2, RoundingMode.HALF_EVEN));
        System.out.println("Best bids: " + bis_data.get(0).getPrice().setScale(2, RoundingMode.HALF_EVEN));
        LocalDateTime now = LocalDateTime.now();
        System.out.println(dtf.format(now));
        System.out.println("-------------------");

    }
}
