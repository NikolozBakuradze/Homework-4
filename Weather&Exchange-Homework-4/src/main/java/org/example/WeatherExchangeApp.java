package org.example;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

class WeatherExchangeApp {
    // OpenWeatherMap API key
    private static final String WEATHER_API_KEY = "5ca911193c2836333858515460f205a1";

    // ExchangeRate API key
    private static final String EXCHANGE_API_KEY = "c9621e460d1a084f277a13d3";

    // HttpClient instance to send HTTP requestsLo
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    public static void main(String[] args) {
        try {
            // Prompt user for location information
            LocationInfo location = promptForLocation();
            System.out.println();

            // Prompt user for currency codes
            CurrencyPair currencies = promptForCurrencies();

            // Fetch and display weather information
            System.out.println("\nFetching weather data...");
            displayWeatherInfo(location);

            // Fetch and display exchange rate information
            System.out.println("\nFetching exchange rate data...");
            displayExchangeRate(currencies);
        } catch (Exception e) {
            // Handle any exceptions that occur during execution
            System.err.println("An error occurred: " + e.getMessage());
        }
    }

    // Fetch JSON data from the specified API URL
    private static JsonElement fetchJsonFromApi(String apiUrl) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 401) {
            throw new IOException("Unauthorized: Check your API key.");
        } else if (response.statusCode() != 200) {
            throw new IOException("API request failed with status code: " + response.statusCode());
        }

        return JsonParser.parseString(response.body());
    }

    // Prompt user for city and state information
    private static LocationInfo promptForLocation() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter city: ");
        String city = scanner.nextLine().trim();
        System.out.print("Enter state code (If applicable): ");
        String stateCode = scanner.nextLine().trim();

        return new LocationInfo(city, stateCode);
    }

    // Prompt user for source and target currency codes
    private static CurrencyPair promptForCurrencies() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter source currency code: ");
        String from = scanner.nextLine().trim().toUpperCase();
        System.out.print("Enter target currency code: ");
        String to = scanner.nextLine().trim().toUpperCase();

        return new CurrencyPair(from, to);
    }

    // Display weather information for the given location
    private static void displayWeatherInfo(LocationInfo location) throws IOException, InterruptedException {
        String[] coordinates = getCoordinates(location);
        String unit = location.stateCode.isEmpty() ? "metric" : "imperial";
        String weatherUrl = String.format("https://api.openweathermap.org/data/2.5/onecall?lat=%s&lon=%s&appid=%s&units=%s",
                coordinates[0], coordinates[1], WEATHER_API_KEY, unit);

        JsonObject weatherData = fetchJsonFromApi(weatherUrl).getAsJsonObject();
        JsonObject currentWeather = weatherData.getAsJsonObject("current");

        System.out.println("Current Weather: " + currentWeather.getAsJsonArray("weather").get(0).getAsJsonObject().get("description").getAsString());

        displaySunriseSunset(currentWeather);
        displayTemperature(currentWeather, unit);

        System.out.println("Humidity: " + currentWeather.get("humidity").getAsInt() + "%");
        System.out.println("Wind Speed: " + currentWeather.get("wind_speed").getAsDouble() + (unit.equals("metric") ? " m/s" : " mph"));
    }

    // Display sunrise and sunset times
    private static void displaySunriseSunset(JsonObject currentWeather) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        LocalDateTime sunrise = LocalDateTime.ofInstant(Instant.ofEpochSecond(currentWeather.get("sunrise").getAsLong()), ZoneId.systemDefault());
        LocalDateTime sunset = LocalDateTime.ofInstant(Instant.ofEpochSecond(currentWeather.get("sunset").getAsLong()), ZoneId.systemDefault());

        System.out.println("Sunrise: " + sunrise.format(formatter) + " | Sunset: " + sunset.format(formatter));
    }

    // Display temperature and "feels like" temperature
    private static void displayTemperature(JsonObject currentWeather, String unit) {
        double temp = currentWeather.get("temp").getAsDouble();
        double feelsLike = currentWeather.get("feels_like").getAsDouble();
        String unitSymbol = unit.equals("metric") ? "°C" : "°F";

        System.out.printf("Temperature: %.1f%s | Feels Like: %.1f%s%n", temp, unitSymbol, feelsLike, unitSymbol);
    }

    // Fetch coordinates (latitude and longitude) for the given location
    private static String[] getCoordinates(LocationInfo location) throws IOException, InterruptedException {
        String geocodingUrl = String.format("https://api.openweathermap.org/geo/1.0/direct?q=%s%s&appid=%s",
                location.city, location.stateCode.isEmpty() ? "" : "," + location.stateCode, WEATHER_API_KEY);

        JsonElement jsonElement = fetchJsonFromApi(geocodingUrl);
        JsonObject coordinates;

        if (jsonElement.isJsonArray()) {
            coordinates = jsonElement.getAsJsonArray().get(0).getAsJsonObject();
        } else {
            coordinates = jsonElement.getAsJsonObject();
        }

        return new String[]{coordinates.get("lat").getAsString(), coordinates.get("lon").getAsString()};
    }

    // Display exchange rate for the given currency pair
    private static void displayExchangeRate(CurrencyPair currencies) throws IOException, InterruptedException {
        String exchangeUrl = String.format("https://v6.exchangerate-api.com/v6/%s/latest/%s", EXCHANGE_API_KEY, currencies.from);
        JsonObject exchangeData = fetchJsonFromApi(exchangeUrl).getAsJsonObject();

        double rate = exchangeData.getAsJsonObject("conversion_rates").get(currencies.to).getAsDouble();
        System.out.printf("Exchange rate: 1 %s = %.4f %s%n", currencies.from, rate, currencies.to);
    }
}



