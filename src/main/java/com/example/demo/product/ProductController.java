package com.example.demo.product;

import jdk.swing.interop.SwingInterOpUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Set;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import org.checkerframework.checker.units.qual.s;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@RestController
@RequestMapping(path = "api/v1/product")
public class ProductController {

    private final ProductService productService;

    // above studentService should be autowired aka automatically instantiated for us (aka dependency injections)
    @Autowired
    public ProductController(ProductService productService) {
        // use method inside of StudentService
        this.productService = productService;
    }

    @GetMapping
    public List<Product> getProducts(@RequestParam HashMap<String, String> queryParams) {
//    public List<Product> getProducts() {

        HashMap<String, HashMap> targetRes;
        HashMap<String, HashMap> walmartRes;
        List<Product> products = new ArrayList<>();

//        String searchStr = queryParams.get("searchStr");

//        String searchStr = "tampons";
//        String searchStr = "plan b";
//        String searchStr = "emergency contraception";
//        String searchStr = "covid tests";
//        String searchStr = "antigen tests";
//        String searchStr = "bicycle";
//        String searchStr = "air conditioner";
        String searchStr = "baby formula";

        // anything baby formula related has special redirect pages that cannot be accessed through the traditional api scraping route for all other search terms, need to handle separately
        if (searchStr.toLowerCase().contains("formula")) {
            try {
                walmartRes = walmartBabyFormula();
            } catch (IOException e) {
                System.out.println(e);
                return new ArrayList<>();
            }
            for (String productId : walmartRes.keySet()) {
                HashMap<String, String> productValues = walmartRes.get(productId);
                Boolean availability = false;
                if (productValues.get("availability").equals("true")) {
                    availability = true;
                }
                Product newProduct = new Product(productValues.get("name"), productValues.get("brand"), "Walmart", Double.parseDouble(productValues.get("price")), availability, null, productValues.get("imageURL"), productValues.get("buyURL"));
                products.add(newProduct);
            }

            try {
                targetRes = targetBabyFormula();
            } catch (IOException e) {
                System.out.println(e);
                return new ArrayList<>();
            }
            for (String productName : targetRes.keySet()) {
                HashMap<String, String> productValues = targetRes.get(productName);
                Set productKeySet = targetRes.get(productName).keySet();
                Boolean availability = false;
                if (productKeySet.contains("buyURL") && productKeySet.contains("allStoresAvailability")) {
                    if (productValues.get("allStoresAvailability").equals("true")) {
                        availability = true;
                    }
                    Product newProduct = new Product(productName, productValues.get("brand"), "Target", Double.parseDouble(productValues.get("price")), availability, null, productValues.get("imageURL"), productValues.get("buyURL"));
                    products.add(newProduct);
                }
            }
        } else {
            try {
                targetRes = target(searchStr);
            } catch (IOException e) {
                System.out.println(e);
                return new ArrayList<>();
            }
            for (String productName : targetRes.keySet()) {
                HashMap<String, String> productValues = targetRes.get(productName);
                Set productKeySet = targetRes.get(productName).keySet();
                Boolean availability = false;
                if (productKeySet.contains("buyURL") && productKeySet.contains("allStoresAvailability")) {
                    if (productValues.get("allStoresAvailability").equals("true")) {
                        availability = true;
                    }
                    Product newProduct = new Product(productName, productValues.get("brand"), "Target", Double.parseDouble(productValues.get("price")), availability, null, productValues.get("imageURL"), productValues.get("buyURL"));
                    products.add(newProduct);
                }
            }

            try {
                walmartRes = walmart(searchStr);
            } catch (IOException e) {
                System.out.println(e);
                return new ArrayList<>();
            }
            for (String productId : walmartRes.keySet()) {
                HashMap<String, String> productValues = walmartRes.get(productId);
                Boolean availability = false;
                if (productValues.get("availability").equals("true")) {
                    availability = true;
                }
                Product newProduct = new Product(productValues.get("name"), productValues.get("brand"), "Walmart", Double.parseDouble(productValues.get("price")), availability, null, productValues.get("imageURL"), productValues.get("buyURL"));
                products.add(newProduct);
            }
        }

        System.out.println(products);
        return products;
//        return productService.getProducts();
    }

    @PostMapping
    public void registerNewProduct(@RequestBody Product product) {
        productService.addNewProduct(product);
    }

    @DeleteMapping(path = "{productId}")
    public void deleteProduct(@PathVariable("productId") Long productId) {
        productService.deleteProduct(productId);
    }

    @PutMapping(path = "{productId}")
    public void updateProduct(
            @PathVariable("productId") Long productId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Double price,
            @RequestParam(required = false) boolean availability,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) String imageURL) {
        productService.updateProduct(productId, name, price, availability, quantity, imageURL);
    }

    public static HashMap<String, HashMap> target(String query_str) throws IOException {

        HashMap<String, HashMap> productsInfo = new HashMap<>();
        ArrayList<String> productsTCINs = new ArrayList<>();

        // TO GET TITLE, BRAND, IMAGEURL, PRICE INFO
//        String query_param = "baby diapers".replace(" ", "%20");
        // user info
        String query_param = query_str.replace(" ", "+");
        String zip_code = "43065";
        String latitude = "40.142"; // either user location or get store's lat long
        String longitude = "-83.094"; // either user location or get store's lat long
        String state = "OH";

        // write something to fetch this info based on user info from store id table in db
        String store_id = "2851";
        String closest_stores = "2851%2C666%2C1236%2C1978%2C1969"; // ArrayList of 5 closest store IDs, join them with %2C
        String store_name = "Powell".replace(" ", "%20");

        // GET MATCHING PRODUCTS ARRAY
        URL url1 = new URL("https://redsky.target.com/redsky_aggregations/v1/web/plp_search_v1?key=9f36aeafbe60771e321a7cc95a78140772ab3e96&channel=WEB&count=24&default_purchasability_filter=true&include_sponsored=true&keyword=" + query_param + "&offset=0&page=%2Fs%2F" + query_param + "&platform=desktop&pricing_store_id=" + store_id + "&scheduled_delivery_store_id=" + store_id + "&store_ids=" + closest_stores + "&useragent=Mozilla%2F5.0+%28Macintosh%3B+Intel+Mac+OS+X+10_15_7%29+AppleWebKit%2F537.36+%28KHTML%2C+like+Gecko%29+Chrome%2F103.0.0.0+Safari%2F537.36&visitor_id=0182A3D3FCBE0201845B982AB4154957&zip=" + zip_code);
        HttpURLConnection httpConn1 = (HttpURLConnection) url1.openConnection();
        httpConn1.setRequestMethod("GET");

        httpConn1.setRequestProperty("authority", "redsky.target.com");
        httpConn1.setRequestProperty("accept", "application/json");
        httpConn1.setRequestProperty("accept-language", "en-US,en;q=0.9");
        httpConn1.setRequestProperty("cache-control", "no-cache");
        httpConn1.setRequestProperty("cookie", "TealeafAkaSid=tW3sNScU-pf8QSWSBc4Dx8IhHnSIJ3G_; visitorId=0182A3D3FCBE0201845B982AB4154957; sapphire=1; UserLocation=" + zip_code + "|" + latitude + "|" + longitude + "|" + state + "|US; egsSessionId=528e5218-c05f-4434-b220-0970933cd8dd; accessToken=eyJraWQiOiJlYXMyIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiIzMjBmNzg4MC04NGZlLTQ1NWItYWFkYi1mYjg1OGU1NWRhYTgiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA2OTIzNTYsImlhdCI6MTY2MDYwNTk1NiwianRpIjoiVEdULmNjNWUxYzUyNTUwNDRhNTFiY2Y4YzFkZDk2MGNiNDVmLWwiLCJza3kiOiJlYXMyIiwic3V0IjoiRyIsImRpZCI6IjQ4NTY1YWZiYWE2YzFiYjVmNjNkMTQ0ZTdmNjhlODYxMTU5YmQzZTczY2I0MjZjZTFjNTliOTI5MzI3ZDk5NGYiLCJzY28iOiJlY29tLm5vbmUsb3BlbmlkIiwiY2xpIjoiZWNvbS13ZWItMS4wLjAiLCJhc2wiOiJMIn0.QRCI69lR_mSLIsnADDBYTVzlViOX5DNO4d-QNDbExhC0mFmEpn0mUED8L1g-XbB-_RLd8FH3ea2oC6fiEvb-VDOArr23IxklKnwTczlFx-8ukshGWpSrLBLmzr85sk0NO61Gb6wwUNcKfylAfO2ddm6oAa0b_wKehZr4N6oP2QbdKAi__OQXyMftyftLBRAUSvpOM9_zXB1UKysyqKK9nBTyBKi-SjgEsQNweBBwkZwbGBpqMcp7Urt6s8xCB3ciwjOQSG29rzYn_6OCdwvZVusvTS6ND6K2uS7Ut8gBLstl37MDGkXpIALAsD_6sVGyFXjiVogykPhGaWLhuhUuQw; idToken=eyJhbGciOiJub25lIn0.eyJzdWIiOiIzMjBmNzg4MC04NGZlLTQ1NWItYWFkYi1mYjg1OGU1NWRhYTgiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA2OTIzNTYsImlhdCI6MTY2MDYwNTk1NiwiYXNzIjoiTCIsInN1dCI6IkciLCJjbGkiOiJlY29tLXdlYi0xLjAuMCIsInBybyI6eyJmbiI6bnVsbCwiZW0iOm51bGwsInBoIjpmYWxzZSwibGVkIjpudWxsLCJsdHkiOmZhbHNlfX0.; refreshToken=YOQvrhY0Rc1OTlqCUPaQmCAQYYDyJl1hHAp2OyABjfmzyRYUPgVA0XYZ3iY03WMIVq1JWQ3U8UGbLuno1pzIog; __gads=ID=fcacfef16f82cade-22572175a1d400ad:T=1660605956:S=ALNI_MaeY6UxbBLJ7EheMAjpRt-PO6D3Pw; __gpi=UID=000007e4b59ef825:T=1660605956:RT=1660605956:S=ALNI_MbwRsUmwavf2MpuVsBXPiDrT7vBBw; ffsession={%22sessionHash%22:%22a38d7281033411660605955675%22%2C%22prevPageName%22:%22home%20page%22%2C%22prevPageType%22:%22home%20page%22%2C%22prevPageUrl%22:%22https://www.target.com/%22%2C%22sessionHit%22:1}; ci_pixmgr=other; _uetsid=9d41daf01cf111ed8a934fc144aa43e9; _uetvid=9d41d7f01cf111edb1e87102d88101e0; _gcl_au=1.1.949956296.1660605957; fiatsCookie=DSI_" + store_id + "|DSN_" + store_name + "|DSZ_" + zip_code + "; _mitata=ZTJlZmJiYjVkYjUyOGQzZjM0YTM1NmVmZWY4ZTU2YzFiMzlkNzQ1OGZhOGZiNWUyZjc2YzkxNzhhNTkyNjY1ZQ==_/@#/1660606098_/@#/cyk4HguDDTbQp3kn_/@#/Y2EzMWVlZmQwMjA0NWVjOWZhMmRmMTgyMjc0M2MyMDYwZWRmOGMwY2M3ZTdkMzc3MzI3ZDk2ZDI2NDZmMDBkNg==_/@#/000");
        httpConn1.setRequestProperty("origin", "https://www.target.com");
        httpConn1.setRequestProperty("pragma", "no-cache");
        httpConn1.setRequestProperty("referer", "https://www.target.com/s?searchTerm=" + query_param);
        httpConn1.setRequestProperty("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
        httpConn1.setRequestProperty("sec-ch-ua-mobile", "?0");
        httpConn1.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
        httpConn1.setRequestProperty("sec-fetch-dest", "empty");
        httpConn1.setRequestProperty("sec-fetch-mode", "cors");
        httpConn1.setRequestProperty("sec-fetch-site", "same-site");
        httpConn1.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");

        InputStream responseStream1 = httpConn1.getResponseCode() / 100 == 2
                ? httpConn1.getInputStream()
                : httpConn1.getErrorStream();
        Scanner s1 = new Scanner(responseStream1).useDelimiter("\\A");
        String response1 = s1.hasNext() ? s1.next() : "";

        JsonElement jsonElement1 = new JsonParser().parse(response1);
        Gson gson1 = new GsonBuilder().setPrettyPrinting().create();
        String json1 = gson1.toJson(jsonElement1);

//		System.out.println(json1);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonMap1 = objectMapper.readValue(json1, new TypeReference<Map<String, Object>>() {
        });
        Object data1 = jsonMap1.get("data");

        Map<String, Object> dataMap1 = objectMapper.convertValue(data1, Map.class);
        Object search1 = dataMap1.get("search");

        Map<String, Object> searchMap1 = objectMapper.convertValue(search1, Map.class);
        Object products1 = searchMap1.get("products");

        ArrayList<Object> productsAL1 = objectMapper.convertValue(products1, ArrayList.class);
        for (Object product : productsAL1) {
            HashMap<String, String> productInfo = new HashMap<>();

            Map<String, Object> productMap = objectMapper.convertValue(product, Map.class);

            // get title
            Object item = productMap.get("item");
            Map<String, Object> itemMap = objectMapper.convertValue(item, Map.class);
            Object productDescription = itemMap.get("product_description");
            Map<String, Object> productDescriptionMap = objectMapper.convertValue(productDescription, Map.class);
            Object title = productDescriptionMap.get("title");
            String titleStr = objectMapper.convertValue(title, String.class);
            productsInfo.put(titleStr, productInfo);

            // get tcin
            Object productTCIN = productMap.get("tcin");
            String productTCINStr = objectMapper.convertValue(productTCIN, String.class);
            productsTCINs.add(productTCINStr);

            // get brand name
            if (itemMap.get("primary_brand") != null) {
                Object brandInfo = itemMap.get("primary_brand");
                Map<String, Object> brandInfoMap = objectMapper.convertValue(brandInfo, Map.class);
                Object brandName = brandInfoMap.get("name");
                String brandNameStr = objectMapper.convertValue(brandName, String.class);
                productInfo.put("brand", brandNameStr);
            }

            // get price
            Object price = productMap.get("price");
            Map<String, Object> priceMap = objectMapper.convertValue(price, Map.class);
            Object curPrice = priceMap.get("formatted_current_price");
            String curPriceStr = objectMapper.convertValue(curPrice, String.class);
            productInfo.put("price", curPriceStr);

            // get primary image URL
            Object enrichment = itemMap.get("enrichment");
            Map<String, Object> enrichmentMap = objectMapper.convertValue(enrichment, Map.class);
            Object images = enrichmentMap.get("images");
            Map<String, Object> imagesMap = objectMapper.convertValue(images, Map.class);
            Object primaryImage = imagesMap.get("primary_image_url");
            String primaryImageStr = objectMapper.convertValue(primaryImage, String.class);
            productInfo.put("imageURL", primaryImageStr);
        }

        // CONVERT ARRAYLIST OF PRODUCT TCINS INTO STRING
        String productsTCINsStr = String.join("%2C", productsTCINs);

        // TO GET AVAILABILITY INFO AND BUY URL FROM FULFILLMENT DATA
        URL url = new URL("https://redsky.target.com/redsky_aggregations/v1/web_platform/product_summary_with_fulfillment_v1?key=9f36aeafbe60771e321a7cc95a78140772ab3e96&tcins=" + productsTCINsStr + "&store_id" + store_id + "&zip=" + zip_code + "&state=" + state + "&latitude=" + latitude + "&longitude=" + longitude + "&scheduled_delivery_store_id=" + store_id + "&required_store_id=" + store_id + "&has_required_store_id=true&channel=WEB&page=%2Fs%2F" + query_param);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("GET");

        httpConn.setRequestProperty("authority", "redsky.target.com");
        httpConn.setRequestProperty("accept", "application/json");
        httpConn.setRequestProperty("accept-language", "en-US,en;q=0.9");
        httpConn.setRequestProperty("cache-control", "no-cache");
        httpConn.setRequestProperty("cookie", "TealeafAkaSid=tW3sNScU-pf8QSWSBc4Dx8IhHnSIJ3G_; visitorId=0182A3D3FCBE0201845B982AB4154957; sapphire=1; UserLocation=" + zip_code + "|" + latitude + "|" + longitude + "|" + state + "|US; egsSessionId=528e5218-c05f-4434-b220-0970933cd8dd; accessToken=eyJraWQiOiJlYXMyIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiIzMjBmNzg4MC04NGZlLTQ1NWItYWFkYi1mYjg1OGU1NWRhYTgiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA2OTIzNTYsImlhdCI6MTY2MDYwNTk1NiwianRpIjoiVEdULmNjNWUxYzUyNTUwNDRhNTFiY2Y4YzFkZDk2MGNiNDVmLWwiLCJza3kiOiJlYXMyIiwic3V0IjoiRyIsImRpZCI6IjQ4NTY1YWZiYWE2YzFiYjVmNjNkMTQ0ZTdmNjhlODYxMTU5YmQzZTczY2I0MjZjZTFjNTliOTI5MzI3ZDk5NGYiLCJzY28iOiJlY29tLm5vbmUsb3BlbmlkIiwiY2xpIjoiZWNvbS13ZWItMS4wLjAiLCJhc2wiOiJMIn0.QRCI69lR_mSLIsnADDBYTVzlViOX5DNO4d-QNDbExhC0mFmEpn0mUED8L1g-XbB-_RLd8FH3ea2oC6fiEvb-VDOArr23IxklKnwTczlFx-8ukshGWpSrLBLmzr85sk0NO61Gb6wwUNcKfylAfO2ddm6oAa0b_wKehZr4N6oP2QbdKAi__OQXyMftyftLBRAUSvpOM9_zXB1UKysyqKK9nBTyBKi-SjgEsQNweBBwkZwbGBpqMcp7Urt6s8xCB3ciwjOQSG29rzYn_6OCdwvZVusvTS6ND6K2uS7Ut8gBLstl37MDGkXpIALAsD_6sVGyFXjiVogykPhGaWLhuhUuQw; idToken=eyJhbGciOiJub25lIn0.eyJzdWIiOiIzMjBmNzg4MC04NGZlLTQ1NWItYWFkYi1mYjg1OGU1NWRhYTgiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA2OTIzNTYsImlhdCI6MTY2MDYwNTk1NiwiYXNzIjoiTCIsInN1dCI6IkciLCJjbGkiOiJlY29tLXdlYi0xLjAuMCIsInBybyI6eyJmbiI6bnVsbCwiZW0iOm51bGwsInBoIjpmYWxzZSwibGVkIjpudWxsLCJsdHkiOmZhbHNlfX0.; refreshToken=YOQvrhY0Rc1OTlqCUPaQmCAQYYDyJl1hHAp2OyABjfmzyRYUPgVA0XYZ3iY03WMIVq1JWQ3U8UGbLuno1pzIog; __gads=ID=fcacfef16f82cade-22572175a1d400ad:T=1660605956:S=ALNI_MaeY6UxbBLJ7EheMAjpRt-PO6D3Pw; __gpi=UID=000007e4b59ef825:T=1660605956:RT=1660605956:S=ALNI_MbwRsUmwavf2MpuVsBXPiDrT7vBBw; ffsession={%22sessionHash%22:%22a38d7281033411660605955675%22%2C%22prevPageName%22:%22home%20page%22%2C%22prevPageType%22:%22home%20page%22%2C%22prevPageUrl%22:%22https://www.target.com/%22%2C%22sessionHit%22:1}; ci_pixmgr=other; _uetsid=9d41daf01cf111ed8a934fc144aa43e9; _uetvid=9d41d7f01cf111edb1e87102d88101e0; _gcl_au=1.1.949956296.1660605957; fiatsCookie=DSI_" + store_id + "|DSN_" + store_name + "|DSZ_" + zip_code + "; _mitata=ZTJlZmJiYjVkYjUyOGQzZjM0YTM1NmVmZWY4ZTU2YzFiMzlkNzQ1OGZhOGZiNWUyZjc2YzkxNzhhNTkyNjY1ZQ==_/@#/1660606098_/@#/cyk4HguDDTbQp3kn_/@#/Y2EzMWVlZmQwMjA0NWVjOWZhMmRmMTgyMjc0M2MyMDYwZWRmOGMwY2M3ZTdkMzc3MzI3ZDk2ZDI2NDZmMDBkNg==_/@#/000");
        httpConn.setRequestProperty("origin", "https://www.target.com");
        httpConn.setRequestProperty("pragma", "no-cache");
        httpConn.setRequestProperty("referer", "https://www.target.com/s?searchTerm=" + query_param);
        httpConn.setRequestProperty("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
        httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
        httpConn.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
        httpConn.setRequestProperty("sec-fetch-dest", "empty");
        httpConn.setRequestProperty("sec-fetch-mode", "cors");
        httpConn.setRequestProperty("sec-fetch-site", "same-site");
        httpConn.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");

        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";

        JsonElement jsonElement = new JsonParser().parse(response);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(jsonElement);

        Map<String, Object> jsonMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        Object data = jsonMap.get("data");

        Map<String, ArrayList<Object>> dataMap = objectMapper.convertValue(data, Map.class);
        ArrayList<Object> productSummaries = dataMap.get("product_summaries");

        for (Object product : productSummaries) {
            Map<String, Object> productMap = objectMapper.convertValue(product, Map.class);


            Object productItem = productMap.get("item");
            Map<String, Object> productItemMap = objectMapper.convertValue(productItem, Map.class);


            // get title
            Object productDescription = productItemMap.get("product_description");
            Map<String, Object> productDescriptionMap = objectMapper.convertValue(productDescription, Map.class);
            Object productTitle = productDescriptionMap.get("title");
//			System.out.println(productTitle);
            String productTitleStr = objectMapper.convertValue(productTitle, String.class);
            HashMap<String, String> selectedProduct = null;
            if (productsInfo.get(productTitleStr) != null) {
                selectedProduct = productsInfo.get(productTitleStr);
            } else {
                System.out.println("NOT FOUND! CHECK FOR ERRORS!");
                System.out.println(productTitleStr);
                continue;
            }

            // get brand

            // get purchase url
            Object enrichment = productItemMap.get("enrichment");
            Map<String, String> enrichmentMap = objectMapper.convertValue(enrichment, Map.class);
            String buyURL = enrichmentMap.get("buy_url");
            selectedProduct.put("buyURL", buyURL);

            // get image url

            // get price
            Object productPrice = productMap.get("price");
            Map<String, Double> productPriceMap = objectMapper.convertValue(productPrice, Map.class);
            Double productRTP = productPriceMap.get("current_retail");
            selectedProduct.put("price", productRTP.toString());

            // get availability
            Object productFulfillment = productMap.get("fulfillment");
//			System.out.println(productFulfillment);
            Map<String, Object> productFulfillmenteMap = objectMapper.convertValue(productFulfillment, Map.class);
            // all store locations
            boolean productInStock = objectMapper.convertValue(productFulfillmenteMap.get("is_out_of_stock_in_all_store_locations"), boolean.class);
            String inStockAtAllStores = objectMapper.convertValue(!(productInStock), String.class);
            selectedProduct.put("allStoresAvailability", inStockAtAllStores);
//			// primary store location
//			Object storeOptions = productFulfillmenteMap.get("store_options");
//			ArrayList<Object> storeOptionsAL = objectMapper.convertValue(storeOptions, ArrayList.class);
//			Object chosenStoreOption = storeOptionsAL.get(0);
//			Map<String, Object> chosenStoreOptionMap = objectMapper.convertValue(chosenStoreOption, Map.class);
//			Object inStoreOnly = chosenStoreOptionMap.get("in_store_only");
//			Map<String, String> inStoreOnlyMap = objectMapper.convertValue(inStoreOnly, Map.class);
//			String inStoreOnlyStatus = inStoreOnlyMap.get("availability_status");
//			Object orderPickup = chosenStoreOptionMap.get("order_pickup");
//			Map<String, String> orderPickupMap = objectMapper.convertValue(orderPickup, Map.class);
//			String orderPickupStatus = orderPickupMap.get("availability_status");
//			if (inStoreOnlyStatus.equals("IN_STOCK") || orderPickupStatus.equals("IN_STOCK")) {
//				selectedProduct.put("closestStoreAvailability", "true");
//			} else {
//				selectedProduct.put("closestStoreAvailability", "false");
//			}
//			System.out.println("-----------------------------------------------------------------------");
        }

        return productsInfo;
    }

    public static HashMap<String, HashMap> walmart(String query_str) throws IOException {
        HashMap<String, HashMap> products = new HashMap<>();
        String response = "";

        // user info
        String query_param = query_str.replace(" ", "%20");

//        String zip_code = "98104"; // Seattle, WA
//        String store_id = "3098"; // Renton, WA
//
//        String zip_code = "43017"; // Columbus, OH
//        String store_id = "2774"; // Dublin, OH
//
//        String zip_code = "27514"; // Chapel Hill, NC
//        String store_id = "2137"; // Durham, NC
//
//        String zip_code = "60645"; // Chicago, IL
//        String store_id = "4177"; // Lincolnwood, IL
//
        String zip_code = "10027"; // New York, NY
        String store_id = "3795"; // North Bergen, NJ

        // unused params
//        String latitude = "40.142"; // either user location or get store's lat long
//        String longitude = "-83.094"; // either user location or get store's lat long
//        String state = "OH";
//        String closest_stores = "2851%2C666%2C1236%2C1978%2C1969"; // ArrayList of 5 closest store IDs, join them with %2C
//        String store_name = "Powell".replace(" ", "%20");

        // 98104 (Seattle, WA)
        if (store_id.equals("3098")) {
            System.out.println("SEATTLE, WA");
            URL url = new URL("https://www.walmart.com/orchestra/home/graphql/search?query=" + query_param + "&page=1&prg=desktop&sort=best_match&ps=40&searchArgs.query=" + query_param + "&searchArgs.prg=desktop&fitmentFieldParams=true&enablePortableFacets=true&enableFacetCount=true&fetchMarquee=true&fetchSkyline=true&fetchSbaTop=true&tenant=WM_GLASS");
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");

            httpConn.setRequestProperty("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
            httpConn.setRequestProperty("x-o-correlation-id", "9QPBDHJp9vCJXH87kPfwYac6KIV0kqyDvukG");
            httpConn.setRequestProperty("DEVICE_PROFILE_REF_ID", "nEpdnEQXqVs3aLXRx-QqC6mbXqd4u0b0gj79");
            httpConn.setRequestProperty("x-latency-trace", "1");
            httpConn.setRequestProperty("WM_MP", "true");
            httpConn.setRequestProperty("x-o-platform-version", "main-1.13.0-6550cc");
            httpConn.setRequestProperty("x-o-segment", "oaoh");
            httpConn.setRequestProperty("x-o-gql-query", "query Search");
            httpConn.setRequestProperty("WM_PAGE_URL", "https://www.walmart.com/search?q=" + query_param);
            httpConn.setRequestProperty("X-APOLLO-OPERATION-NAME", "Search");
            httpConn.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
            httpConn.setRequestProperty("x-o-bu", "WALMART-US");
            httpConn.setRequestProperty("traceparent", "9QPBDHJp9vCJXH87kPfwYac6KIV0kqyDvukG");
            httpConn.setRequestProperty("x-o-mart", "B2C");
            httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
            httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
            httpConn.setRequestProperty("x-o-platform", "rweb");
            httpConn.setRequestProperty("Content-Type", "application/json");
            httpConn.setRequestProperty("accept", "application/json");
            httpConn.setRequestProperty("Referer", "https://www.walmart.com/search?q=" + query_param);
            httpConn.setRequestProperty("x-enable-server-timing", "1");
            httpConn.setRequestProperty("x-o-ccm", "server");
            httpConn.setRequestProperty("wm_qos.correlation_id", "9QPBDHJp9vCJXH87kPfwYac6KIV0kqyDvukG");

            httpConn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
            writer.write("{\"query\":\"query Search( $query:String $page:Int $prg:Prg! $facet:String $sort:Sort = best_match $catId:String $max_price:String $min_price:String $spelling:Boolean = true $affinityOverride:AffinityOverride $storeSlotBooked:String $ps:Int $ptss:String $recall_set:String $fitmentFieldParams:JSON ={}$fitmentSearchParams:JSON ={}$fetchMarquee:Boolean! $trsp:String $fetchSkyline:Boolean! $fetchSbaTop:Boolean! $additionalQueryParams:JSON ={}$searchArgs:SearchArgumentsForCLS $enablePortableFacets:Boolean = false $intentSource:IntentSource $tenant:String! $enableFacetCount:Boolean = true $pageType:String! = \\\"SearchPage\\\" ){search( query:$query page:$page prg:$prg facet:$facet sort:$sort cat_id:$catId max_price:$max_price min_price:$min_price spelling:$spelling affinityOverride:$affinityOverride storeSlotBooked:$storeSlotBooked ps:$ps ptss:$ptss recall_set:$recall_set trsp:$trsp intentSource:$intentSource additionalQueryParams:$additionalQueryParams pageType:$pageType ){query searchResult{...SearchResultFragment}}contentLayout( channel:\\\"WWW\\\" pageType:$pageType tenant:$tenant searchArgs:$searchArgs ){modules{...ModuleFragment configs{...SearchNonItemFragment __typename...on TempoWM_GLASSWWWSponsoredProductCarouselConfigs{_rawConfigs}...on _TempoWM_GLASSWWWSearchSortFilterModuleConfigs{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}}...on _TempoWM_GLASSWWWSearchGuidedNavModuleConfigs{guidedNavigation{...GuidedNavFragment}}...on TempoWM_GLASSWWWPillsModuleConfigs{moduleSource pillsV2{...PillsModuleFragment}}...TileTakeOverProductFragment...on TempoWM_GLASSWWWSearchFitmentModuleConfigs{fitments( fitmentSearchParams:$fitmentSearchParams fitmentFieldParams:$fitmentFieldParams ){...FitmentFragment sisFitmentResponse{...SearchResultFragment}}}...on TempoWM_GLASSWWWStoreSelectionHeaderConfigs{fulfillmentMethodLabel storeDislayName}...BrandAmplifierAdConfigs @include(if:$fetchSbaTop)...BannerModuleFragment...MarqueeDisplayAdConfigsFragment @include(if:$fetchMarquee)...SkylineDisplayAdConfigsFragment @include(if:$fetchSkyline)...HorizontalChipModuleConfigsFragment...SkinnyBannerFragment}}...LayoutFragment pageMetadata{location{pickupStore deliveryStore intent postalCode stateOrProvinceCode city storeId accessPointId accessType spokeNodeId}pageContext}}}fragment SearchResultFragment on SearchInterface{title aggregatedCount...BreadCrumbFragment...DebugFragment...ItemStacksFragment...PageMetaDataFragment...PaginationFragment...SpellingFragment...SpanishTranslationFragment...RequestContextFragment...ErrorResponse modules{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}guidedNavigation{...GuidedNavFragment}guidedNavigationV2{...PillsModuleFragment}pills{...PillsModuleFragment}spellCheck{title subTitle urlLinkText url}}}fragment ModuleFragment on TempoModule{name version type moduleId schedule{priority}matchedTrigger{zone}}fragment LayoutFragment on ContentLayout{layouts{id layout}}fragment BreadCrumbFragment on SearchInterface{breadCrumb{id name url}}fragment DebugFragment on SearchInterface{debug{sisUrl adsUrl}}fragment ItemStacksFragment on SearchInterface{itemStacks{displayMessage meta{adsBeacon{adUuid moduleInfo max_ads}query stackId stackType title layoutEnum totalItemCount totalItemCountDisplay viewAllParams{query cat_id sort facet affinityOverride recall_set min_price max_price}}itemsV2{...ItemFragment...InGridMarqueeAdFragment...TileTakeOverTileFragment}}}fragment ItemFragment on Product{__typename id usItemId fitmentLabel name checkStoreAvailabilityATC seeShippingEligibility brand type shortDescription weightIncrement imageInfo{...ProductImageInfoFragment}canonicalUrl externalInfo{url}itemType category{path{name url}}badges{flags{...on BaseBadge{key text type id}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{...on BaseBadge{key text type}}}classType averageRating numberOfReviews esrb mediaRating salesUnitType sellerId sellerName hasSellerBadge availabilityStatusV2{display value}groupMetaData{groupType groupSubType numberOfComponents groupComponents{quantity offerId componentType productDisplayName}}productLocation{displayValue aisle{zone aisle}}fulfillmentSpeed offerId preOrder{...PreorderFragment}priceInfo{...ProductPriceInfoFragment}variantCriteria{...VariantCriteriaFragment}snapEligible fulfillmentBadge fulfillmentTitle fulfillmentType brand manufacturerName showAtc sponsoredProduct{spQs clickBeacon spTags viewBeacon}showOptions showBuyNow rewards{eligible state minQuantity rewardAmt promotionId selectionToken cbOffer term expiry description}}fragment ProductImageInfoFragment on ProductImageInfo{thumbnailUrl size}fragment ProductPriceInfoFragment on ProductPriceInfo{priceRange{minPrice maxPrice}currentPrice{...ProductPriceFragment}comparisonPrice{...ProductPriceFragment}wasPrice{...ProductPriceFragment}unitPrice{...ProductPriceFragment}listPrice{...ProductPriceFragment}savingsAmount{...ProductSavingsFragment}shipPrice{...ProductPriceFragment}subscriptionPrice{priceString subscriptionString}priceDisplayCodes{priceDisplayCondition finalCostByWeight submapType}}fragment PreorderFragment on PreOrder{isPreOrder preOrderMessage preOrderStreetDateMessage}fragment ProductPriceFragment on ProductPrice{price priceString variantPriceString priceType currencyUnit priceDisplay}fragment ProductSavingsFragment on ProductSavings{amount percent priceString}fragment VariantCriteriaFragment on VariantCriterion{name type id isVariantTypeSwatch variantList{id images name rank swatchImageUrl availabilityStatus products selectedProduct{canonicalUrl usItemId}}}fragment InGridMarqueeAdFragment on MarqueePlaceholder{__typename type moduleLocation lazy}fragment TileTakeOverTileFragment on TileTakeOverProductPlaceholder{__typename type tileTakeOverTile{span title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}fragment PageMetaDataFragment on SearchInterface{pageMetadata{storeSelectionHeader{fulfillmentMethodLabel storeDislayName}title canonical description location{addressId}}}fragment PaginationFragment on SearchInterface{paginationV2{maxPage pageProperties}}fragment SpanishTranslationFragment on SearchInterface{translation{metadata{originalQuery translatedQuery isTranslated translationOfferType moduleSource}translationModule{title urlLinkText originalQueryUrl}}}fragment SpellingFragment on SearchInterface{spelling{correctedTerm}}fragment RequestContextFragment on SearchInterface{requestContext{vertical isFitmentFilterQueryApplied searchMatchType categories{id name}}}fragment ErrorResponse on SearchInterface{errorResponse{correlationId source errorCodes errors{errorType statusCode statusMsg source}}}fragment GuidedNavFragment on GuidedNavigationSearchInterface{title url}fragment PillsModuleFragment on PillsSearchInterface{title url image:imageV1{src alt}}fragment BannerModuleFragment on TempoWM_GLASSWWWSearchBannerConfigs{moduleType viewConfig{title image imageAlt displayName description url urlAlt appStoreLink appStoreLinkAlt playStoreLink playStoreLinkAlt}}fragment FacetFragment on Facet{title name type layout min max selectedMin selectedMax unboundedMax stepSize isSelected values{id title name description type itemCount @include(if:$enableFacetCount) isSelected baseSeoURL}}fragment FitmentFragment on Fitments{partTypeIDs result{status formId position quantityTitle extendedAttributes{...FitmentFieldFragment}labels{...LabelFragment}resultSubTitle notes suggestions{...FitmentSuggestionFragment}}labels{...LabelFragment}savedVehicle{vehicleType{...VehicleFieldFragment}vehicleYear{...VehicleFieldFragment}vehicleMake{...VehicleFieldFragment}vehicleModel{...VehicleFieldFragment}additionalAttributes{...VehicleFieldFragment}}fitmentFields{...VehicleFieldFragment}fitmentForms{id fields{...FitmentFieldFragment}title labels{...LabelFragment}}}fragment LabelFragment on FitmentLabels{ctas{...FitmentLabelEntityFragment}messages{...FitmentLabelEntityFragment}links{...FitmentLabelEntityFragment}images{...FitmentLabelEntityFragment}}fragment FitmentLabelEntityFragment on FitmentLabelEntity{id label}fragment VehicleFieldFragment on FitmentVehicleField{id label value}fragment FitmentFieldFragment on FitmentField{id displayName value extended data{value label}dependsOn}fragment FitmentSuggestionFragment on FitmentSuggestion{id position loadIndex speedRating searchQueryParam labels{...LabelFragment}cat_id fitmentSuggestionParams{id value}}fragment MarqueeDisplayAdConfigsFragment on TempoWM_GLASSWWWMarqueeDisplayAdConfigs{_rawConfigs ad{...DisplayAdFragment}}fragment DisplayAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataDisplayAdFragment}}}fragment AdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment AdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment SkylineDisplayAdConfigsFragment on TempoWM_GLASSWWWSkylineDisplayAdConfigs{_rawConfigs ad{...SkylineDisplayAdFragment}}fragment SkylineDisplayAdFragment on Ad{...SkylineAdFragment adContent{type data{__typename...SkylineAdDataDisplayAdFragment}}}fragment SkylineAdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment SkylineAdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment BrandAmplifierAdConfigs on TempoWM_GLASSWWWBrandAmplifierAdConfigs{_rawConfigs moduleLocation ad{...SponsoredBrandsAdFragment}}fragment SponsoredBrandsAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataSponsoredBrandsFragment}}}fragment AdDataSponsoredBrandsFragment on AdData{...on SponsoredBrands{adUuid adExpInfo moduleInfo brands{logo{featuredHeadline featuredImage featuredImageName featuredUrl logoClickTrackUrl}products{...ProductFragment}}}}fragment ProductFragment on Product{usItemId offerId badges{flags{__typename...on BaseBadge{id text key query type}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought criteria{name value}}}labels{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{__typename...on BaseBadge{id text key}}}priceInfo{priceDisplayCodes{rollback reducedPrice eligibleForAssociateDiscount clearance strikethrough submapType priceDisplayCondition unitOfMeasure pricePerUnitUom}currentPrice{price priceString}wasPrice{price priceString}priceRange{minPrice maxPrice priceString}unitPrice{price priceString}}showOptions sponsoredProduct{spQs clickBeacon spTags}canonicalUrl numberOfReviews averageRating availabilityStatus imageInfo{thumbnailUrl allImages{id url}}name fulfillmentBadge classType type showAtc p13nData{predictedQuantity flags{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}labels{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}}}fragment SearchNonItemFragment on TempoWM_GLASSWWWSearchNonItemConfigs{title subTitle urlLinkText url}fragment HorizontalChipModuleConfigsFragment on TempoWM_GLASSWWWHorizontalChipModuleConfigs{chipModuleSource:moduleSource chipModule{title url{linkText title clickThrough{type value}}}chipModuleWithImages{title url{linkText title clickThrough{type value}}image{alt clickThrough{type value}height src title width}}}fragment SkinnyBannerFragment on TempoWM_GLASSWWWSkinnyBannerConfigs{bannerType desktopBannerHeight bannerImage{src title alt}mobileBannerHeight mobileImage{src title alt}clickThroughUrl{clickThrough{value}}backgroundColor heading{title fontColor}subHeading{title fontColor}bannerCta{ctaLink{linkText clickThrough{value}}textColor ctaType}}fragment TileTakeOverProductFragment on TempoWM_GLASSWWWTileTakeOverProductConfigs{dwebSlots mwebSlots TileTakeOverProductDetails{pageNumber span dwebPosition mwebPosition title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}\",\"variables\":{\"id\":\"\",\"dealsId\":\"\",\"query\":\"" + query_param + "\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"" + query_param + "\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"fitmentFieldParams\":{\"powerSportEnabled\":true},\"fitmentSearchParams\":{\"id\":\"\",\"dealsId\":\"\",\"query\":\"" + query_param + "\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"" + query_param + "\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"cat_id\":\"\",\"_be_shelf_id\":\"\"},\"enablePortableFacets\":true,\"enableFacetCount\":true,\"fetchMarquee\":true,\"fetchSkyline\":true,\"fetchSbaTop\":true,\"tenant\":\"WM_GLASS\",\"pageType\":\"SearchPage\"}}");
            writer.flush();
            writer.close();
            httpConn.getOutputStream().close();

            InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                    ? httpConn.getInputStream()
                    : httpConn.getErrorStream();
            Scanner s = new Scanner(responseStream).useDelimiter("\\A");
            response = s.hasNext() ? s.next() : "";
            // 10027 (New York, NY)
        } else if (store_id.equals("3795")) {
            System.out.println("NORTH BERGEN, NJ");
            URL url = new URL("https://www.walmart.com/orchestra/home/graphql/search?query=" + query_param + "&page=1&prg=desktop&sort=best_match&ps=40&searchArgs.query=" + query_param + "&searchArgs.prg=desktop&fitmentFieldParams=true&enablePortableFacets=true&enableFacetCount=true&fetchMarquee=true&fetchSkyline=true&fetchSbaTop=true&tenant=WM_GLASS");
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");

            httpConn.setRequestProperty("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
            httpConn.setRequestProperty("x-o-correlation-id", "wTkxfKeZPeyVpMcUmtCeLWI_WNaHBU3Bh2UK");
            httpConn.setRequestProperty("DEVICE_PROFILE_REF_ID", "nEpdnEQXqVs3aLXRx-QqC6mbXqd4u0b0gj79");
            httpConn.setRequestProperty("x-latency-trace", "1");
            httpConn.setRequestProperty("WM_MP", "true");
            httpConn.setRequestProperty("x-o-platform-version", "main-1.13.0-6550cc");
            httpConn.setRequestProperty("x-o-segment", "oaoh");
            httpConn.setRequestProperty("x-o-gql-query", "query Search");
            httpConn.setRequestProperty("WM_PAGE_URL", "https://www.walmart.com/search?q=" + query_param);
            httpConn.setRequestProperty("X-APOLLO-OPERATION-NAME", "Search");
            httpConn.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
            httpConn.setRequestProperty("x-o-bu", "WALMART-US");
            httpConn.setRequestProperty("traceparent", "wTkxfKeZPeyVpMcUmtCeLWI_WNaHBU3Bh2UK");
            httpConn.setRequestProperty("x-o-mart", "B2C");
            httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
            httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
            httpConn.setRequestProperty("x-o-platform", "rweb");
            httpConn.setRequestProperty("Content-Type", "application/json");
            httpConn.setRequestProperty("accept", "application/json");
            httpConn.setRequestProperty("Referer", "https://www.walmart.com/search?q=" + query_param);
            httpConn.setRequestProperty("x-enable-server-timing", "1");
            httpConn.setRequestProperty("x-o-ccm", "server");
            httpConn.setRequestProperty("wm_qos.correlation_id", "wTkxfKeZPeyVpMcUmtCeLWI_WNaHBU3Bh2UK");

            httpConn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
            writer.write("{\"query\":\"query Search( $query:String $page:Int $prg:Prg! $facet:String $sort:Sort = best_match $catId:String $max_price:String $min_price:String $spelling:Boolean = true $affinityOverride:AffinityOverride $storeSlotBooked:String $ps:Int $ptss:String $recall_set:String $fitmentFieldParams:JSON ={}$fitmentSearchParams:JSON ={}$fetchMarquee:Boolean! $trsp:String $fetchSkyline:Boolean! $fetchSbaTop:Boolean! $additionalQueryParams:JSON ={}$searchArgs:SearchArgumentsForCLS $enablePortableFacets:Boolean = false $intentSource:IntentSource $tenant:String! $enableFacetCount:Boolean = true $pageType:String! = \\\"SearchPage\\\" ){search( query:$query page:$page prg:$prg facet:$facet sort:$sort cat_id:$catId max_price:$max_price min_price:$min_price spelling:$spelling affinityOverride:$affinityOverride storeSlotBooked:$storeSlotBooked ps:$ps ptss:$ptss recall_set:$recall_set trsp:$trsp intentSource:$intentSource additionalQueryParams:$additionalQueryParams pageType:$pageType ){query searchResult{...SearchResultFragment}}contentLayout( channel:\\\"WWW\\\" pageType:$pageType tenant:$tenant searchArgs:$searchArgs ){modules{...ModuleFragment configs{...SearchNonItemFragment __typename...on TempoWM_GLASSWWWSponsoredProductCarouselConfigs{_rawConfigs}...on _TempoWM_GLASSWWWSearchSortFilterModuleConfigs{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}}...on _TempoWM_GLASSWWWSearchGuidedNavModuleConfigs{guidedNavigation{...GuidedNavFragment}}...on TempoWM_GLASSWWWPillsModuleConfigs{moduleSource pillsV2{...PillsModuleFragment}}...TileTakeOverProductFragment...on TempoWM_GLASSWWWSearchFitmentModuleConfigs{fitments( fitmentSearchParams:$fitmentSearchParams fitmentFieldParams:$fitmentFieldParams ){...FitmentFragment sisFitmentResponse{...SearchResultFragment}}}...on TempoWM_GLASSWWWStoreSelectionHeaderConfigs{fulfillmentMethodLabel storeDislayName}...BrandAmplifierAdConfigs @include(if:$fetchSbaTop)...BannerModuleFragment...MarqueeDisplayAdConfigsFragment @include(if:$fetchMarquee)...SkylineDisplayAdConfigsFragment @include(if:$fetchSkyline)...HorizontalChipModuleConfigsFragment...SkinnyBannerFragment}}...LayoutFragment pageMetadata{location{pickupStore deliveryStore intent postalCode stateOrProvinceCode city storeId accessPointId accessType spokeNodeId}pageContext}}}fragment SearchResultFragment on SearchInterface{title aggregatedCount...BreadCrumbFragment...DebugFragment...ItemStacksFragment...PageMetaDataFragment...PaginationFragment...SpellingFragment...SpanishTranslationFragment...RequestContextFragment...ErrorResponse modules{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}guidedNavigation{...GuidedNavFragment}guidedNavigationV2{...PillsModuleFragment}pills{...PillsModuleFragment}spellCheck{title subTitle urlLinkText url}}}fragment ModuleFragment on TempoModule{name version type moduleId schedule{priority}matchedTrigger{zone}}fragment LayoutFragment on ContentLayout{layouts{id layout}}fragment BreadCrumbFragment on SearchInterface{breadCrumb{id name url}}fragment DebugFragment on SearchInterface{debug{sisUrl adsUrl}}fragment ItemStacksFragment on SearchInterface{itemStacks{displayMessage meta{adsBeacon{adUuid moduleInfo max_ads}query stackId stackType title layoutEnum totalItemCount totalItemCountDisplay viewAllParams{query cat_id sort facet affinityOverride recall_set min_price max_price}}itemsV2{...ItemFragment...InGridMarqueeAdFragment...TileTakeOverTileFragment}}}fragment ItemFragment on Product{__typename id usItemId fitmentLabel name checkStoreAvailabilityATC seeShippingEligibility brand type shortDescription weightIncrement imageInfo{...ProductImageInfoFragment}canonicalUrl externalInfo{url}itemType category{path{name url}}badges{flags{...on BaseBadge{key text type id}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{...on BaseBadge{key text type}}}classType averageRating numberOfReviews esrb mediaRating salesUnitType sellerId sellerName hasSellerBadge availabilityStatusV2{display value}groupMetaData{groupType groupSubType numberOfComponents groupComponents{quantity offerId componentType productDisplayName}}productLocation{displayValue aisle{zone aisle}}fulfillmentSpeed offerId preOrder{...PreorderFragment}priceInfo{...ProductPriceInfoFragment}variantCriteria{...VariantCriteriaFragment}snapEligible fulfillmentBadge fulfillmentTitle fulfillmentType brand manufacturerName showAtc sponsoredProduct{spQs clickBeacon spTags viewBeacon}showOptions showBuyNow rewards{eligible state minQuantity rewardAmt promotionId selectionToken cbOffer term expiry description}}fragment ProductImageInfoFragment on ProductImageInfo{thumbnailUrl size}fragment ProductPriceInfoFragment on ProductPriceInfo{priceRange{minPrice maxPrice}currentPrice{...ProductPriceFragment}comparisonPrice{...ProductPriceFragment}wasPrice{...ProductPriceFragment}unitPrice{...ProductPriceFragment}listPrice{...ProductPriceFragment}savingsAmount{...ProductSavingsFragment}shipPrice{...ProductPriceFragment}subscriptionPrice{priceString subscriptionString}priceDisplayCodes{priceDisplayCondition finalCostByWeight submapType}}fragment PreorderFragment on PreOrder{isPreOrder preOrderMessage preOrderStreetDateMessage}fragment ProductPriceFragment on ProductPrice{price priceString variantPriceString priceType currencyUnit priceDisplay}fragment ProductSavingsFragment on ProductSavings{amount percent priceString}fragment VariantCriteriaFragment on VariantCriterion{name type id isVariantTypeSwatch variantList{id images name rank swatchImageUrl availabilityStatus products selectedProduct{canonicalUrl usItemId}}}fragment InGridMarqueeAdFragment on MarqueePlaceholder{__typename type moduleLocation lazy}fragment TileTakeOverTileFragment on TileTakeOverProductPlaceholder{__typename type tileTakeOverTile{span title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}fragment PageMetaDataFragment on SearchInterface{pageMetadata{storeSelectionHeader{fulfillmentMethodLabel storeDislayName}title canonical description location{addressId}}}fragment PaginationFragment on SearchInterface{paginationV2{maxPage pageProperties}}fragment SpanishTranslationFragment on SearchInterface{translation{metadata{originalQuery translatedQuery isTranslated translationOfferType moduleSource}translationModule{title urlLinkText originalQueryUrl}}}fragment SpellingFragment on SearchInterface{spelling{correctedTerm}}fragment RequestContextFragment on SearchInterface{requestContext{vertical isFitmentFilterQueryApplied searchMatchType categories{id name}}}fragment ErrorResponse on SearchInterface{errorResponse{correlationId source errorCodes errors{errorType statusCode statusMsg source}}}fragment GuidedNavFragment on GuidedNavigationSearchInterface{title url}fragment PillsModuleFragment on PillsSearchInterface{title url image:imageV1{src alt}}fragment BannerModuleFragment on TempoWM_GLASSWWWSearchBannerConfigs{moduleType viewConfig{title image imageAlt displayName description url urlAlt appStoreLink appStoreLinkAlt playStoreLink playStoreLinkAlt}}fragment FacetFragment on Facet{title name type layout min max selectedMin selectedMax unboundedMax stepSize isSelected values{id title name description type itemCount @include(if:$enableFacetCount) isSelected baseSeoURL}}fragment FitmentFragment on Fitments{partTypeIDs result{status formId position quantityTitle extendedAttributes{...FitmentFieldFragment}labels{...LabelFragment}resultSubTitle notes suggestions{...FitmentSuggestionFragment}}labels{...LabelFragment}savedVehicle{vehicleType{...VehicleFieldFragment}vehicleYear{...VehicleFieldFragment}vehicleMake{...VehicleFieldFragment}vehicleModel{...VehicleFieldFragment}additionalAttributes{...VehicleFieldFragment}}fitmentFields{...VehicleFieldFragment}fitmentForms{id fields{...FitmentFieldFragment}title labels{...LabelFragment}}}fragment LabelFragment on FitmentLabels{ctas{...FitmentLabelEntityFragment}messages{...FitmentLabelEntityFragment}links{...FitmentLabelEntityFragment}images{...FitmentLabelEntityFragment}}fragment FitmentLabelEntityFragment on FitmentLabelEntity{id label}fragment VehicleFieldFragment on FitmentVehicleField{id label value}fragment FitmentFieldFragment on FitmentField{id displayName value extended data{value label}dependsOn}fragment FitmentSuggestionFragment on FitmentSuggestion{id position loadIndex speedRating searchQueryParam labels{...LabelFragment}cat_id fitmentSuggestionParams{id value}}fragment MarqueeDisplayAdConfigsFragment on TempoWM_GLASSWWWMarqueeDisplayAdConfigs{_rawConfigs ad{...DisplayAdFragment}}fragment DisplayAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataDisplayAdFragment}}}fragment AdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment AdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment SkylineDisplayAdConfigsFragment on TempoWM_GLASSWWWSkylineDisplayAdConfigs{_rawConfigs ad{...SkylineDisplayAdFragment}}fragment SkylineDisplayAdFragment on Ad{...SkylineAdFragment adContent{type data{__typename...SkylineAdDataDisplayAdFragment}}}fragment SkylineAdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment SkylineAdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment BrandAmplifierAdConfigs on TempoWM_GLASSWWWBrandAmplifierAdConfigs{_rawConfigs moduleLocation ad{...SponsoredBrandsAdFragment}}fragment SponsoredBrandsAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataSponsoredBrandsFragment}}}fragment AdDataSponsoredBrandsFragment on AdData{...on SponsoredBrands{adUuid adExpInfo moduleInfo brands{logo{featuredHeadline featuredImage featuredImageName featuredUrl logoClickTrackUrl}products{...ProductFragment}}}}fragment ProductFragment on Product{usItemId offerId badges{flags{__typename...on BaseBadge{id text key query type}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought criteria{name value}}}labels{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{__typename...on BaseBadge{id text key}}}priceInfo{priceDisplayCodes{rollback reducedPrice eligibleForAssociateDiscount clearance strikethrough submapType priceDisplayCondition unitOfMeasure pricePerUnitUom}currentPrice{price priceString}wasPrice{price priceString}priceRange{minPrice maxPrice priceString}unitPrice{price priceString}}showOptions sponsoredProduct{spQs clickBeacon spTags}canonicalUrl numberOfReviews averageRating availabilityStatus imageInfo{thumbnailUrl allImages{id url}}name fulfillmentBadge classType type showAtc p13nData{predictedQuantity flags{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}labels{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}}}fragment SearchNonItemFragment on TempoWM_GLASSWWWSearchNonItemConfigs{title subTitle urlLinkText url}fragment HorizontalChipModuleConfigsFragment on TempoWM_GLASSWWWHorizontalChipModuleConfigs{chipModuleSource:moduleSource chipModule{title url{linkText title clickThrough{type value}}}chipModuleWithImages{title url{linkText title clickThrough{type value}}image{alt clickThrough{type value}height src title width}}}fragment SkinnyBannerFragment on TempoWM_GLASSWWWSkinnyBannerConfigs{bannerType desktopBannerHeight bannerImage{src title alt}mobileBannerHeight mobileImage{src title alt}clickThroughUrl{clickThrough{value}}backgroundColor heading{title fontColor}subHeading{title fontColor}bannerCta{ctaLink{linkText clickThrough{value}}textColor ctaType}}fragment TileTakeOverProductFragment on TempoWM_GLASSWWWTileTakeOverProductConfigs{dwebSlots mwebSlots TileTakeOverProductDetails{pageNumber span dwebPosition mwebPosition title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}\",\"variables\":{\"id\":\"\",\"dealsId\":\"\",\"query\":\"" + query_param + "\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"" + query_param + "\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"fitmentFieldParams\":{\"powerSportEnabled\":true},\"fitmentSearchParams\":{\"id\":\"\",\"dealsId\":\"\",\"query\":\"" + query_param + "\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"" + query_param + "\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"cat_id\":\"\",\"_be_shelf_id\":\"\"},\"enablePortableFacets\":true,\"enableFacetCount\":true,\"fetchMarquee\":true,\"fetchSkyline\":true,\"fetchSbaTop\":true,\"tenant\":\"WM_GLASS\",\"pageType\":\"SearchPage\"}}");
            writer.flush();
            writer.close();
            httpConn.getOutputStream().close();

            InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                    ? httpConn.getInputStream()
                    : httpConn.getErrorStream();
            Scanner s = new Scanner(responseStream).useDelimiter("\\A");
            response = s.hasNext() ? s.next() : "";
            // 43017 (Columbus, OH)
        } else if (store_id.equals("2774")) {
            System.out.println("COLUMBUS, OH");
            URL url = new URL("https://www.walmart.com/orchestra/home/graphql/search?query=" + query_param + "&page=1&prg=desktop&sort=best_match&ps=40&searchArgs.query=" + query_param + "&searchArgs.prg=desktop&fitmentFieldParams=true&enablePortableFacets=true&enableFacetCount=true&fetchMarquee=true&fetchSkyline=true&fetchSbaTop=true&tenant=WM_GLASS");
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");

            httpConn.setRequestProperty("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
            httpConn.setRequestProperty("x-o-correlation-id", "uBf0tlZgkJUhPrRP2vGARE_5m1poB5fhSzK9");
            httpConn.setRequestProperty("DEVICE_PROFILE_REF_ID", "nEpdnEQXqVs3aLXRx-QqC6mbXqd4u0b0gj79");
            httpConn.setRequestProperty("x-latency-trace", "1");
            httpConn.setRequestProperty("WM_MP", "true");
            httpConn.setRequestProperty("x-o-platform-version", "main-1.13.0-6550cc");
            httpConn.setRequestProperty("x-o-segment", "oaoh");
            httpConn.setRequestProperty("x-o-gql-query", "query Search");
            httpConn.setRequestProperty("WM_PAGE_URL", "https://www.walmart.com/search?q=" + query_param);
            httpConn.setRequestProperty("X-APOLLO-OPERATION-NAME", "Search");
            httpConn.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
            httpConn.setRequestProperty("x-o-bu", "WALMART-US");
            httpConn.setRequestProperty("traceparent", "uBf0tlZgkJUhPrRP2vGARE_5m1poB5fhSzK9");
            httpConn.setRequestProperty("x-o-mart", "B2C");
            httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
            httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
            httpConn.setRequestProperty("x-o-platform", "rweb");
            httpConn.setRequestProperty("Content-Type", "application/json");
            httpConn.setRequestProperty("accept", "application/json");
            httpConn.setRequestProperty("Referer", "https://www.walmart.com/search?q=" + query_param);
            httpConn.setRequestProperty("x-enable-server-timing", "1");
            httpConn.setRequestProperty("x-o-ccm", "server");
            httpConn.setRequestProperty("wm_qos.correlation_id", "uBf0tlZgkJUhPrRP2vGARE_5m1poB5fhSzK9");

            httpConn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
            writer.write("{\"query\":\"query Search( $query:String $page:Int $prg:Prg! $facet:String $sort:Sort = best_match $catId:String $max_price:String $min_price:String $spelling:Boolean = true $affinityOverride:AffinityOverride $storeSlotBooked:String $ps:Int $ptss:String $recall_set:String $fitmentFieldParams:JSON ={}$fitmentSearchParams:JSON ={}$fetchMarquee:Boolean! $trsp:String $fetchSkyline:Boolean! $fetchSbaTop:Boolean! $additionalQueryParams:JSON ={}$searchArgs:SearchArgumentsForCLS $enablePortableFacets:Boolean = false $intentSource:IntentSource $tenant:String! $enableFacetCount:Boolean = true $pageType:String! = \\\"SearchPage\\\" ){search( query:$query page:$page prg:$prg facet:$facet sort:$sort cat_id:$catId max_price:$max_price min_price:$min_price spelling:$spelling affinityOverride:$affinityOverride storeSlotBooked:$storeSlotBooked ps:$ps ptss:$ptss recall_set:$recall_set trsp:$trsp intentSource:$intentSource additionalQueryParams:$additionalQueryParams pageType:$pageType ){query searchResult{...SearchResultFragment}}contentLayout( channel:\\\"WWW\\\" pageType:$pageType tenant:$tenant searchArgs:$searchArgs ){modules{...ModuleFragment configs{...SearchNonItemFragment __typename...on TempoWM_GLASSWWWSponsoredProductCarouselConfigs{_rawConfigs}...on _TempoWM_GLASSWWWSearchSortFilterModuleConfigs{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}}...on _TempoWM_GLASSWWWSearchGuidedNavModuleConfigs{guidedNavigation{...GuidedNavFragment}}...on TempoWM_GLASSWWWPillsModuleConfigs{moduleSource pillsV2{...PillsModuleFragment}}...TileTakeOverProductFragment...on TempoWM_GLASSWWWSearchFitmentModuleConfigs{fitments( fitmentSearchParams:$fitmentSearchParams fitmentFieldParams:$fitmentFieldParams ){...FitmentFragment sisFitmentResponse{...SearchResultFragment}}}...on TempoWM_GLASSWWWStoreSelectionHeaderConfigs{fulfillmentMethodLabel storeDislayName}...BrandAmplifierAdConfigs @include(if:$fetchSbaTop)...BannerModuleFragment...MarqueeDisplayAdConfigsFragment @include(if:$fetchMarquee)...SkylineDisplayAdConfigsFragment @include(if:$fetchSkyline)...HorizontalChipModuleConfigsFragment...SkinnyBannerFragment}}...LayoutFragment pageMetadata{location{pickupStore deliveryStore intent postalCode stateOrProvinceCode city storeId accessPointId accessType spokeNodeId}pageContext}}}fragment SearchResultFragment on SearchInterface{title aggregatedCount...BreadCrumbFragment...DebugFragment...ItemStacksFragment...PageMetaDataFragment...PaginationFragment...SpellingFragment...SpanishTranslationFragment...RequestContextFragment...ErrorResponse modules{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}guidedNavigation{...GuidedNavFragment}guidedNavigationV2{...PillsModuleFragment}pills{...PillsModuleFragment}spellCheck{title subTitle urlLinkText url}}}fragment ModuleFragment on TempoModule{name version type moduleId schedule{priority}matchedTrigger{zone}}fragment LayoutFragment on ContentLayout{layouts{id layout}}fragment BreadCrumbFragment on SearchInterface{breadCrumb{id name url}}fragment DebugFragment on SearchInterface{debug{sisUrl adsUrl}}fragment ItemStacksFragment on SearchInterface{itemStacks{displayMessage meta{adsBeacon{adUuid moduleInfo max_ads}query stackId stackType title layoutEnum totalItemCount totalItemCountDisplay viewAllParams{query cat_id sort facet affinityOverride recall_set min_price max_price}}itemsV2{...ItemFragment...InGridMarqueeAdFragment...TileTakeOverTileFragment}}}fragment ItemFragment on Product{__typename id usItemId fitmentLabel name checkStoreAvailabilityATC seeShippingEligibility brand type shortDescription weightIncrement imageInfo{...ProductImageInfoFragment}canonicalUrl externalInfo{url}itemType category{path{name url}}badges{flags{...on BaseBadge{key text type id}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{...on BaseBadge{key text type}}}classType averageRating numberOfReviews esrb mediaRating salesUnitType sellerId sellerName hasSellerBadge availabilityStatusV2{display value}groupMetaData{groupType groupSubType numberOfComponents groupComponents{quantity offerId componentType productDisplayName}}productLocation{displayValue aisle{zone aisle}}fulfillmentSpeed offerId preOrder{...PreorderFragment}priceInfo{...ProductPriceInfoFragment}variantCriteria{...VariantCriteriaFragment}snapEligible fulfillmentBadge fulfillmentTitle fulfillmentType brand manufacturerName showAtc sponsoredProduct{spQs clickBeacon spTags viewBeacon}showOptions showBuyNow rewards{eligible state minQuantity rewardAmt promotionId selectionToken cbOffer term expiry description}}fragment ProductImageInfoFragment on ProductImageInfo{thumbnailUrl size}fragment ProductPriceInfoFragment on ProductPriceInfo{priceRange{minPrice maxPrice}currentPrice{...ProductPriceFragment}comparisonPrice{...ProductPriceFragment}wasPrice{...ProductPriceFragment}unitPrice{...ProductPriceFragment}listPrice{...ProductPriceFragment}savingsAmount{...ProductSavingsFragment}shipPrice{...ProductPriceFragment}subscriptionPrice{priceString subscriptionString}priceDisplayCodes{priceDisplayCondition finalCostByWeight submapType}}fragment PreorderFragment on PreOrder{isPreOrder preOrderMessage preOrderStreetDateMessage}fragment ProductPriceFragment on ProductPrice{price priceString variantPriceString priceType currencyUnit priceDisplay}fragment ProductSavingsFragment on ProductSavings{amount percent priceString}fragment VariantCriteriaFragment on VariantCriterion{name type id isVariantTypeSwatch variantList{id images name rank swatchImageUrl availabilityStatus products selectedProduct{canonicalUrl usItemId}}}fragment InGridMarqueeAdFragment on MarqueePlaceholder{__typename type moduleLocation lazy}fragment TileTakeOverTileFragment on TileTakeOverProductPlaceholder{__typename type tileTakeOverTile{span title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}fragment PageMetaDataFragment on SearchInterface{pageMetadata{storeSelectionHeader{fulfillmentMethodLabel storeDislayName}title canonical description location{addressId}}}fragment PaginationFragment on SearchInterface{paginationV2{maxPage pageProperties}}fragment SpanishTranslationFragment on SearchInterface{translation{metadata{originalQuery translatedQuery isTranslated translationOfferType moduleSource}translationModule{title urlLinkText originalQueryUrl}}}fragment SpellingFragment on SearchInterface{spelling{correctedTerm}}fragment RequestContextFragment on SearchInterface{requestContext{vertical isFitmentFilterQueryApplied searchMatchType categories{id name}}}fragment ErrorResponse on SearchInterface{errorResponse{correlationId source errorCodes errors{errorType statusCode statusMsg source}}}fragment GuidedNavFragment on GuidedNavigationSearchInterface{title url}fragment PillsModuleFragment on PillsSearchInterface{title url image:imageV1{src alt}}fragment BannerModuleFragment on TempoWM_GLASSWWWSearchBannerConfigs{moduleType viewConfig{title image imageAlt displayName description url urlAlt appStoreLink appStoreLinkAlt playStoreLink playStoreLinkAlt}}fragment FacetFragment on Facet{title name type layout min max selectedMin selectedMax unboundedMax stepSize isSelected values{id title name description type itemCount @include(if:$enableFacetCount) isSelected baseSeoURL}}fragment FitmentFragment on Fitments{partTypeIDs result{status formId position quantityTitle extendedAttributes{...FitmentFieldFragment}labels{...LabelFragment}resultSubTitle notes suggestions{...FitmentSuggestionFragment}}labels{...LabelFragment}savedVehicle{vehicleType{...VehicleFieldFragment}vehicleYear{...VehicleFieldFragment}vehicleMake{...VehicleFieldFragment}vehicleModel{...VehicleFieldFragment}additionalAttributes{...VehicleFieldFragment}}fitmentFields{...VehicleFieldFragment}fitmentForms{id fields{...FitmentFieldFragment}title labels{...LabelFragment}}}fragment LabelFragment on FitmentLabels{ctas{...FitmentLabelEntityFragment}messages{...FitmentLabelEntityFragment}links{...FitmentLabelEntityFragment}images{...FitmentLabelEntityFragment}}fragment FitmentLabelEntityFragment on FitmentLabelEntity{id label}fragment VehicleFieldFragment on FitmentVehicleField{id label value}fragment FitmentFieldFragment on FitmentField{id displayName value extended data{value label}dependsOn}fragment FitmentSuggestionFragment on FitmentSuggestion{id position loadIndex speedRating searchQueryParam labels{...LabelFragment}cat_id fitmentSuggestionParams{id value}}fragment MarqueeDisplayAdConfigsFragment on TempoWM_GLASSWWWMarqueeDisplayAdConfigs{_rawConfigs ad{...DisplayAdFragment}}fragment DisplayAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataDisplayAdFragment}}}fragment AdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment AdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment SkylineDisplayAdConfigsFragment on TempoWM_GLASSWWWSkylineDisplayAdConfigs{_rawConfigs ad{...SkylineDisplayAdFragment}}fragment SkylineDisplayAdFragment on Ad{...SkylineAdFragment adContent{type data{__typename...SkylineAdDataDisplayAdFragment}}}fragment SkylineAdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment SkylineAdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment BrandAmplifierAdConfigs on TempoWM_GLASSWWWBrandAmplifierAdConfigs{_rawConfigs moduleLocation ad{...SponsoredBrandsAdFragment}}fragment SponsoredBrandsAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataSponsoredBrandsFragment}}}fragment AdDataSponsoredBrandsFragment on AdData{...on SponsoredBrands{adUuid adExpInfo moduleInfo brands{logo{featuredHeadline featuredImage featuredImageName featuredUrl logoClickTrackUrl}products{...ProductFragment}}}}fragment ProductFragment on Product{usItemId offerId badges{flags{__typename...on BaseBadge{id text key query type}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought criteria{name value}}}labels{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{__typename...on BaseBadge{id text key}}}priceInfo{priceDisplayCodes{rollback reducedPrice eligibleForAssociateDiscount clearance strikethrough submapType priceDisplayCondition unitOfMeasure pricePerUnitUom}currentPrice{price priceString}wasPrice{price priceString}priceRange{minPrice maxPrice priceString}unitPrice{price priceString}}showOptions sponsoredProduct{spQs clickBeacon spTags}canonicalUrl numberOfReviews averageRating availabilityStatus imageInfo{thumbnailUrl allImages{id url}}name fulfillmentBadge classType type showAtc p13nData{predictedQuantity flags{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}labels{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}}}fragment SearchNonItemFragment on TempoWM_GLASSWWWSearchNonItemConfigs{title subTitle urlLinkText url}fragment HorizontalChipModuleConfigsFragment on TempoWM_GLASSWWWHorizontalChipModuleConfigs{chipModuleSource:moduleSource chipModule{title url{linkText title clickThrough{type value}}}chipModuleWithImages{title url{linkText title clickThrough{type value}}image{alt clickThrough{type value}height src title width}}}fragment SkinnyBannerFragment on TempoWM_GLASSWWWSkinnyBannerConfigs{bannerType desktopBannerHeight bannerImage{src title alt}mobileBannerHeight mobileImage{src title alt}clickThroughUrl{clickThrough{value}}backgroundColor heading{title fontColor}subHeading{title fontColor}bannerCta{ctaLink{linkText clickThrough{value}}textColor ctaType}}fragment TileTakeOverProductFragment on TempoWM_GLASSWWWTileTakeOverProductConfigs{dwebSlots mwebSlots TileTakeOverProductDetails{pageNumber span dwebPosition mwebPosition title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}\",\"variables\":{\"id\":\"\",\"dealsId\":\"\",\"query\":\"" + query_param + "\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"" + query_param + "\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"fitmentFieldParams\":{\"powerSportEnabled\":true},\"fitmentSearchParams\":{\"id\":\"\",\"dealsId\":\"\",\"query\":\"" + query_param + "\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"" + query_param + "\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"cat_id\":\"\",\"_be_shelf_id\":\"\"},\"enablePortableFacets\":true,\"enableFacetCount\":true,\"fetchMarquee\":true,\"fetchSkyline\":true,\"fetchSbaTop\":true,\"tenant\":\"WM_GLASS\",\"pageType\":\"SearchPage\"}}");
            writer.flush();
            writer.close();
            httpConn.getOutputStream().close();

            InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                    ? httpConn.getInputStream()
                    : httpConn.getErrorStream();
            Scanner s = new Scanner(responseStream).useDelimiter("\\A");
            response = s.hasNext() ? s.next() : "";
            // 27514 (Chapel Hill, NC)
        } else if (store_id.equals("2137")) {
            System.out.println("CHAPEL HILL, NC");
            URL url = new URL("https://www.walmart.com/orchestra/home/graphql/search?query=" + query_param + "&page=1&prg=desktop&sort=best_match&ps=40&searchArgs.query=" + query_param + "&searchArgs.prg=desktop&fitmentFieldParams=true&enablePortableFacets=true&enableFacetCount=true&fetchMarquee=true&fetchSkyline=true&fetchSbaTop=true&tenant=WM_GLASS");
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");

            httpConn.setRequestProperty("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
            httpConn.setRequestProperty("x-o-correlation-id", "iP4jdHzhhTzWO5cmKsOpWt-N_3uiFcXe5kJC");
            httpConn.setRequestProperty("DEVICE_PROFILE_REF_ID", "nEpdnEQXqVs3aLXRx-QqC6mbXqd4u0b0gj79");
            httpConn.setRequestProperty("x-latency-trace", "1");
            httpConn.setRequestProperty("WM_MP", "true");
            httpConn.setRequestProperty("x-o-platform-version", "main-1.13.0-6550cc");
            httpConn.setRequestProperty("x-o-segment", "oaoh");
            httpConn.setRequestProperty("x-o-gql-query", "query Search");
            httpConn.setRequestProperty("WM_PAGE_URL", "https://www.walmart.com/search?q=" + query_param);
            httpConn.setRequestProperty("X-APOLLO-OPERATION-NAME", "Search");
            httpConn.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
            httpConn.setRequestProperty("x-o-bu", "WALMART-US");
            httpConn.setRequestProperty("traceparent", "iP4jdHzhhTzWO5cmKsOpWt-N_3uiFcXe5kJC");
            httpConn.setRequestProperty("x-o-mart", "B2C");
            httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
            httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
            httpConn.setRequestProperty("x-o-platform", "rweb");
            httpConn.setRequestProperty("Content-Type", "application/json");
            httpConn.setRequestProperty("accept", "application/json");
            httpConn.setRequestProperty("Referer", "https://www.walmart.com/search?q=" + query_param);
            httpConn.setRequestProperty("x-enable-server-timing", "1");
            httpConn.setRequestProperty("x-o-ccm", "server");
            httpConn.setRequestProperty("wm_qos.correlation_id", "iP4jdHzhhTzWO5cmKsOpWt-N_3uiFcXe5kJC");

            httpConn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
            writer.write("{\"query\":\"query Search( $query:String $page:Int $prg:Prg! $facet:String $sort:Sort = best_match $catId:String $max_price:String $min_price:String $spelling:Boolean = true $affinityOverride:AffinityOverride $storeSlotBooked:String $ps:Int $ptss:String $recall_set:String $fitmentFieldParams:JSON ={}$fitmentSearchParams:JSON ={}$fetchMarquee:Boolean! $trsp:String $fetchSkyline:Boolean! $fetchSbaTop:Boolean! $additionalQueryParams:JSON ={}$searchArgs:SearchArgumentsForCLS $enablePortableFacets:Boolean = false $intentSource:IntentSource $tenant:String! $enableFacetCount:Boolean = true $pageType:String! = \\\"SearchPage\\\" ){search( query:$query page:$page prg:$prg facet:$facet sort:$sort cat_id:$catId max_price:$max_price min_price:$min_price spelling:$spelling affinityOverride:$affinityOverride storeSlotBooked:$storeSlotBooked ps:$ps ptss:$ptss recall_set:$recall_set trsp:$trsp intentSource:$intentSource additionalQueryParams:$additionalQueryParams pageType:$pageType ){query searchResult{...SearchResultFragment}}contentLayout( channel:\\\"WWW\\\" pageType:$pageType tenant:$tenant searchArgs:$searchArgs ){modules{...ModuleFragment configs{...SearchNonItemFragment __typename...on TempoWM_GLASSWWWSponsoredProductCarouselConfigs{_rawConfigs}...on _TempoWM_GLASSWWWSearchSortFilterModuleConfigs{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}}...on _TempoWM_GLASSWWWSearchGuidedNavModuleConfigs{guidedNavigation{...GuidedNavFragment}}...on TempoWM_GLASSWWWPillsModuleConfigs{moduleSource pillsV2{...PillsModuleFragment}}...TileTakeOverProductFragment...on TempoWM_GLASSWWWSearchFitmentModuleConfigs{fitments( fitmentSearchParams:$fitmentSearchParams fitmentFieldParams:$fitmentFieldParams ){...FitmentFragment sisFitmentResponse{...SearchResultFragment}}}...on TempoWM_GLASSWWWStoreSelectionHeaderConfigs{fulfillmentMethodLabel storeDislayName}...BrandAmplifierAdConfigs @include(if:$fetchSbaTop)...BannerModuleFragment...MarqueeDisplayAdConfigsFragment @include(if:$fetchMarquee)...SkylineDisplayAdConfigsFragment @include(if:$fetchSkyline)...HorizontalChipModuleConfigsFragment...SkinnyBannerFragment}}...LayoutFragment pageMetadata{location{pickupStore deliveryStore intent postalCode stateOrProvinceCode city storeId accessPointId accessType spokeNodeId}pageContext}}}fragment SearchResultFragment on SearchInterface{title aggregatedCount...BreadCrumbFragment...DebugFragment...ItemStacksFragment...PageMetaDataFragment...PaginationFragment...SpellingFragment...SpanishTranslationFragment...RequestContextFragment...ErrorResponse modules{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}guidedNavigation{...GuidedNavFragment}guidedNavigationV2{...PillsModuleFragment}pills{...PillsModuleFragment}spellCheck{title subTitle urlLinkText url}}}fragment ModuleFragment on TempoModule{name version type moduleId schedule{priority}matchedTrigger{zone}}fragment LayoutFragment on ContentLayout{layouts{id layout}}fragment BreadCrumbFragment on SearchInterface{breadCrumb{id name url}}fragment DebugFragment on SearchInterface{debug{sisUrl adsUrl}}fragment ItemStacksFragment on SearchInterface{itemStacks{displayMessage meta{adsBeacon{adUuid moduleInfo max_ads}query stackId stackType title layoutEnum totalItemCount totalItemCountDisplay viewAllParams{query cat_id sort facet affinityOverride recall_set min_price max_price}}itemsV2{...ItemFragment...InGridMarqueeAdFragment...TileTakeOverTileFragment}}}fragment ItemFragment on Product{__typename id usItemId fitmentLabel name checkStoreAvailabilityATC seeShippingEligibility brand type shortDescription weightIncrement imageInfo{...ProductImageInfoFragment}canonicalUrl externalInfo{url}itemType category{path{name url}}badges{flags{...on BaseBadge{key text type id}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{...on BaseBadge{key text type}}}classType averageRating numberOfReviews esrb mediaRating salesUnitType sellerId sellerName hasSellerBadge availabilityStatusV2{display value}groupMetaData{groupType groupSubType numberOfComponents groupComponents{quantity offerId componentType productDisplayName}}productLocation{displayValue aisle{zone aisle}}fulfillmentSpeed offerId preOrder{...PreorderFragment}priceInfo{...ProductPriceInfoFragment}variantCriteria{...VariantCriteriaFragment}snapEligible fulfillmentBadge fulfillmentTitle fulfillmentType brand manufacturerName showAtc sponsoredProduct{spQs clickBeacon spTags viewBeacon}showOptions showBuyNow rewards{eligible state minQuantity rewardAmt promotionId selectionToken cbOffer term expiry description}}fragment ProductImageInfoFragment on ProductImageInfo{thumbnailUrl size}fragment ProductPriceInfoFragment on ProductPriceInfo{priceRange{minPrice maxPrice}currentPrice{...ProductPriceFragment}comparisonPrice{...ProductPriceFragment}wasPrice{...ProductPriceFragment}unitPrice{...ProductPriceFragment}listPrice{...ProductPriceFragment}savingsAmount{...ProductSavingsFragment}shipPrice{...ProductPriceFragment}subscriptionPrice{priceString subscriptionString}priceDisplayCodes{priceDisplayCondition finalCostByWeight submapType}}fragment PreorderFragment on PreOrder{isPreOrder preOrderMessage preOrderStreetDateMessage}fragment ProductPriceFragment on ProductPrice{price priceString variantPriceString priceType currencyUnit priceDisplay}fragment ProductSavingsFragment on ProductSavings{amount percent priceString}fragment VariantCriteriaFragment on VariantCriterion{name type id isVariantTypeSwatch variantList{id images name rank swatchImageUrl availabilityStatus products selectedProduct{canonicalUrl usItemId}}}fragment InGridMarqueeAdFragment on MarqueePlaceholder{__typename type moduleLocation lazy}fragment TileTakeOverTileFragment on TileTakeOverProductPlaceholder{__typename type tileTakeOverTile{span title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}fragment PageMetaDataFragment on SearchInterface{pageMetadata{storeSelectionHeader{fulfillmentMethodLabel storeDislayName}title canonical description location{addressId}}}fragment PaginationFragment on SearchInterface{paginationV2{maxPage pageProperties}}fragment SpanishTranslationFragment on SearchInterface{translation{metadata{originalQuery translatedQuery isTranslated translationOfferType moduleSource}translationModule{title urlLinkText originalQueryUrl}}}fragment SpellingFragment on SearchInterface{spelling{correctedTerm}}fragment RequestContextFragment on SearchInterface{requestContext{vertical isFitmentFilterQueryApplied searchMatchType categories{id name}}}fragment ErrorResponse on SearchInterface{errorResponse{correlationId source errorCodes errors{errorType statusCode statusMsg source}}}fragment GuidedNavFragment on GuidedNavigationSearchInterface{title url}fragment PillsModuleFragment on PillsSearchInterface{title url image:imageV1{src alt}}fragment BannerModuleFragment on TempoWM_GLASSWWWSearchBannerConfigs{moduleType viewConfig{title image imageAlt displayName description url urlAlt appStoreLink appStoreLinkAlt playStoreLink playStoreLinkAlt}}fragment FacetFragment on Facet{title name type layout min max selectedMin selectedMax unboundedMax stepSize isSelected values{id title name description type itemCount @include(if:$enableFacetCount) isSelected baseSeoURL}}fragment FitmentFragment on Fitments{partTypeIDs result{status formId position quantityTitle extendedAttributes{...FitmentFieldFragment}labels{...LabelFragment}resultSubTitle notes suggestions{...FitmentSuggestionFragment}}labels{...LabelFragment}savedVehicle{vehicleType{...VehicleFieldFragment}vehicleYear{...VehicleFieldFragment}vehicleMake{...VehicleFieldFragment}vehicleModel{...VehicleFieldFragment}additionalAttributes{...VehicleFieldFragment}}fitmentFields{...VehicleFieldFragment}fitmentForms{id fields{...FitmentFieldFragment}title labels{...LabelFragment}}}fragment LabelFragment on FitmentLabels{ctas{...FitmentLabelEntityFragment}messages{...FitmentLabelEntityFragment}links{...FitmentLabelEntityFragment}images{...FitmentLabelEntityFragment}}fragment FitmentLabelEntityFragment on FitmentLabelEntity{id label}fragment VehicleFieldFragment on FitmentVehicleField{id label value}fragment FitmentFieldFragment on FitmentField{id displayName value extended data{value label}dependsOn}fragment FitmentSuggestionFragment on FitmentSuggestion{id position loadIndex speedRating searchQueryParam labels{...LabelFragment}cat_id fitmentSuggestionParams{id value}}fragment MarqueeDisplayAdConfigsFragment on TempoWM_GLASSWWWMarqueeDisplayAdConfigs{_rawConfigs ad{...DisplayAdFragment}}fragment DisplayAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataDisplayAdFragment}}}fragment AdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment AdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment SkylineDisplayAdConfigsFragment on TempoWM_GLASSWWWSkylineDisplayAdConfigs{_rawConfigs ad{...SkylineDisplayAdFragment}}fragment SkylineDisplayAdFragment on Ad{...SkylineAdFragment adContent{type data{__typename...SkylineAdDataDisplayAdFragment}}}fragment SkylineAdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment SkylineAdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment BrandAmplifierAdConfigs on TempoWM_GLASSWWWBrandAmplifierAdConfigs{_rawConfigs moduleLocation ad{...SponsoredBrandsAdFragment}}fragment SponsoredBrandsAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataSponsoredBrandsFragment}}}fragment AdDataSponsoredBrandsFragment on AdData{...on SponsoredBrands{adUuid adExpInfo moduleInfo brands{logo{featuredHeadline featuredImage featuredImageName featuredUrl logoClickTrackUrl}products{...ProductFragment}}}}fragment ProductFragment on Product{usItemId offerId badges{flags{__typename...on BaseBadge{id text key query type}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought criteria{name value}}}labels{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{__typename...on BaseBadge{id text key}}}priceInfo{priceDisplayCodes{rollback reducedPrice eligibleForAssociateDiscount clearance strikethrough submapType priceDisplayCondition unitOfMeasure pricePerUnitUom}currentPrice{price priceString}wasPrice{price priceString}priceRange{minPrice maxPrice priceString}unitPrice{price priceString}}showOptions sponsoredProduct{spQs clickBeacon spTags}canonicalUrl numberOfReviews averageRating availabilityStatus imageInfo{thumbnailUrl allImages{id url}}name fulfillmentBadge classType type showAtc p13nData{predictedQuantity flags{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}labels{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}}}fragment SearchNonItemFragment on TempoWM_GLASSWWWSearchNonItemConfigs{title subTitle urlLinkText url}fragment HorizontalChipModuleConfigsFragment on TempoWM_GLASSWWWHorizontalChipModuleConfigs{chipModuleSource:moduleSource chipModule{title url{linkText title clickThrough{type value}}}chipModuleWithImages{title url{linkText title clickThrough{type value}}image{alt clickThrough{type value}height src title width}}}fragment SkinnyBannerFragment on TempoWM_GLASSWWWSkinnyBannerConfigs{bannerType desktopBannerHeight bannerImage{src title alt}mobileBannerHeight mobileImage{src title alt}clickThroughUrl{clickThrough{value}}backgroundColor heading{title fontColor}subHeading{title fontColor}bannerCta{ctaLink{linkText clickThrough{value}}textColor ctaType}}fragment TileTakeOverProductFragment on TempoWM_GLASSWWWTileTakeOverProductConfigs{dwebSlots mwebSlots TileTakeOverProductDetails{pageNumber span dwebPosition mwebPosition title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}\",\"variables\":{\"id\":\"\",\"dealsId\":\"\",\"query\":\"" + query_param + "\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"" + query_param + "\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"fitmentFieldParams\":{\"powerSportEnabled\":true},\"fitmentSearchParams\":{\"id\":\"\",\"dealsId\":\"\",\"query\":\"" + query_param + "\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"" + query_param + "\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"cat_id\":\"\",\"_be_shelf_id\":\"\"},\"enablePortableFacets\":true,\"enableFacetCount\":true,\"fetchMarquee\":true,\"fetchSkyline\":true,\"fetchSbaTop\":true,\"tenant\":\"WM_GLASS\",\"pageType\":\"SearchPage\"}}");
            writer.flush();
            writer.close();
            httpConn.getOutputStream().close();

            InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                    ? httpConn.getInputStream()
                    : httpConn.getErrorStream();
            Scanner s = new Scanner(responseStream).useDelimiter("\\A");
            response = s.hasNext() ? s.next() : "";
            // 60645 (Chicago, IL)
        } else if (store_id.equals("4177")) {
            System.out.println("CHICAGO, IL");
            URL url = new URL("https://www.walmart.com/orchestra/home/graphql/search?query=" + query_param + "&page=1&prg=desktop&sort=best_match&ps=40&searchArgs.query=" + query_param + "&searchArgs.prg=desktop&fitmentFieldParams=true&enablePortableFacets=true&enableFacetCount=true&fetchMarquee=true&fetchSkyline=true&fetchSbaTop=true&tenant=WM_GLASS");
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");

            httpConn.setRequestProperty("authority", "www.walmart.com");
            httpConn.setRequestProperty("accept", "application/json");
            httpConn.setRequestProperty("accept-language", "en-US,en;q=0.9");
            httpConn.setRequestProperty("cache-control", "no-cache");
            httpConn.setRequestProperty("content-type", "application/json");
            httpConn.setRequestProperty("cookie", "ACID=75163779-30d7-44dc-9f48-906f0d74316e; hasACID=true; TB_Latency_Tracker_100=1; TB_Navigation_Preload_01=1; TB_SFOU-100=; vtc=bGyEseXp-IozsFx_exNrwM; _pxhd=5c28b416c4e36f8cdcb60b52f06f33890cc6d1eebdf299bb10048055926d59e0:54a64c37-1ccd-11ed-9521-597a74517274; adblocked=false; TBV=7; pxcts=55810fbf-1ccd-11ed-b02d-68756d7a6748; _pxvid=54a64c37-1ccd-11ed-9521-597a74517274; hasLocData=1; bstc=R8ZkrK5TZWWfKUZCTbHpZc; mobileweb=0; exp-ck=2SWkj15_9FA2927zv1Af1yH1HDfyl2MRnMk1Nnski1PzQ_l1Q_fUo1SmVSa1TA_mk2XBPw91XU_XK1fZHdi1g8z9_1kFqfr1n3tYK1nzyw-1odFJG1pGAhC1qG0gZ2qyn672yxNJ65; _astc=a90c415f4aa683a6946d96c09b6578e6; wmlh=d4831d3c66d3a76c555ec3826b29dac4556990c5a6188d6c05b7f34ff1192735; tb_sw_supported=true; bm_mi=32748D0BBBC6B2F7AB28C947619FD97F~YAAQF40duHdW9mqCAQAA+c5eoxD3Mw+hnJmpb6ujdleRQ4BlnvvYbVVgr1PJj5qglGghHB73efUQTR9d8EYv9KqJ18vIgBJwSrZWaLeGn5O1p35m2ORhzVCMULWli3CIyl3RyvDsFt3/Pf80YDB95ESq1+PawIhnR/ltyOMX7tnDwSpykAcYtyXm6u0pqt9XQROwuRTwFHPttr/tZbIfIYS0sZ92BSUNvsG6a9iusi7+KZ0BrVqfyYWtou3XtGyOXq0qZ6UWO7GdBfOti6CLCkL4Cf9IcECwRNSMIDSFgvdXoq7R1d0YDwffYfhy+c7DTZT3SLzslz7dwM/fcuBc5xE7ci6CbnEqTzIyu7pB/w9keyVmgq+Ei020cGZdsMLM6iVV2jxyd6dit6A1UN93UWLOZZvX8/pADZ7R~1; ak_bmsc=2D6CA2BC539214156CA11BF42ADD7C31~000000000000000000000000000000~YAAQF40duPhW9mqCAQAA2NheoxAqP0mqOwPyqw+6QWpBnkEzMTPzAcKEfnzAP8MgxXJweyWfmi/rY7AEAmowd8pY43U5XE8Z7Wuy+m7/N1OKOjplFRIdhSWtfcbZK7vbpPob0wexRjN8cXDtdQSc/nhsUGkeI38XgLuRYM5Y1+aF1KVK3gbrLba/RiK3cn96e7YXpupjLquBbwaIgR3Qa6AITeQv6NS0xnva/vC0JMGP8luhWRKF14hYTHE1TxXdCwskZ3SAmDaN/yJ/kg2dPoUkWj8PQepfLQUDQgdmz/rbAVkLmZlUNncXNsxSmejB7DkTIsD0A6hhLISEML1pP6eRKrVSmeflYACiqBsZPGheiP+EmZrBM16ZLGbEQP8xgUJX/nszt0hQaZlCyYnbt5UGp8B18p9AuspxsImykNY5Ifw09w9sP3nlN3u7kwUH275IDHt/xVeMSXhXQNRJxKQP1+fkyYwc58X8ufNNbBsw0tpdQUkzdVT98ov202I+09/QUJwHrt47Jw==; TS01b0be75=01538efd7c66a354a383bfd2413e72997a3dfb28b1304fc054cebc1350fdde27dc22859b6f03e929db529c17edd82a4f936f265ef2; akavpau_p1=1660603925~id=7d92a8e6536a9aa8084b95e3060269f0; AID=wmlspartner%253D0%253Areflectorid%253D0000000000000000000000%253Alastupd%253D1660603747620; xpa=1lxDt|2Oqb6|2SWkj|5_9FA|5cBG1|927zv|9l3gx|AYraE|Af1yH|BIcmp|GkqrP|HDfyl|LTD5Y|MRnMk|Nnski|P_fCM|PzQ_l|Q_fUo|RZlxg|SmVSa|TA_mk|XBPw9|XU_XK|bcl64|cfVAR|elin2|fZHdi|g8z9_|kFqfr|lqVt_|n3tYK|nzyw-|odFJG|pGAhC|qG0gZ|qyn67|s9Rs7|sbXp_|uru_L|vwMwa|yxNJ6|zCylr; xpm=1%2B1660603836%2BbGyEseXp-IozsFx_exNrwM~%2B0; auth=MTAyOTYyMDE4D%2BBjzKFG%2B5buTiO3OcbRfTqy2h1iiAcxl6cUdB9GD4uCDg42S7myix347huQ4%2F3aZN0ouAh1mjBkMGElLXPkwo0mrbWZog%2Bz3R1IiJk9Z40E%2BKH57TtParulWI0Ja3kv767wuZloTfhm7Wk2KcjygsxZsyqqU4iLObNl%2BMfESvnarXjkv9EZPtcS1PWgOMulcu0218W2TVzIid8xf%2BVHyAo%2BcuBzw3BRC1x13bKyXOsUMk70P8glgOEpLOprhDfMM%2FFHGZ2dCNmxWrdkwqEKrjLBtMiYw9u5da%2FtWIn5ycyHOky2prc3Wpn2%2B9PaWmbPf05lLil5Ar%2F7DDgFVs%2FXrLVE%2BvxsB97HuO94OAaXtRcFEpLxLGNe5NBJiM9wl9sKhGonPgqYhmgxRmV4vdxw55E5WBBdZBCyKnCQAR7o6eg%3D; locDataV3=eyJpc0RlZmF1bHRlZCI6ZmFsc2UsImluc3RvcmUiOmZhbHNlLCJpbnRlbnQiOiJQSUNLVVAiLCJwaWNrdXAiOlt7ImJ1SWQiOiIwIiwibm9kZUlkIjoiNDE3NyIsImRpc3BsYXlOYW1lIjoiTGluY29sbndvb2QgV2FsbWFydCBQaWNrdXAgU3RvcmUiLCJub2RlVHlwZSI6IlNUT1JFIiwiYWRkcmVzcyI6eyJwb3N0YWxDb2RlIjoiNjA3MTIiLCJhZGRyZXNzTGluZTEiOiI2ODQwIE4gTWNjb3JtaWNrIEJsdmQiLCJjaXR5IjoiTGluY29sbndvb2QiLCJzdGF0ZSI6IklMIiwiY291bnRyeSI6IlVTIiwicG9zdGFsQ29kZTkiOiI2MDcxMi0yNzAzIn0sImdlb1BvaW50Ijp7ImxhdGl0dWRlIjo0Mi4wMDU1MDgsImxvbmdpdHVkZSI6LTg3LjcxMzIyNX0sImlzR2xhc3NFbmFibGVkIjp0cnVlLCJzY2hlZHVsZWRFbmFibGVkIjp0cnVlLCJ1blNjaGVkdWxlZEVuYWJsZWQiOmZhbHNlLCJodWJOb2RlSWQiOiI0MTc3Iiwic3RvcmVIcnMiOiIwNzowMC0yMDozMCIsInN1cHBvcnRlZEFjY2Vzc1R5cGVzIjpbIlBJQ0tVUF9DVVJCU0lERSIsIlBJQ0tVUF9JTlNUT1JFIl19XSwic2hpcHBpbmdBZGRyZXNzIjp7ImxhdGl0dWRlIjo0Mi4wMDkyLCJsb25naXR1ZGUiOi04Ny42OTQyLCJwb3N0YWxDb2RlIjoiNjA2NDUiLCJjaXR5IjoiQ2hpY2FnbyIsInN0YXRlIjoiSUwiLCJjb3VudHJ5Q29kZSI6IlVTQSIsImdpZnRBZGRyZXNzIjpmYWxzZX0sImFzc29ydG1lbnQiOnsibm9kZUlkIjoiNDE3NyIsImRpc3BsYXlOYW1lIjoiTGluY29sbndvb2QgV2FsbWFydCBQaWNrdXAgU3RvcmUiLCJhY2Nlc3NQb2ludHMiOm51bGwsInN1cHBvcnRlZEFjY2Vzc1R5cGVzIjpbXSwiaW50ZW50IjoiUElDS1VQIiwic2NoZWR1bGVFbmFibGVkIjpmYWxzZX0sImlzRXhwbGljaXQiOnRydWUsImRlbGl2ZXJ5Ijp7ImJ1SWQiOiIwIiwibm9kZUlkIjoiNDE3NyIsImRpc3BsYXlOYW1lIjoiTGluY29sbndvb2QgV2FsbWFydCBQaWNrdXAgU3RvcmUiLCJub2RlVHlwZSI6IlNUT1JFIiwiYWRkcmVzcyI6eyJwb3N0YWxDb2RlIjoiNjA3MTIiLCJhZGRyZXNzTGluZTEiOiI2ODQwIE4gTWNjb3JtaWNrIEJsdmQiLCJjaXR5IjoiTGluY29sbndvb2QiLCJzdGF0ZSI6IklMIiwiY291bnRyeSI6IlVTIiwicG9zdGFsQ29kZTkiOiI2MDcxMi0yNzAzIn0sImdlb1BvaW50Ijp7ImxhdGl0dWRlIjo0Mi4wMDU1MDgsImxvbmdpdHVkZSI6LTg3LjcxMzIyNX0sImlzR2xhc3NFbmFibGVkIjp0cnVlLCJzY2hlZHVsZWRFbmFibGVkIjp0cnVlLCJ1blNjaGVkdWxlZEVuYWJsZWQiOmZhbHNlLCJhY2Nlc3NQb2ludHMiOlt7ImFjY2Vzc1R5cGUiOiJERUxJVkVSWV9BRERSRVNTIn1dLCJodWJOb2RlSWQiOiI0MTc3IiwiaXNFeHByZXNzRGVsaXZlcnlPbmx5IjpmYWxzZSwic3VwcG9ydGVkQWNjZXNzVHlwZXMiOlsiREVMSVZFUllfQUREUkVTUyJdfSwicmVmcmVzaEF0IjoxNjYwNjI2MjE4MTIxLCJ2YWxpZGF0ZUtleSI6InByb2Q6djI6NzUxNjM3NzktMzBkNy00NGRjLTlmNDgtOTA2ZjBkNzQzMTZlIn0%3D; assortmentStoreId=4177; locGuestData=eyJpbnRlbnQiOiJQSUNLVVAiLCJpc0V4cGxpY2l0Ijp0cnVlLCJzdG9yZUludGVudCI6IlBJQ0tVUCIsIm1lcmdlRmxhZyI6dHJ1ZSwiaXNEZWZhdWx0ZWQiOmZhbHNlLCJwaWNrdXAiOnsibm9kZUlkIjoiNDE3NyIsInRpbWVzdGFtcCI6MTY2MDYwNDYxODExMX0sInBvc3RhbENvZGUiOnsidGltZXN0YW1wIjoxNjYwNjA0NjE4MTExLCJiYXNlIjoiNjA2NDUifSwidmFsaWRhdGVLZXkiOiJwcm9kOnYyOjc1MTYzNzc5LTMwZDctNDRkYy05ZjQ4LTkwNmYwZDc0MzE2ZSJ9; _px3=be89392598ebda4ad88eaab55926ec60662906d1e08ec8d789093517895e7f18:ljz8BQKf+WFxUZozP0qyRbk1WpDx6S9VftZxtvLBBhft1MH91F+A9bjsWMC2IJv27xE3mTed5zZpYvQ9g8dO+g==:1000:L4plUYA22Wnmoh6KFDLRRHX+TOB+Fd6g9f71PiZmAHFpVU+MQj6BFHXjqkPTN1jrrpF9YdzUGoTvdODZ9RZXKxGb5tojAJc5B6p+MA5+NYx1dopTZ/hv9n8NYEyHwOje2Co/FiOlpY2xbDzLbUR5l5pU2JbMXe3CUc2rZ1mXIMk/7vec3oC6HujQwoYOFwHvTd/wfyVPR5Py93RTO44/Ow==; dimensionData=769; com.wm.reflector=\"reflectorid:0000000000000000000000@lastupd:1660604733000@firstcreate:1660590373274\"; xptwg=2714392201:16526BC2CC8D2D0:39BBB4F:FDEFF3BD:FB287932:CD8163F1:; akavpau_p2=1660605334~id=0439f3a411a733e9cbe9678378a47f19; TS012768cf=01cfcb78496361f6d31d689cd5727b2442498d4140be8c920b6ab15f37b57d3cf3ea849c84ec92767b55d7490bf73dfd6d32ef2b6c; TS01a90220=01cfcb78496361f6d31d689cd5727b2442498d4140be8c920b6ab15f37b57d3cf3ea849c84ec92767b55d7490bf73dfd6d32ef2b6c; TS2a5e0c5c027=08417417ecab20000cd8cfb636c6f7964836fefa27c4cf585fb46c4399849354828aa83917c77f2008d468a7811130003d85357942232bb4b0d47cf1bad6bca6041ea5443ec8f89a8ab5636340c1f2922c26b82bd98552b54f43f82912c4060a; bm_sv=6C2B54C207872945500C425398670ED0~YAAQZo0duNO5nIKCAQAAEWzBoxA/wvd/f2kK1B0od6rBBudWZSkNISw6n+pXSPAG9N/a0MWAeyISjbqEbqsdMPuQobnOhFNUS54/KDtOW61FXeSuqoSnO46JKG3RIHh9c1zVTBpGRjupvIOtUpHzMH97DUlYa2ppZDWzrc92oEsq8F2r1MtQ8C62CFiFwwZpI2+NX/LPFg+o5xHdoBo7ufJ8tQht34YmFGjwH3OjZT5lQ6ATXdDKauLLxW6g/NvnhsxV~1");
            httpConn.setRequestProperty("device_profile_ref_id", "nEpdnEQXqVs3aLXRx-QqC6mbXqd4u0b0gj79");
            httpConn.setRequestProperty("origin", "https://www.walmart.com");
            httpConn.setRequestProperty("pragma", "no-cache");
            httpConn.setRequestProperty("referer", "https://www.walmart.com/search?q=" + query_param);
            httpConn.setRequestProperty("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
            httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
            httpConn.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
            httpConn.setRequestProperty("sec-fetch-dest", "empty");
            httpConn.setRequestProperty("sec-fetch-mode", "cors");
            httpConn.setRequestProperty("sec-fetch-site", "same-origin");
            httpConn.setRequestProperty("traceparent", "VDjaxoJNL8q7YUWD0tfLa1O5nlE8IOv0zzBg");
            httpConn.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
            httpConn.setRequestProperty("wm_mp", "true");
            httpConn.setRequestProperty("wm_page_url", "https://www.walmart.com/search?q=" + query_param);
            httpConn.setRequestProperty("wm_qos.correlation_id", "VDjaxoJNL8q7YUWD0tfLa1O5nlE8IOv0zzBg");
            httpConn.setRequestProperty("x-apollo-operation-name", "Search");
            httpConn.setRequestProperty("x-enable-server-timing", "1");
            httpConn.setRequestProperty("x-latency-trace", "1");
            httpConn.setRequestProperty("x-o-bu", "WALMART-US");
            httpConn.setRequestProperty("x-o-ccm", "server");
            httpConn.setRequestProperty("x-o-correlation-id", "VDjaxoJNL8q7YUWD0tfLa1O5nlE8IOv0zzBg");
            httpConn.setRequestProperty("x-o-gql-query", "query Search");
            httpConn.setRequestProperty("x-o-mart", "B2C");
            httpConn.setRequestProperty("x-o-platform", "rweb");
            httpConn.setRequestProperty("x-o-platform-version", "main-1.13.0-6550cc");
            httpConn.setRequestProperty("x-o-segment", "oaoh");

            httpConn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
            writer.write("{\"query\":\"query Search( $query:String $page:Int $prg:Prg! $facet:String $sort:Sort = best_match $catId:String $max_price:String $min_price:String $spelling:Boolean = true $affinityOverride:AffinityOverride $storeSlotBooked:String $ps:Int $ptss:String $recall_set:String $fitmentFieldParams:JSON ={}$fitmentSearchParams:JSON ={}$fetchMarquee:Boolean! $trsp:String $fetchSkyline:Boolean! $fetchSbaTop:Boolean! $additionalQueryParams:JSON ={}$searchArgs:SearchArgumentsForCLS $enablePortableFacets:Boolean = false $intentSource:IntentSource $tenant:String! $enableFacetCount:Boolean = true $pageType:String! = \\\"SearchPage\\\" ){search( query:$query page:$page prg:$prg facet:$facet sort:$sort cat_id:$catId max_price:$max_price min_price:$min_price spelling:$spelling affinityOverride:$affinityOverride storeSlotBooked:$storeSlotBooked ps:$ps ptss:$ptss recall_set:$recall_set trsp:$trsp intentSource:$intentSource additionalQueryParams:$additionalQueryParams pageType:$pageType ){query searchResult{...SearchResultFragment}}contentLayout( channel:\\\"WWW\\\" pageType:$pageType tenant:$tenant searchArgs:$searchArgs ){modules{...ModuleFragment configs{...SearchNonItemFragment __typename...on TempoWM_GLASSWWWSponsoredProductCarouselConfigs{_rawConfigs}...on _TempoWM_GLASSWWWSearchSortFilterModuleConfigs{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}}...on _TempoWM_GLASSWWWSearchGuidedNavModuleConfigs{guidedNavigation{...GuidedNavFragment}}...on TempoWM_GLASSWWWPillsModuleConfigs{moduleSource pillsV2{...PillsModuleFragment}}...TileTakeOverProductFragment...on TempoWM_GLASSWWWSearchFitmentModuleConfigs{fitments( fitmentSearchParams:$fitmentSearchParams fitmentFieldParams:$fitmentFieldParams ){...FitmentFragment sisFitmentResponse{...SearchResultFragment}}}...on TempoWM_GLASSWWWStoreSelectionHeaderConfigs{fulfillmentMethodLabel storeDislayName}...BrandAmplifierAdConfigs @include(if:$fetchSbaTop)...BannerModuleFragment...MarqueeDisplayAdConfigsFragment @include(if:$fetchMarquee)...SkylineDisplayAdConfigsFragment @include(if:$fetchSkyline)...HorizontalChipModuleConfigsFragment...SkinnyBannerFragment}}...LayoutFragment pageMetadata{location{pickupStore deliveryStore intent postalCode stateOrProvinceCode city storeId accessPointId accessType spokeNodeId}pageContext}}}fragment SearchResultFragment on SearchInterface{title aggregatedCount...BreadCrumbFragment...DebugFragment...ItemStacksFragment...PageMetaDataFragment...PaginationFragment...SpellingFragment...SpanishTranslationFragment...RequestContextFragment...ErrorResponse modules{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}guidedNavigation{...GuidedNavFragment}guidedNavigationV2{...PillsModuleFragment}pills{...PillsModuleFragment}spellCheck{title subTitle urlLinkText url}}}fragment ModuleFragment on TempoModule{name version type moduleId schedule{priority}matchedTrigger{zone}}fragment LayoutFragment on ContentLayout{layouts{id layout}}fragment BreadCrumbFragment on SearchInterface{breadCrumb{id name url}}fragment DebugFragment on SearchInterface{debug{sisUrl adsUrl}}fragment ItemStacksFragment on SearchInterface{itemStacks{displayMessage meta{adsBeacon{adUuid moduleInfo max_ads}query stackId stackType title layoutEnum totalItemCount totalItemCountDisplay viewAllParams{query cat_id sort facet affinityOverride recall_set min_price max_price}}itemsV2{...ItemFragment...InGridMarqueeAdFragment...TileTakeOverTileFragment}}}fragment ItemFragment on Product{__typename id usItemId fitmentLabel name checkStoreAvailabilityATC seeShippingEligibility brand type shortDescription weightIncrement imageInfo{...ProductImageInfoFragment}canonicalUrl externalInfo{url}itemType category{path{name url}}badges{flags{...on BaseBadge{key text type id}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{...on BaseBadge{key text type}}}classType averageRating numberOfReviews esrb mediaRating salesUnitType sellerId sellerName hasSellerBadge availabilityStatusV2{display value}groupMetaData{groupType groupSubType numberOfComponents groupComponents{quantity offerId componentType productDisplayName}}productLocation{displayValue aisle{zone aisle}}fulfillmentSpeed offerId preOrder{...PreorderFragment}priceInfo{...ProductPriceInfoFragment}variantCriteria{...VariantCriteriaFragment}snapEligible fulfillmentBadge fulfillmentTitle fulfillmentType brand manufacturerName showAtc sponsoredProduct{spQs clickBeacon spTags viewBeacon}showOptions showBuyNow rewards{eligible state minQuantity rewardAmt promotionId selectionToken cbOffer term expiry description}}fragment ProductImageInfoFragment on ProductImageInfo{thumbnailUrl size}fragment ProductPriceInfoFragment on ProductPriceInfo{priceRange{minPrice maxPrice}currentPrice{...ProductPriceFragment}comparisonPrice{...ProductPriceFragment}wasPrice{...ProductPriceFragment}unitPrice{...ProductPriceFragment}listPrice{...ProductPriceFragment}savingsAmount{...ProductSavingsFragment}shipPrice{...ProductPriceFragment}subscriptionPrice{priceString subscriptionString}priceDisplayCodes{priceDisplayCondition finalCostByWeight submapType}}fragment PreorderFragment on PreOrder{isPreOrder preOrderMessage preOrderStreetDateMessage}fragment ProductPriceFragment on ProductPrice{price priceString variantPriceString priceType currencyUnit priceDisplay}fragment ProductSavingsFragment on ProductSavings{amount percent priceString}fragment VariantCriteriaFragment on VariantCriterion{name type id isVariantTypeSwatch variantList{id images name rank swatchImageUrl availabilityStatus products selectedProduct{canonicalUrl usItemId}}}fragment InGridMarqueeAdFragment on MarqueePlaceholder{__typename type moduleLocation lazy}fragment TileTakeOverTileFragment on TileTakeOverProductPlaceholder{__typename type tileTakeOverTile{span title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}fragment PageMetaDataFragment on SearchInterface{pageMetadata{storeSelectionHeader{fulfillmentMethodLabel storeDislayName}title canonical description location{addressId}}}fragment PaginationFragment on SearchInterface{paginationV2{maxPage pageProperties}}fragment SpanishTranslationFragment on SearchInterface{translation{metadata{originalQuery translatedQuery isTranslated translationOfferType moduleSource}translationModule{title urlLinkText originalQueryUrl}}}fragment SpellingFragment on SearchInterface{spelling{correctedTerm}}fragment RequestContextFragment on SearchInterface{requestContext{vertical isFitmentFilterQueryApplied searchMatchType categories{id name}}}fragment ErrorResponse on SearchInterface{errorResponse{correlationId source errorCodes errors{errorType statusCode statusMsg source}}}fragment GuidedNavFragment on GuidedNavigationSearchInterface{title url}fragment PillsModuleFragment on PillsSearchInterface{title url image:imageV1{src alt}}fragment BannerModuleFragment on TempoWM_GLASSWWWSearchBannerConfigs{moduleType viewConfig{title image imageAlt displayName description url urlAlt appStoreLink appStoreLinkAlt playStoreLink playStoreLinkAlt}}fragment FacetFragment on Facet{title name type layout min max selectedMin selectedMax unboundedMax stepSize isSelected values{id title name description type itemCount @include(if:$enableFacetCount) isSelected baseSeoURL}}fragment FitmentFragment on Fitments{partTypeIDs result{status formId position quantityTitle extendedAttributes{...FitmentFieldFragment}labels{...LabelFragment}resultSubTitle notes suggestions{...FitmentSuggestionFragment}}labels{...LabelFragment}savedVehicle{vehicleType{...VehicleFieldFragment}vehicleYear{...VehicleFieldFragment}vehicleMake{...VehicleFieldFragment}vehicleModel{...VehicleFieldFragment}additionalAttributes{...VehicleFieldFragment}}fitmentFields{...VehicleFieldFragment}fitmentForms{id fields{...FitmentFieldFragment}title labels{...LabelFragment}}}fragment LabelFragment on FitmentLabels{ctas{...FitmentLabelEntityFragment}messages{...FitmentLabelEntityFragment}links{...FitmentLabelEntityFragment}images{...FitmentLabelEntityFragment}}fragment FitmentLabelEntityFragment on FitmentLabelEntity{id label}fragment VehicleFieldFragment on FitmentVehicleField{id label value}fragment FitmentFieldFragment on FitmentField{id displayName value extended data{value label}dependsOn}fragment FitmentSuggestionFragment on FitmentSuggestion{id position loadIndex speedRating searchQueryParam labels{...LabelFragment}cat_id fitmentSuggestionParams{id value}}fragment MarqueeDisplayAdConfigsFragment on TempoWM_GLASSWWWMarqueeDisplayAdConfigs{_rawConfigs ad{...DisplayAdFragment}}fragment DisplayAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataDisplayAdFragment}}}fragment AdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment AdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment SkylineDisplayAdConfigsFragment on TempoWM_GLASSWWWSkylineDisplayAdConfigs{_rawConfigs ad{...SkylineDisplayAdFragment}}fragment SkylineDisplayAdFragment on Ad{...SkylineAdFragment adContent{type data{__typename...SkylineAdDataDisplayAdFragment}}}fragment SkylineAdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment SkylineAdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment BrandAmplifierAdConfigs on TempoWM_GLASSWWWBrandAmplifierAdConfigs{_rawConfigs moduleLocation ad{...SponsoredBrandsAdFragment}}fragment SponsoredBrandsAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataSponsoredBrandsFragment}}}fragment AdDataSponsoredBrandsFragment on AdData{...on SponsoredBrands{adUuid adExpInfo moduleInfo brands{logo{featuredHeadline featuredImage featuredImageName featuredUrl logoClickTrackUrl}products{...ProductFragment}}}}fragment ProductFragment on Product{usItemId offerId badges{flags{__typename...on BaseBadge{id text key query type}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought criteria{name value}}}labels{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{__typename...on BaseBadge{id text key}}}priceInfo{priceDisplayCodes{rollback reducedPrice eligibleForAssociateDiscount clearance strikethrough submapType priceDisplayCondition unitOfMeasure pricePerUnitUom}currentPrice{price priceString}wasPrice{price priceString}priceRange{minPrice maxPrice priceString}unitPrice{price priceString}}showOptions sponsoredProduct{spQs clickBeacon spTags}canonicalUrl numberOfReviews averageRating availabilityStatus imageInfo{thumbnailUrl allImages{id url}}name fulfillmentBadge classType type showAtc p13nData{predictedQuantity flags{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}labels{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}}}fragment SearchNonItemFragment on TempoWM_GLASSWWWSearchNonItemConfigs{title subTitle urlLinkText url}fragment HorizontalChipModuleConfigsFragment on TempoWM_GLASSWWWHorizontalChipModuleConfigs{chipModuleSource:moduleSource chipModule{title url{linkText title clickThrough{type value}}}chipModuleWithImages{title url{linkText title clickThrough{type value}}image{alt clickThrough{type value}height src title width}}}fragment SkinnyBannerFragment on TempoWM_GLASSWWWSkinnyBannerConfigs{bannerType desktopBannerHeight bannerImage{src title alt}mobileBannerHeight mobileImage{src title alt}clickThroughUrl{clickThrough{value}}backgroundColor heading{title fontColor}subHeading{title fontColor}bannerCta{ctaLink{linkText clickThrough{value}}textColor ctaType}}fragment TileTakeOverProductFragment on TempoWM_GLASSWWWTileTakeOverProductConfigs{dwebSlots mwebSlots TileTakeOverProductDetails{pageNumber span dwebPosition mwebPosition title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}\",\"variables\":{\"id\":\"\",\"dealsId\":\"\",\"query\":\"" + query_param + "\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"" + query_param + "\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"fitmentFieldParams\":{\"powerSportEnabled\":true},\"fitmentSearchParams\":{\"id\":\"\",\"dealsId\":\"\",\"query\":\"" + query_param + "\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"" + query_param + "\",\"cat_id\":\"\",\"prg\":\"desktop\",\"facet\":\"\"},\"cat_id\":\"\",\"_be_shelf_id\":\"\"},\"enablePortableFacets\":true,\"enableFacetCount\":true,\"fetchMarquee\":true,\"fetchSkyline\":true,\"fetchSbaTop\":true,\"tenant\":\"WM_GLASS\",\"pageType\":\"SearchPage\"}}");
            writer.flush();
            writer.close();
            httpConn.getOutputStream().close();

            InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                    ? httpConn.getInputStream()
                    : httpConn.getErrorStream();
            Scanner s = new Scanner(responseStream).useDelimiter("\\A");
            response = s.hasNext() ? s.next() : "";
        }

        JsonElement jsonElement = new JsonParser().parse(response);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(jsonElement);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        Object data = jsonMap.get("data");
        Map<String, Object> dataMap = objectMapper.convertValue(data, Map.class);

        // GETTING BRANDS

        Object contentLayout = dataMap.get("contentLayout");
        Map<String, Object> contentLayoutMap = objectMapper.convertValue(contentLayout, Map.class);

        Object modules = contentLayoutMap.get("modules");
        ArrayList<Object> modulesAL = objectMapper.convertValue(modules, ArrayList.class);

        Object chosenModule = modulesAL.get(1);
        Map<String, Object> moduleMap = objectMapper.convertValue(chosenModule, Map.class);
        Object type = moduleMap.get("type");
        String typeStr = objectMapper.convertValue(type, String.class);
        if (!typeStr.equals("SearchSortFilterModule")) {
            chosenModule = modulesAL.get(2);
            moduleMap = objectMapper.convertValue(chosenModule, Map.class);
        }

        Object configs = moduleMap.get("configs");
        Map<String, Object> configsMap = objectMapper.convertValue(configs, Map.class);

        Object topNavFacets = configsMap.get("topNavFacets");
        ArrayList<Object> topNavFacetsAL = objectMapper.convertValue(topNavFacets, ArrayList.class);

        Object chosenFacet = topNavFacetsAL.get(2);
        Map<String, Object> chosenFacetMap = objectMapper.convertValue(chosenFacet, Map.class);

        Object values = chosenFacetMap.get("values");
        ArrayList<Object> brandsAL = objectMapper.convertValue(values, ArrayList.class);

        HashMap<String, String> brands = new HashMap<String, String>();

        for (Object brand : brandsAL) {
            Map<String, String> brandInfo = objectMapper.convertValue(brand, Map.class);
            brands.put(brandInfo.get("name").toLowerCase(), brandInfo.get("name"));
        }

        // GETTING PRODUCTS

        Object search = dataMap.get("search");

        Map<String, Object> searchMap = objectMapper.convertValue(search, Map.class);
        Object searchResult = searchMap.get("searchResult");

        Map<String, Object> searchResultMap = objectMapper.convertValue(searchResult, Map.class);
        Object itemStacks = searchResultMap.get("itemStacks");

        ArrayList<Object> itemStacksAL = objectMapper.convertValue(itemStacks, ArrayList.class);
        Object firstInAL = itemStacksAL.get(0);

        Map<String, Object> itemsV2Map = objectMapper.convertValue(firstInAL, Map.class);
        Object itemsV2 = itemsV2Map.get("itemsV2");

        ArrayList<Object> itemsAL = objectMapper.convertValue(itemsV2, ArrayList.class);

        for (Object o : itemsAL) {

            HashMap<String, String> newProduct = new HashMap<>();

            Map<String, Object> item = objectMapper.convertValue(o, Map.class);

            // get id
            Object productID = item.get("usItemId");
            String productIDStr = objectMapper.convertValue(productID, String.class);
//			System.out.println("productID: " + productIDStr);
            products.put(productIDStr, newProduct);

            // get name
            Object name = item.get("name");
            String nameStr = objectMapper.convertValue(name, String.class).toLowerCase();
//			System.out.println("name: " + name);
            newProduct.put("name", nameStr);

            // get buyURL
            Object buyURL = item.get("canonicalUrl");
            String buyURLStr = objectMapper.convertValue(buyURL, String.class);
            int toIndex = buyURLStr.indexOf("?");
            if (toIndex <= buyURLStr.length() && toIndex >= 0) {
                buyURLStr = buyURLStr.substring(0, toIndex);
            }
//			System.out.println("buyURL: " + buyURLStr);
            newProduct.put("buyURL", buyURLStr);

            //get brand
            boolean foundBrand = false;

            for (String brand : brands.keySet()) {
                if (nameStr.contains(brand)) {
                    foundBrand = true;
//					System.out.println("brand: " + brands.get(brand));
                    newProduct.put("brand", brands.get(brand));
                    break;
                }
            }

            if (!foundBrand) {
                for (String brand : brands.keySet()) {
                    if (isBrand(nameStr, brand)) {
//						System.out.println("brand: " + brands.get(brand));
                        newProduct.put("brand", brands.get(brand));
                        break;
                    }
                }
            }

            // get availability
            Map<String, Object> itemAvailability = objectMapper.convertValue(item.get("availabilityStatusV2"), Map.class);
//			System.out.println("availability: " + itemAvailability.get("display"));
            Object inStockStatus = itemAvailability.get("display");
            String inStockStatusStr = objectMapper.convertValue(inStockStatus, String.class);
            String availabilityStr = "false";
            if (inStockStatusStr.equals("In stock")) {
                availabilityStr = "true";
            }
            newProduct.put("availability", availabilityStr);

            // get price
            Map<String, Object> itemPriceInfo = objectMapper.convertValue(item.get("priceInfo"), Map.class);
            if (itemPriceInfo.get("currentPrice") != null) {
                Map<String, Object> itemCurPrice = objectMapper.convertValue(itemPriceInfo.get("currentPrice"), Map.class);
                Object price = itemCurPrice.get("price");
                Double priceDouble = objectMapper.convertValue(price, Double.class);
                String priceStr = objectMapper.convertValue(priceDouble, String.class);
//				System.out.println("price: " + itemCurPrice.get("price"));
                newProduct.put("price", priceStr);
            } else if (itemPriceInfo.get("priceRange") != null) {
                Map<String, Object> itemPriceRange = objectMapper.convertValue(itemPriceInfo.get("priceRange"), Map.class);
                Object minPrice = itemPriceRange.get("minPrice");
                Double minPriceDouble = objectMapper.convertValue(minPrice, Double.class);
                Object maxPrice = itemPriceRange.get("maxPrice");
                Double maxPriceDouble = objectMapper.convertValue(maxPrice, Double.class);
                if (minPrice != maxPrice) {
//					System.out.println("min price: " + minPrice);
//					System.out.println("max price: " + maxPrice);
                    String maxPriceStr = objectMapper.convertValue(maxPrice, String.class);
                    newProduct.put("price", maxPriceStr);
                } else {
//					System.out.println("price: " + minPrice);
                    String maxPriceStr = objectMapper.convertValue(maxPrice, String.class);
                    newProduct.put("price", maxPriceStr);
                }
            }

            // get thumbnail image URL
            Object imageInfo = item.get("imageInfo");
            Map<String, Object> imageInfoMap = objectMapper.convertValue(imageInfo, Map.class);
            Object thumbnailURL = imageInfoMap.get("thumbnailUrl");
            String thumbnailURLStr = objectMapper.convertValue(thumbnailURL, String.class);
            newProduct.put("imageURL", thumbnailURLStr);
//			System.out.println("imageURL: " + thumbnailURL);
//			System.out.println("--------------------------------------------------------");
        }
        return products;

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // GET INDIVIDUAL PICKUP VS DELIVERY VS SHIPPING AVAILABILITY OPTIONS
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//		URL url = new URL("https://www.walmart.com/orchestra/home/graphql/ip");
//		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
//		httpConn.setRequestMethod("POST");
//
//		httpConn.setRequestProperty("authority", "www.walmart.com");
//		httpConn.setRequestProperty("accept", "application/json");
//		httpConn.setRequestProperty("accept-language", "en-US,en;q=0.9");
//		httpConn.setRequestProperty("cache-control", "no-cache");
//		httpConn.setRequestProperty("calltype", "CLIENT");
//		httpConn.setRequestProperty("content-type", "application/json");
//		httpConn.setRequestProperty("cookie", "AID=wmlspartner%253D0%253Areflectorid%253D0000000000000000000000%253Alastupd%253D1660428100121; auth=MTAyOTYyMDE4ehDWX%2FpHyXfIOYK3T6MTIDUvBJyRsIx6ZRQ1yJdfPhmawXPN6qIU8XXHXolMyaFuJberVZ4dOTPPgNyt1mRpPG9W%2BBnWb1UMwns8Ex5E4l4rQLT6HHHYf%2BrPchUOkR14767wuZloTfhm7Wk2Kcjygi5k0VvBM%2FJjwcKWWhCnBS%2FeCA9hHbWyiKxs58%2BAL16ICOVoaNl7QhAB9kTzFMcXb0Kg8eKKIDEu%2F8QfVzHKQSoUMk70P8glgOEpLOprhDfMM%2FFHGZ2dCNmxWrdkwqEKrqA%2Bl%2B2vk7qcJK5QqjevkIX9E0GlJd9qt7xfYAwcJ2OgxH1h8DZxbdDqR7kW%2FDV%2B8smjmcFosupf5PhyibK2fKRUIQPCAduHVVxzh9AJFlZBc8Ff2%2BT%2Bngoug3mdNkRb%2F5E5WBBdZBCyKnCQAR7o6eg%3D; ACID=979aacd3-ce1e-4b0b-8a3c-d7286e2ba30b; hasACID=true; assortmentStoreId=" + storeID + "; hasLocData=1; TB_Latency_Tracker_100=1; TB_Navigation_Preload_01=1; TB_SFOU-100=; vtc=R_xfpnBXvr_njANSx6Yv1o; bstc=R_xfpnBXvr_njANSx6Yv1o; mobileweb=0; xpa=2SWkj|5q86Y|9l3gx|AQhLM|Af1yH|CN28l|GkqrP|HDfyl|IOIpg|LTD5Y|MRnMk|PiOGS|PzQ_l|SmVSa|T-onc|TA_mk|W7hWw|XBPw9|XU_XK|bcl64|ccDGr|elin2|fZHdi|jncu9|lqVt_|n3tYK|nzyw-|oDpYF|qyn67|r8csb|s9Rs7|uJQh6|uru_L|vwMwa|yefCT|yxNJ6|zCylr; exp-ck=2SWkj1AQhLM1Af1yH1HDfyl2MRnMk1PzQ_l1SmVSa1TA_mk2W7hWw2XBPw91XU_XK1ccDGr1fZHdi1jncu91n3tYK1nzyw-1qyn672r8csb1yefCT1yxNJ65; _pxhd=c35188f9383b21b96ae1b7f36df86a14261c16b319c0af68aa34fb66c1c1881d:824e2ed8-1b53-11ed-8a3b-6d6465557345; pxcts=8341d6af-1b53-11ed-9c85-77416e716e54; _pxvid=824e2ed8-1b53-11ed-8a3b-6d6465557345; TBV=7; adblocked=false; dimensionData=821; xpm=1%2B1660428100%2BR_xfpnBXvr_njANSx6Yv1o~%2B0; ak_bmsc=028C8FE51D66E78C527BFC1268D68E46~000000000000000000000000000000~YAAQhS4gF7vr2pCCAQAASiw6mRCxc80NIAGBUCnB45zpMxQ4fnc+zG3xVl/tkEP5mMps8hQB1/ck/swj28q+cSS3dTqbasXZWwDWZkqmQDnmYz9fZnwBnTx/71Ih0aun7NCfY4YuXTbXhDcHc256as9leKTD+MqGqiQq9BaUzmesxn/sywoY8mkliYWYcsz7qK5UHSH4Vo+x2A9G1e4nqdzmLstSzkSMAe4j5lj66NYcEQ+sYJjDchEbVLIh19AAYbZYln/Ft5UUC7Agw686PzoNtYeml3PvqaDp7FkI73I4gsqcSSkMgk9NrV6ghpreasTa6ebn+Szcyjc31sbJAJdQFUDecggyTnMUWbr71zPGtcmfLKKUQnXQAt13v0Okya5NpJH23PGOg+Lx0o7gAq65lGcf2CCiigIhlyrisi0sUvJzeqHkGzNkfjp1KjOCpidcEi2KxEMDH7/eDh42k65bbhl9Iq4DR+3KQFYxBGFbsm5l5oj7LoJk5pmZ; locDataV3=eyJpc0RlZmF1bHRlZCI6ZmFsc2UsImlzRXhwbGljaXQiOmZhbHNlLCJpbnRlbnQiOiJTSElQUElORyIsInBpY2t1cCI6W3siYnVJZCI6IjAiLCJub2RlSWQiOiIzMDk4IiwiZGlzcGxheU5hbWUiOiJCZWxsZXZ1ZSBOZWlnaGJvcmhvb2QgTWFya2V0Iiwibm9kZVR5cGUiOiJTVE9SRSIsImFkZHJlc3MiOnsicG9zdGFsQ29kZSI6Ijk4MDA3IiwiYWRkcmVzc0xpbmUxIjoiMTUwNjMgTWFpbiBTdCIsImNpdHkiOiJCZWxsZXZ1ZSIsInN0YXRlIjoiV0EiLCJjb3VudHJ5IjoiVVMiLCJwb3N0YWxDb2RlOSI6Ijk4MDA3LTUyMjUifSwiZ2VvUG9pbnQiOnsibGF0aXR1ZGUiOjQ3LjYwOTAzNiwibG9uZ2l0dWRlIjotMTIyLjEzOTQ4N30sImlzR2xhc3NFbmFibGVkIjp0cnVlLCJzY2hlZHVsZWRFbmFibGVkIjp0cnVlLCJ1blNjaGVkdWxlZEVuYWJsZWQiOmZhbHNlLCJodWJOb2RlSWQiOiIzMDk4Iiwic3RvcmVIcnMiOiIwNjowMC0yMjowMCIsInN1cHBvcnRlZEFjY2Vzc1R5cGVzIjpbIlBJQ0tVUF9DVVJCU0lERSIsIlBJQ0tVUF9JTlNUT1JFIl19XSwic2hpcHBpbmdBZGRyZXNzIjp7ImxhdGl0dWRlIjo0Ny42Mjk3LCJsb25naXR1ZGUiOi0xMjIuMzQ1NSwicG9zdGFsQ29kZSI6Ijk4MTA5IiwiY2l0eSI6IlNlYXR0bGUiLCJzdGF0ZSI6IldBIiwiY291bnRyeUNvZGUiOiJVU0EiLCJnaWZ0QWRkcmVzcyI6ZmFsc2V9LCJhc3NvcnRtZW50Ijp7Im5vZGVJZCI6IjMwOTgiLCJkaXNwbGF5TmFtZSI6IkJlbGxldnVlIE5laWdoYm9yaG9vZCBNYXJrZXQiLCJhY2Nlc3NQb2ludHMiOm51bGwsInN1cHBvcnRlZEFjY2Vzc1R5cGVzIjpbXSwiaW50ZW50IjoiUElDS1VQIiwic2NoZWR1bGVFbmFibGVkIjpmYWxzZX0sImluc3RvcmUiOmZhbHNlLCJyZWZyZXNoQXQiOjE2NjA0NDk3MDI3OTMsInZhbGlkYXRlS2V5IjoicHJvZDp2Mjo5NzlhYWNkMy1jZTFlLTRiMGItOGEzYy1kNzI4NmUyYmEzMGIifQ%3D%3D; locGuestData=eyJpbnRlbnQiOiJTSElQUElORyIsImlzRXhwbGljaXQiOmZhbHNlLCJzdG9yZUludGVudCI6IlBJQ0tVUCIsIm1lcmdlRmxhZyI6ZmFsc2UsImlzRGVmYXVsdGVkIjpmYWxzZSwicGlja3VwIjp7Im5vZGVJZCI6IjMwOTgiLCJ0aW1lc3RhbXAiOjE2NjA0MjgxMDI3ODJ9LCJwb3N0YWxDb2RlIjp7InRpbWVzdGFtcCI6MTY2MDQyODEwMjc4MiwiYmFzZSI6Ijk4MTA5In0sInZhbGlkYXRlS2V5IjoicHJvZDp2Mjo5NzlhYWNkMy1jZTFlLTRiMGItOGEzYy1kNzI4NmUyYmEzMGIifQ%3D%3D; _astc=17106784aec13ceb9f7f56e853d3e22b; wmlh=605f07329326543945783f506b7bc67e2d7edb360aa0a19de005602f816d9f84; tb_sw_supported=true; _px3=458bfc2183896896eb2e5691613a5bc47397f2afba584d54d5a4009aba243593:oYUm6Yst+aWs0jeJbx7q95qjLvq1ylOYCakGNm99j2na2eYWyw5dI/PKTbXO3JuLypx7DWMKuFVnAytO5WwPeA==:1000:l39eR+lg9KPLwI9KotshZTp8OQBGRBny+pkwz9/+bir1Dq8alK+QkIp9Aslu15T2PWCS2mRnz6DW2WIYlvy40hrjMtKrqYnPRG6FwzAMSXlbJncK9clxYWhPJ/f2F7yB1VCjWug1MYHxmK55ymDwD2tyuTKmeLmtg9dXKyCo45VeXvvzUmDjiy0ce58IcfoqFjhRLMcw0MU3nRqRAGTesw==; akavpau_p2=1660429506~id=a9d389f31620e93dec1fc86c31a45525; com.wm.reflector=\"reflectorid:0000000000000000000000@lastupd:1660428907000@firstcreate:1660428100121\"; xptwg=234446872:23DEBDCBF6FBF00:5CC8A47:AFEC17BD:92B4E032:92A4B3BF:; TS012768cf=01bf728746d87b920173c3106c389fdcdc83c01d00963e0c114a166bd04b52e890282d9fb91fec7187f2c46036a0505fd6d4bcbf24; TS01a90220=01bf728746d87b920173c3106c389fdcdc83c01d00963e0c114a166bd04b52e890282d9fb91fec7187f2c46036a0505fd6d4bcbf24; TS2a5e0c5c027=08096cbb3cab200093650148a7e5bf599e7cab1f432ed2c06ac2757cec17ef649090efae25d6237b08d4a1bd62113000ace2cba864045cb807ef2f7cccf54f4d8143695d456e967a3c0d3204add8d66a76ae2c54747f4df87290a9939219011e; bm_sv=0363A9DCDC1CEFA0EF37C07A079CEA49~YAAQHHZiaOOEdZeCAQAAA3hGmRANfJe22Ci4MI9XtxvzuhQOhWpk9c/CNU+EAiKKAWd7iRSOxYo0iS4U9ISwqjAKMZ1Kx9ri8yP86HZSEaz/lFaBp4O8Pe8TVbA8P58SfbF13GHDSSejYahsT6kN5FZNfTbdDvNje5ecTjLNojQxJAhQocLK4QtUUKaMW3u4WcggmZCjEARvogZinySK6nHwbVPX8lnhutEQpgJ55uB/2y9a0kIRQ83zffb1UjvKsL4=~1");
//		httpConn.setRequestProperty("device_profile_ref_id", "AlN3Xyk-Y_qK1cDMtkr2Bmfwk7IH0CsVaGIx");
//		httpConn.setRequestProperty("is-variant-fetch", "false");
//		httpConn.setRequestProperty("origin", "https://www.walmart.com");
//		httpConn.setRequestProperty("pragma", "no-cache");
//
//		httpConn.setRequestProperty("referer", "https://www.walmart.com" + buyURL);
////		httpConn.setRequestProperty("referer", "https://www.walmart.com/ip/Degree-Men-Original-Antiperspirant-Deodorant-Cool-Rush-Mens-Deodorant-Stick-48-Hour-Odor-Protection-2-7-oz-2-Count/10898692?athbdg=L1200");
//		httpConn.setRequestProperty("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
//		httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
//		httpConn.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
//		httpConn.setRequestProperty("sec-fetch-dest", "empty");
//		httpConn.setRequestProperty("sec-fetch-mode", "cors");
//		httpConn.setRequestProperty("sec-fetch-site", "same-origin");
//		httpConn.setRequestProperty("traceparent", "3vSAQDBYCxa9HWfRmIq3909HNhivFyiMpTe-");
//		httpConn.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
//		httpConn.setRequestProperty("wm_mp", "true");
//		httpConn.setRequestProperty("wm_page_url", "https://www.walmart.com" + buyURL);
//		httpConn.setRequestProperty("wm_qos.correlation_id", "3vSAQDBYCxa9HWfRmIq3909HNhivFyiMpTe-");
//		httpConn.setRequestProperty("x-apollo-operation-name", "ItemById");
//		httpConn.setRequestProperty("x-enable-server-timing", "1");
//		httpConn.setRequestProperty("x-latency-trace", "1");
//		httpConn.setRequestProperty("x-o-bu", "WALMART-US");
//		httpConn.setRequestProperty("x-o-ccm", "server");
//		httpConn.setRequestProperty("x-o-correlation-id", "3vSAQDBYCxa9HWfRmIq3909HNhivFyiMpTe-");
//		httpConn.setRequestProperty("x-o-gql-query", "query ItemById");
//		httpConn.setRequestProperty("x-o-item-id", itemID);
//		httpConn.setRequestProperty("x-o-mart", "B2C");
//		httpConn.setRequestProperty("x-o-platform", "rweb");
//		httpConn.setRequestProperty("x-o-platform-version", "main-1.13.0-6550cc");
//		httpConn.setRequestProperty("x-o-segment", "oaoh");
//
//		httpConn.setDoOutput(true);
//		OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
//		writer.write("{\"query\":\"query ItemById( $iId:String! $vFId:String $pAdd:PostalAddress $sFId:[StoreFrontId]$p13N:P13NRequest $p13nCls:JSON $layout:[String]$tempo:JSON $pageType:String! $vTOP:JSON $sSId:Int $cSId:String $fBB:Boolean! $fBBAd:Boolean! $fMq:Boolean! $fSL:Boolean! $fSCar:Boolean! $fFit:Boolean! $fIdml:Boolean! $fRev:Boolean! $fulInt:String $fSeo:Boolean! $fP13:Boolean! $fAff:Boolean! $fDis:Boolean! $eItIb:Boolean! $fIlc:Boolean! ){contentLayout( channel:\\\"WWW\\\" pageType:$pageType tenant:\\\"WM_GLASS\\\" version:\\\"v1\\\" ){modules(p13n:$p13nCls tempo:$tempo virtualTryOnParams:$vTOP){configs{__typename...on EnricherModuleConfigsV1 @include(if:$fP13){zoneV1}...on TempoWM_GLASSWWWItemCompleteTheLookConfigsV1{title subTitle products{...CompleteTheLookProduct}p13nCtlColumn1{...CompleteTheLookColumn}p13nCtlColumn2{...CompleteTheLookColumn}p13nCtlColumn3{...CompleteTheLookColumn}}...on TempoWM_GLASSWWWBlitzConfigs{onlineBlitzMessageText1 onlineBlitzMessageText2 onlineBlitzMessageText3 inStoreBlitzMessageText1 inStoreBlitzMessageText2 inStoreBlitzMessageText3 pulses{pulseMessageLine1 pulseMessageLine2}}...on TempoWM_GLASSWWWItemCarouselConfigsV1 @include(if:$fP13){products{...ContentLayoutProduct}subTitle title type spBeaconInfo{adUuid moduleInfo pageViewUUID placement max}}...on TempoWM_GLASSWWWItemFitmentModuleConfigs @include(if:$fFit){fitment{partTypeID partTypeIDs result{status notes position formId quantityTitle resultSubTitle suggestions{id position loadIndex speedRating searchQueryParam labels{...FitmentLabel}fitmentSuggestionParams{id value}cat_id}extendedAttributes{...FitmentFieldFragment}labels{...FitmentLabel}}labels{...FitmentLabel}savedVehicle{...FitmentVehicleFragment}}}...on TempoWM_GLASSWWWItemRelatedShelvesConfigs @include(if:$fSeo){seoItemRelmData(id:$iId){relm{id url name}}}...on TempoWM_GLASSWWWWalmartPlusBannerConfigsV1{heading disclaimer ctaLink{linkText title clickThrough{type value}}isDownloadBanner}...on TempoWM_GLASSWWWWalmartPlusEarlyAccessBeforeEventConfigsV1{earlyAccessLogo{alt src}earlyAccessCardMesssage earlyAccessLink1{linkText clickThrough{value}}earlyAccessLink2{linkText clickThrough{value}}}...on TempoWM_GLASSWWWWalmartPlusEarlyAccessDuringEventConfigsV1{earlyAccessLogo{alt src}earlyAccessCardMesssage earlyAccessSubText earlyAccessLink1{linkText clickThrough{value}}}...on TempoWM_GLASSWWWProductWarrantyPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWGeneralWarningsPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWProductIndicationsPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWProductDescriptionPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWRefurbishedInfoPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWProductDirectionsPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWProductSpecificationsPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWNutritionValuePlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWReviewsPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWComparisonChartsPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWStyliticsModulePlaceholderConfigs{_rawConfigs}...on TempoWM_GLASSWWWAppDownloadPOVConfigs{heading ctaLink{linkText clickThrough{type value}}}...on TempoWM_GLASSWWWMarketingContentBtfConfigs{heading expandedOnPageLoad}...on TempoWM_GLASSWWWProductDescriptionPlaceholderConfigs{expandedOnPageLoad}...on TempoWM_GLASSWWWZeekitConfigs{zeekit{...Zeekit}}...CapitalOneBannerFragment...BrandBoxDisplayAdConfigsFragment @include(if:$fBB)...BuyBoxAdConfigsFragment @include(if:$fBBAd)...MarqueeDisplayAdConfigsFragment @include(if:$fMq)...SkylineDisplayAdConfigsFragment @include(if:$fSL)...SponsoredProductCarouselConfigsFragment @include(if:$fSCar)}moduleId matchedTrigger{pageType pageId zone inheritable}name type version status publishedDate}layouts(layout:$layout){id layout}pageMetadata{location{pickupStore deliveryStore intent postalCode stateOrProvinceCode city storeId accessType accessPointId spokeNodeId}pageContext p13nMetadata lazyModules}}seoItemMetaData(id:$iId){noIndex}product( catalogSellerId:$cSId itemId:$iId postalAddress:$pAdd storeFrontIds:$sFId selected:true semStoreId:$sSId p13N:$p13N variantFieldId:$vFId fulfillmentIntent:$fulInt ){...FullProductFragment}idml(itemId:$iId html:true) @include(if:$fIdml){...IDMLFragment}reviews(itemId:$iId) @include(if:$fRev){...FullReviewsFragment}}fragment CapitalOneBannerFragment on TempoWM_GLASSWWWCapitalOneBannerConfigsV1{bannerBackgroundColor primaryImage{alt src}bannerCta{ctaLink{linkText title clickThrough{value}uid}textColor}bannerText{text isBold isUnderlined underlinedColor textColor}}fragment FullProductFragment on Product{blitzItem giftingEligibility shipAsIs subscription{showSubscriptionModule subscriptionEligible subscriptionTransactable}subscriptionEligible showSubscriptionModule discounts @include(if:$fDis){discountedValue{priceString}discountMetaData{type savings{percent}unitPrice{priceString}}}rewards @include(if:$eItIb){eligible state minQuantity rewardAmt promotionId selectionToken cbOffer description term expiry}showFulfillmentLink additionalOfferCount legalRestriction shippingRestriction availabilityStatus averageRating suppressReviews brand brandUrl badges{...BadgesFragment}rhPath aaiaBrandId manufacturerProductId productTypeId tireSize tireLoadIndex tireSpeedRating viscosity model buyNowEligible earlyAccessEvent isEarlyAccessItem isWplusMember showBuyWithWplus preOrder{...PreorderFragment}ozarkAttributes{shippingPromise exactAddress}canonicalUrl catalogSellerId sellerReviewCount sellerAverageRating category{...ProductCategoryFragment}classType classId fulfillmentTitle shortDescription fulfillmentType fulfillmentBadge checkStoreAvailabilityATC checkAvailabilityGlobalDFS fulfillmentLabel{checkStoreAvailability wPlusFulfillmentText message shippingText fulfillmentText locationText fulfillmentMethod addressEligibility fulfillmentType postalCode}fulfillmentOptions @include(if:$fIlc){...on ShippingOptionV2{__typename type selected intent availableQuantity maxOrderQuantity orderLimit speedDetails{fulfillmentBadge deliveryDate fulfillmentPrice{price priceString}freeFulfillment wPlusEligible}locationText availabilityStatus inventoryStatus{fulfillmentBadge quantity}restricted}...on PickupOptionV2{__typename type selected intent availableQuantity maxOrderQuantity orderLimit speedDetails{fulfillmentBadge deliveryDate fulfillmentPrice{price priceString}freeFulfillment wPlusEligible}locationText availabilityStatus inventoryStatus{fulfillmentBadge quantity}productLocation{displayValue}addressLine checkStoreAvailability}...on DeliveryOptionV2{__typename type selected intent availableQuantity maxOrderQuantity orderLimit speedDetails{fulfillmentBadge deliveryDate fulfillmentPrice{price priceString}freeFulfillment wPlusEligible}locationText availabilityStatus}...on DigitalOptionV2{__typename type}}hasSellerBadge hasCarePlans hasHomeServices itemType id primaryUsItemId conditionType imageInfo{...ProductImageInfoFragment}location{postalCode stateOrProvinceCode city storeIds addressId pickupLocation{storeId accessPointId accessType}}manufacturerName name personalizable externalInfo{url}numberOfReviews orderMinLimit orderLimit weightIncrement offerId offerType priceInfo{priceDisplayCodes{...PriceDisplayCodesFragment}currentPrice{...ProductPriceFragment priceDisplay}wasPrice{...ProductPriceFragment}comparisonPrice{priceString}unitPrice{...ProductPriceFragment}savings{priceString}savingsAmount{priceString}subscriptionPrice{price priceString intervalFrequency duration percentageRate subscriptionString}priceRange{minPrice maxPrice priceString currencyUnit denominations{price priceString selected}}listPrice{...ProductPriceFragment}capType walmartFundedAmount{...ProductPriceFragment}}returnPolicy{returnable freeReturns returnWindow{value unitType}returnPolicyText}fsaEligibleInd sellerId sellerName sellerDisplayName secondaryOfferPrice{currentPrice{priceType priceString price}}semStoreData{pickupStoreId deliveryStoreId isSemLocationDifferent}shippingOption{...ShippingOptionFragment}type pickupOption{slaTier accessTypes availabilityStatus storeName storeId}salesUnit usItemId variantCriteria{id categoryTypeAllValues isFitPredictable isSizeChartApplicable name type variantList{availabilityStatus id images name products swatchImageUrl selected}}variants{...MinimalProductFragment}groupMetaData{groupComponents{quantity offerId componentType productDisplayName}}upc wfsEnabled sellerType ironbankCategory snapEligible promoData @include(if:$fAff){id description terms type templateData{priceString imageUrl aprString}noInterestInstallmentsPromotion{bank installments payments currencyAmount currencyUnit}}showAddOnServices addOnServices{serviceType serviceTitle serviceSubTitle groups{groupType groupTitle assetUrl shortDescription unavailabilityReason services{name displayName offerId usItemId selectedDisplayName serviceMetaData currentPrice{price priceString}giftEligible}}}productLocation{displayValue}zeekitData{...Zeekit}experienceType}fragment Zeekit on ZeekitEntity{color abstractProductId zeekit{numberOfSimulatedImages isProductEnabledInZeekit simulatedImages{id url zoomable}personaDetails{id size height firstName bust waist hips inseam}i18n{labels{name value}}}}fragment BadgesFragment on UnifiedBadge{flags{__typename...on BaseBadge{id text key query type}...on PreviouslyPurchasedBadge{id text key lastBoughtOn criteria{name value}}}labels{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key lastBoughtOn}}tags{__typename...on BaseBadge{id text key}}}fragment ShippingOptionFragment on ShippingOption{availabilityStatus slaTier deliveryDate maxDeliveryDate shipMethod shipPrice{...ProductPriceFragment}}fragment ProductCategoryFragment on ProductCategory{categoryPathId path{name url}}fragment PreorderFragment on PreOrder{isPreOrder}fragment MinimalProductFragment on Variant{availabilityStatus discounts{discountedValue{priceString}}subscriptionEligible imageInfo{...ProductImageInfoFragment}priceInfo{priceDisplayCodes{...PriceDisplayCodesFragment}currentPrice{...ProductPriceFragment}wasPrice{...ProductPriceFragment}unitPrice{...ProductPriceFragment}listPrice{...ProductPriceFragment}}productUrl usItemId id:productId fulfillmentBadge}fragment ProductImageInfoFragment on ProductImageInfo{allImages{id url zoomable}thumbnailUrl}fragment PriceDisplayCodesFragment on PriceDisplayCodes{clearance eligibleForAssociateDiscount finalCostByWeight priceDisplayCondition reducedPrice rollback submapType}fragment ProductPriceFragment on ProductPrice{price priceString variantPriceString currencyUnit}fragment NutrientFragment on Nutrient{name amount dvp childNutrients{name amount dvp}}fragment NutritionAttributeFragment on NutritionAttribute{name mainNutrient{...NutrientFragment}childNutrients{...NutrientFragment childNutrients{...NutrientFragment}}}fragment IdmlAttributeFragment on IdmlAttribute{name value attribute}fragment ServingAttributeFragment on ServingAttribute{name values{...IdmlAttributeFragment values{...IdmlAttributeFragment}}}fragment IDMLFragment on Idml{sizeCharts{id tagDisplay url}chokingHazards{...LegalContentFragment}directions{name value}indications{name value}ingredients{activeIngredientName{name value}activeIngredients{name value}inactiveIngredients{name value}ingredients{name value}}longDescription shortDescription interactiveProductVideo specifications{name value}warnings{name value}warranty{information length}esrbRating mpaaRating nutritionFacts{calorieInfo{...NutritionAttributeFragment}keyNutrients{name values{...NutritionAttributeFragment}}vitaminMinerals{...NutritionAttributeFragment}servingInfo{...ServingAttributeFragment}additionalDisclaimer{...IdmlAttributeFragment values{...IdmlAttributeFragment values{...IdmlAttributeFragment}}}staticContent{...IdmlAttributeFragment values{...IdmlAttributeFragment values{...IdmlAttributeFragment}}}}videos{poster title versions{small large}}product360ImageContainer{name url}marketingDescription{name value}}fragment FullReviewsFragment on ProductReviews{averageOverallRating aspects{id name score snippetCount}lookupId customerReviews{...CustomerReviewsFragment}ratingValueFiveCount ratingValueFourCount ratingValueOneCount ratingValueThreeCount ratingValueTwoCount roundedAverageOverallRating topNegativeReview{rating reviewSubmissionTime userNickname negativeFeedback positiveFeedback reviewText reviewTitle badges{badgeType id contentType glassBadge{id text}}syndicationSource{logoImageUrl contentLink name}}topPositiveReview{rating reviewSubmissionTime userNickname negativeFeedback positiveFeedback reviewText reviewTitle badges{badgeType id contentType glassBadge{id text}}syndicationSource{logoImageUrl contentLink name}}totalReviewCount}fragment LegalContentFragment on LegalContent{ageRestriction headline headline image mature message}fragment CustomerReviewsFragment on CustomerReview{rating reviewSubmissionTime reviewText reviewTitle userNickname photos{caption id sizes{normal{...ReviewPhotoSizeFragment}thumbnail{...ReviewPhotoSizeFragment}}}badges{badgeType id contentType glassBadge{id text}}syndicationSource{logoImageUrl contentLink name}}fragment FitmentLabel on FitmentLabels{links{...FitmentLabelEntity}messages{...FitmentLabelEntity}ctas{...FitmentLabelEntity}images{...FitmentLabelEntity}}fragment FitmentVehicleFragment on FitmentVehicle{vehicleType{...FitmentVehicleFieldFragment}vehicleYear{...FitmentVehicleFieldFragment}vehicleMake{...FitmentVehicleFieldFragment}vehicleModel{...FitmentVehicleFieldFragment}additionalAttributes{...FitmentVehicleFieldFragment}}fragment FitmentVehicleFieldFragment on FitmentVehicleField{id value label}fragment FitmentFieldFragment on FitmentField{id value displayName data{value label}extended dependsOn}fragment FitmentLabelEntity on FitmentLabelEntity{id label}fragment ReviewPhotoSizeFragment on ReviewPhotoSize{id url}fragment CompleteTheLookColumn on TempoWM_GLASSWWWP13nCtlColumn{isAnchor cellSpan index}fragment CompleteTheLookProduct on Product{name brand catalogSellerId canonicalUrl availabilityStatus externalInfo{url}id imageInfo{thumbnailUrl}priceInfo{currentPrice{...ProductPriceFragment}}usItemId}fragment ContentLayoutProduct on Product{name badges{...BadgesFragment}canonicalUrl classType availabilityStatus showAtc personalizable externalInfo{url}averageRating fulfillmentBadge fulfillmentSpeed fulfillmentTitle fulfillmentType itemType groupMetaData{groupType groupSubType numberOfComponents groupComponents{quantity offerId componentType productDisplayName}}imageInfo{thumbnailUrl}numberOfReviews offerId orderMinLimit orderLimit weightIncrement p13nDataV1{predictedQuantity flags{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}}previouslyPurchased{label}preOrder{...PreorderFragment}priceInfo{currentPrice{...ProductPriceFragment priceDisplay}listPrice{...ProductPriceFragment}subscriptionPrice{priceString}priceDisplayCodes{clearance eligibleForAssociateDiscount finalCostByWeight priceDisplayCondition reducedPrice rollback submapType}priceRange{minPrice maxPrice priceString}unitPrice{...ProductPriceFragment}wasPrice{...ProductPriceFragment}comparisonPrice{...ProductPriceFragment}savingsAmount{priceString}capType walmartFundedAmount{...ProductPriceFragment}}rhPath salesUnit sellerId sellerName hasSellerBadge seller{name sellerId}shippingOption{slaTier shipMethod}showOptions snapEligible sponsoredProduct{spQs clickBeacon spTags}usItemId variantCount variantCriteria{name id variantList{name swatchImageUrl selectedProduct{usItemId canonicalUrl}}}}fragment BrandBoxDisplayAdConfigsFragment on TempoWM_GLASSWWWBrandBoxDisplayAdConfigs{_rawConfigs}fragment BuyBoxAdConfigsFragment on TempoWM_GLASSWWWBuyBoxAdConfigs{_rawConfigs moduleLocation lazy ad{...SponsoredProductsAdFragment}}fragment MarqueeDisplayAdConfigsFragment on TempoWM_GLASSWWWMarqueeDisplayAdConfigs{_rawConfigs ad{...DisplayAdFragment}}fragment DisplayAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataDisplayAdFragment}}}fragment AdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment AdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment SkylineDisplayAdConfigsFragment on TempoWM_GLASSWWWSkylineDisplayAdConfigs{_rawConfigs ad{...SkylineDisplayAdFragment}}fragment SkylineDisplayAdFragment on Ad{...SkylineAdFragment adContent{type data{__typename...SkylineAdDataDisplayAdFragment}}}fragment SkylineAdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment SkylineAdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment SponsoredProductCarouselConfigsFragment on TempoWM_GLASSWWWSponsoredProductCarouselConfigs{_rawConfigs moduleType ad{...SponsoredProductsAdFragment}}fragment SponsoredProductsAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataSponsoredProductsFragment}}}fragment AdDataSponsoredProductsFragment on AdData{...on SponsoredProducts{adUuid adExpInfo moduleInfo products{...ProductFragment}}}fragment ProductFragment on Product{usItemId offerId badges{flags{__typename...on BaseBadge{id text key query type}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought criteria{name value}}}labels{__typename...on BaseBadge{id text key}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{__typename...on BaseBadge{id text key}}}priceInfo{priceDisplayCodes{rollback reducedPrice eligibleForAssociateDiscount clearance strikethrough submapType priceDisplayCondition unitOfMeasure pricePerUnitUom}currentPrice{price priceString}wasPrice{price priceString}priceRange{minPrice maxPrice priceString}unitPrice{price priceString}}showOptions sponsoredProduct{spQs clickBeacon spTags}canonicalUrl numberOfReviews averageRating availabilityStatus imageInfo{thumbnailUrl allImages{id url}}name fulfillmentBadge classType type showAtc p13nData{predictedQuantity flags{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}labels{PREVIOUSLY_PURCHASED{text}CUSTOMERS_PICK{text}}}}\",\"variables\":{\"channel\":\"WWW\",\"pageType\":\"ItemPageGlobal\",\"tenant\":\"WM_GLASS\",\"version\":\"v1\",\"iId\":\"" + itemID + "\",\"layout\":[\"itemDesktop\"],\"tempo\":{\"targeting\":\"%7B%22userState%22%3A%22loggedIn%22%7D\",\"params\":[{\"key\":\"expoVars\",\"value\":\"expoVariationValue\"},{\"key\":\"expoVars\",\"value\":\"expoVariationValue2\"}]},\"p13N\":{\"userClientInfo\":{\"isZipLocated\":true,\"deviceType\":\"desktop\"},\"userReqInfo\":{\"refererContext\":{\"source\":\"itempage\",\"catId\":\"\",\"facet\":\"\",\"query\":\"deodorant\"},\"pageUrl\":\"" + buyURL + "?athbdg=L1200\"}},\"p13nCls\":{\"pageId\":\"" + itemID + "\",\"userClientInfo\":{\"isZipLocated\":true},\"userReqInfo\":{\"refererContext\":{\"source\":\"itempage\"}},\"p13NCallType\":\"ATF\"},\"fBBAd\":true,\"fSL\":true,\"fIdml\":true,\"fRev\":true,\"fFit\":true,\"fSeo\":true,\"fP13\":true,\"fAff\":true,\"fMq\":true,\"fSCar\":true,\"fBB\":true,\"fDis\":false,\"eItIb\":true,\"fIlc\":false}}");
//		writer.flush();
//		writer.close();
//		httpConn.getOutputStream().close();
//
//		InputStream responseStream = httpConn.getResponseCode() / 100 == 2
//				? httpConn.getInputStream()
//				: httpConn.getErrorStream();
//		Scanner s = new Scanner(responseStream).useDelimiter("\\A");
//		String response = s.hasNext() ? s.next() : "";
//		JsonElement jsonElement = new JsonParser().parse(response);
//		Gson gson = new GsonBuilder().setPrettyPrinting().create();
//		String json = gson.toJson(jsonElement);
//		System.out.println(json);
    }

    public static HashMap<String, HashMap> walmartBabyFormula() throws IOException {

        HashMap<String, HashMap> products = new HashMap<>();
//        String response = "";
//        String store_id = "3098"; // Bellevue, WA
//        String store_id = "2774"; // Dublin, OH
//        String store_id = "2137"; // Durham, NC
//        String store_id = "4177"; // Lincolnwood, IL
        String store_id = "3795"; // North Bergen, NJ

        URL url = new URL("https://www.walmart.com/orchestra/home/graphql/browse?affinityOverride=default&page=1&prg=desktop&catId=5427_133283_4720344&sort=best_match&ps=40&searchArgs.cat_id=5427_133283_4720344&searchArgs.prg=desktop&fitmentFieldParams=true&fetchMarquee=true&fetchSkyline=true&fetchSbaTop=false&enablePortableFacets=true&tenant=WM_GLASS&enableFacetCount=true&marketSpecificParams=undefined");
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("POST");

        httpConn.setRequestProperty("authority", "www.walmart.com");
        httpConn.setRequestProperty("accept", "application/json");
        httpConn.setRequestProperty("accept-language", "en-US,en;q=0.9");
        httpConn.setRequestProperty("cache-control", "no-cache");
        httpConn.setRequestProperty("content-type", "application/json");
        httpConn.setRequestProperty("cookie", "auth=MTAyOTYyMDE4rW%2B7H602S2b6n1WayljgUiMWILgXp0sMGo3xLmp1GcLGyZvl%2FV%2BqOb%2B8Imq5KcWZr9dc4qpXnjdnXf6XzxT4WjEEN1Au8PQAD7wuTu7QqJ9lzQKPU%2Fg2hpJm5OAIYat6767wuZloTfhm7Wk2Kcjygv3M5Jnvc7ePkiG6%2BkglNABvrRNtdllI6HdrStY6VRzw7ZkH6rBvCROUtbdMQ8gkz42PfNz5Ol9qQf8LXIM0E3EUMk70P8glgOEpLOprhDfMM%2FFHGZ2dCNmxWrdkwqEKrveQRy%2B5k1OODgi6%2BbP7dZee6c5U%2FtP9JfbHBXEJTeo3AB0LijKst1AYYjfYwTpOTXgT9EO6FQkGtNwSN42wkh3IhJoc67KGKL5ZMkquUDC2vGgFha9o91ueqA9QpQS6ekjyrOXbKKhH072NS%2FW0j%2FU%3D; ACID=d8dcccf2-3479-4abb-89db-a0223b6c4ff1; hasACID=true; assortmentStoreId=" + store_id + "; hasLocData=1; TB_Latency_Tracker_100=1; TB_Navigation_Preload_01=1; TB_SFOU-100=; vtc=RDshC1kugEE82Hsc2iCV6k; bstc=RDshC1kugEE82Hsc2iCV6k; mobileweb=0; xpa=0NcOK|0t4gT|1A0pE|2Oqb6|2SWkj|2t453|5_9FA|927zv|AQhLM|AYraE|Af1yH|CN28l|HDfyl|IouYg|LTD5Y|Nnski|P_fCM|PhqzS|PzQ_l|RE_dK|RZlxg|XBPw9|bcl64|ccDGr|g8z9_|iT-9R|ibBir|lqVt_|nzyw-|odFJG|pGAhC|qyn67|sbXp_|uJQh6|w_GEw|yxNJ6|zCylr; exp-ck=0NcOK10t4gT12SWkj12t45315_9FA2927zv1AQhLM1Af1yH1HDfyl2IouYg2Nnski1PzQ_l1XBPw91ccDGr1g8z9_1ibBir1nzyw-1odFJG1pGAhC1qyn672w_GEw1yxNJ65; _pxhd=507c57269a4e3c903ec9533b76cd420f6cb910847e67aa447d95562da395a500:e3b8065b-1d91-11ed-9587-595449755955; adblocked=false; TBV=7; xpm=1%2B1660674794%2BRDshC1kugEE82Hsc2iCV6k~%2B0; pxcts=e4b9a181-1d91-11ed-bfc3-6d7350527a4e; _pxvid=e3b8065b-1d91-11ed-9587-595449755955; ak_bmsc=BA90273CE586D373BA8416BFE4591535~000000000000000000000000000000~YAAQT40duGoR3GOCAQAAp2zupxDNABf70Dv3XoJxdTt6rst/CgXv5CIlkk4Xas3u8XZ29uPe4g+ZP7frwCD/xofQusk7nhFuCCkqj6tV6a5ai+gbVq53gURc9GXkAgh8ojZtor2g+X0t6Gc3Mu6ApGyWa/8IlD+eeO4NgQkJfvtxK8us99RdfkpZ97srV9S7qa1bLpIcsqb9lp3bEEYf937sW/cYQDldQrZ1GSCflNSgoqixBjkQ3RgdO7LwIzfa4aiVRRQ8Wbv0lVCU6wNWqZjx3BnE1H4MnzKinNcmxVyRcveExN5SFZFtFOmzNV4b7xSrRtKh3AAZsanzx4Pczd3UqDDENB8Kbk9J8G91Fmo/2jvA/jHNpwdagqh6KB2m3cxHfV0IeJhsB/WqFXK89NVGr7iKpR5DeV6cL2cspWYQh6v3Ipo89t4oe8f5W3aWCJnfeBy8ZhApmopa9JuqUNMerR9eHMNyTG1UT2TFZHHJs0QQTSwFWyPwEyQ+; locDataV3=eyJpc0RlZmF1bHRlZCI6ZmFsc2UsImlzRXhwbGljaXQiOmZhbHNlLCJpbnRlbnQiOiJTSElQUElORyIsInBpY2t1cCI6W3siYnVJZCI6IjAiLCJub2RlSWQiOiIyNTE2IiwiZGlzcGxheU5hbWUiOiJSZW50b24gU3VwZXJjZW50ZXIiLCJub2RlVHlwZSI6IlNUT1JFIiwiYWRkcmVzcyI6eyJwb3N0YWxDb2RlIjoiOTgwNTciLCJhZGRyZXNzTGluZTEiOiI3NDMgUmFpbmllciBBdmVudWUgU291dGgiLCJjaXR5IjoiUmVudG9uIiwic3RhdGUiOiJXQSIsImNvdW50cnkiOiJVUyIsInBvc3RhbENvZGU5IjoiOTgwNTctMzIwNCJ9LCJnZW9Qb2ludCI6eyJsYXRpdHVkZSI6NDcuNDcyMTUzLCJsb25naXR1ZGUiOi0xMjIuMjIxMDg4fSwiaXNHbGFzc0VuYWJsZWQiOnRydWUsInNjaGVkdWxlZEVuYWJsZWQiOnRydWUsInVuU2NoZWR1bGVkRW5hYmxlZCI6dHJ1ZSwiaHViTm9kZUlkIjoiMjUxNiIsInN0b3JlSHJzIjoiMDY6MDAtMjM6MDAiLCJzdXBwb3J0ZWRBY2Nlc3NUeXBlcyI6WyJQSUNLVVBfQ1VSQlNJREUiLCJCQUtFUllfUElDS1VQIiwiUElDS1VQX0lOU1RPUkUiLCJQSUNLVVBfU1BFQ0lBTF9FVkVOVCJdfV0sInNoaXBwaW5nQWRkcmVzcyI6eyJsYXRpdHVkZSI6NDcuNTc0MywibG9uZ2l0dWRlIjotMTIyLjM5NDIsInBvc3RhbENvZGUiOiI5ODExNiIsImNpdHkiOiJTZWF0dGxlIiwic3RhdGUiOiJXQSIsImNvdW50cnlDb2RlIjoiVVNBIiwiZ2lmdEFkZHJlc3MiOmZhbHNlfSwiYXNzb3J0bWVudCI6eyJub2RlSWQiOiIyNTE2IiwiZGlzcGxheU5hbWUiOiJSZW50b24gU3VwZXJjZW50ZXIiLCJhY2Nlc3NQb2ludHMiOm51bGwsInN1cHBvcnRlZEFjY2Vzc1R5cGVzIjpbXSwiaW50ZW50IjoiUElDS1VQIiwic2NoZWR1bGVFbmFibGVkIjpmYWxzZX0sImluc3RvcmUiOmZhbHNlLCJyZWZyZXNoQXQiOjE2NjA2OTYzOTYzNDYsInZhbGlkYXRlS2V5IjoicHJvZDp2MjpkOGRjY2NmMi0zNDc5LTRhYmItODlkYi1hMDIyM2I2YzRmZjEifQ%3D%3D; locGuestData=eyJpbnRlbnQiOiJTSElQUElORyIsImlzRXhwbGljaXQiOmZhbHNlLCJzdG9yZUludGVudCI6IlBJQ0tVUCIsIm1lcmdlRmxhZyI6ZmFsc2UsImlzRGVmYXVsdGVkIjpmYWxzZSwicGlja3VwIjp7Im5vZGVJZCI6IjI1MTYiLCJ0aW1lc3RhbXAiOjE2NjA2NzQ3OTYzMzR9LCJwb3N0YWxDb2RlIjp7InRpbWVzdGFtcCI6MTY2MDY3NDc5NjMzNCwiYmFzZSI6Ijk4MTE2In0sInZhbGlkYXRlS2V5IjoicHJvZDp2MjpkOGRjY2NmMi0zNDc5LTRhYmItODlkYi1hMDIyM2I2YzRmZjEifQ%3D%3D; _astc=a90c415f4aa683a6946d96c09b6578e6; wmlh=d4831d3c66d3a76c555ec3826b29dac4556990c5a6188d6c05b7f34ff1192735; dimensionData=821; akavpau_p1=1660675487~id=f8da8fb64aa0512d1e760306e14fb31b; AID=wmlspartner%253D0%253Areflectorid%253D0000000000000000000000%253Alastupd%253D1660675078985; akavpau_p2=1660676085~id=fda97dc5adc3a4a2b13f551048666ba0; com.wm.reflector=\"reflectorid:0000000000000000000000@lastupd:1660675486000@firstcreate:1660674794664\"; xptwg=2430688244:15CE3C771A06F40:386535F:DBD28C04:28BFB2FB:458CABED:; TS012768cf=01e92ffd082cdafda16814c9fbbd1fe2722d6afd7717862faf1fe8e39186c957f457c8376ffccba92b979020b117d18c21a7c4f7db; TS01a90220=01e92ffd082cdafda16814c9fbbd1fe2722d6afd7717862faf1fe8e39186c957f457c8376ffccba92b979020b117d18c21a7c4f7db; TS2a5e0c5c027=08074ac0edab200034c6372d1a05eaa49033c01be9677251845a3fcebd121c8f5dfd74ea5ee18ad308c5504390113000d2a1395d4ea330ff9c1863f161fabc33cdd6fe26277ff0718127049c3e06490457b0c26dc389b97bd5ad22406087aec7; bm_sv=B4E13460496FE332B223C78418ADC951~YAAQfY0duBNW6ZKCAQAAF/T4pxAVFK+frrstrmL3BsHVqc9iRYpt3QtrUP78b5xl5TQ+WRlNOWZpsSmssokTvhSG8UuD0+AmKcMy93dsjHIiM/oUW4utUWtbB+k3R6WoC2V2ppOOItNtsc0/WzV37XJ2zSUyJT7GEASrOZ4qD7xsiIKxhT4jmhZfzqAYdQFP3PU1nEAmQ0Z0kZxCjAIGhBBuIc7rylOgdz8Ol7+UKxH21wu8JsujBZG8H/ELPKLZ0r8=~1");
        httpConn.setRequestProperty("device_profile_ref_id", "kx7a34qtoaQFLeCId5v6irlUoepE6-ND6DDY");
        httpConn.setRequestProperty("origin", "https://www.walmart.com");
        httpConn.setRequestProperty("pragma", "no-cache");
        httpConn.setRequestProperty("referer", "https://www.walmart.com/browse/feeding/baby-formula/5427_133283_4720344?redirectQuery=baby+formula&search_redirect=true&affinityOverride=default");
        httpConn.setRequestProperty("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
        httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
        httpConn.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
        httpConn.setRequestProperty("sec-fetch-dest", "empty");
        httpConn.setRequestProperty("sec-fetch-mode", "cors");
        httpConn.setRequestProperty("sec-fetch-site", "same-origin");
        httpConn.setRequestProperty("traceparent", "I2AapAK20LEN0wfz8P3DA4dfnnTqQ79FXfSw");
        httpConn.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");
        httpConn.setRequestProperty("wm_mp", "true");
        httpConn.setRequestProperty("wm_page_url", "https://www.walmart.com/browse/feeding/baby-formula/5427_133283_4720344?redirectQuery=baby+formula&search_redirect=true&affinityOverride=default");
        httpConn.setRequestProperty("wm_qos.correlation_id", "I2AapAK20LEN0wfz8P3DA4dfnnTqQ79FXfSw");
        httpConn.setRequestProperty("x-apollo-operation-name", "Browse");
        httpConn.setRequestProperty("x-enable-server-timing", "1");
        httpConn.setRequestProperty("x-latency-trace", "1");
        httpConn.setRequestProperty("x-o-bu", "WALMART-US");
        httpConn.setRequestProperty("x-o-ccm", "server");
        httpConn.setRequestProperty("x-o-correlation-id", "I2AapAK20LEN0wfz8P3DA4dfnnTqQ79FXfSw");
        httpConn.setRequestProperty("x-o-gql-query", "query Browse");
        httpConn.setRequestProperty("x-o-mart", "B2C");
        httpConn.setRequestProperty("x-o-platform", "rweb");
        httpConn.setRequestProperty("x-o-platform-version", "main-1.13.0-26e552");
        httpConn.setRequestProperty("x-o-segment", "oaoh");

        httpConn.setDoOutput(true);
        OutputStreamWriter writer = new OutputStreamWriter(httpConn.getOutputStream());
        writer.write("{\"query\":\"query Browse( $query:String $page:Int $prg:Prg! $facet:String $sort:Sort $catId:String! $max_price:String $min_price:String $module_search:String $affinityOverride:AffinityOverride $ps:Int $ptss:String $beShelfId:String $fitmentFieldParams:JSON ={}$fitmentSearchParams:JSON ={}$rawFacet:String $seoPath:String $trsp:String $fetchMarquee:Boolean! $fetchSkyline:Boolean! $additionalQueryParams:JSON ={}$enablePortableFacets:Boolean = false $intentSource:IntentSource $tenant:String! $enableFacetCount:Boolean = true $pageType:String! = \\\"BrowsePage\\\" $marketSpecificParams:String ){search( query:$query page:$page prg:$prg facet:$facet sort:$sort cat_id:$catId max_price:$max_price min_price:$min_price module_search:$module_search affinityOverride:$affinityOverride additionalQueryParams:$additionalQueryParams ps:$ps ptss:$ptss trsp:$trsp intentSource:$intentSource _be_shelf_id:$beShelfId pageType:$pageType ){query searchResult{...BrowseResultFragment}}contentLayout( channel:\\\"WWW\\\" pageType:$pageType tenant:$tenant version:\\\"v1\\\" searchArgs:{query:$query cat_id:$catId _be_shelf_id:$beShelfId prg:$prg}){modules{...ModuleFragment configs{...on EnricherModuleConfigsV1{zoneV1}__typename...on _TempoWM_GLASSWWWSearchSortFilterModuleConfigs{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}}...on TempoWM_GLASSWWWPillsModuleConfigs{moduleSource pillsV2{...PillsModuleFragment}}...TileTakeOverProductFragment...on TempoWM_GLASSWWWSearchFitmentModuleConfigs{fitments( fitmentSearchParams:$fitmentSearchParams fitmentFieldParams:$fitmentFieldParams ){...FitmentFragment sisFitmentResponse{...BrowseResultFragment}}}...on TempoWM_GLASSWWWStoreSelectionHeaderConfigs{fulfillmentMethodLabel storeDislayName}...on TempoWM_GLASSWWWSponsoredProductCarouselConfigs{_rawConfigs}...PopularInModuleFragment...CopyBlockModuleFragment...BannerModuleFragment...HeroPOVModuleFragment...InlineSearchModuleFragment...MarqueeDisplayAdConfigsFragment @include(if:$fetchMarquee)...SkylineDisplayAdConfigsFragment @include(if:$fetchSkyline)...HorizontalChipModuleConfigsFragment...SkinnyBannerFragment}}...LayoutFragment pageMetadata{location{pickupStore deliveryStore intent postalCode stateOrProvinceCode city storeId accessPointId accessType spokeNodeId}pageContext}}seoBrowseMetaData( id:$catId facets:$rawFacet path:$seoPath facet_query_param:$facet _be_shelf_id:$beShelfId marketSpecificParams:$marketSpecificParams ){metaTitle metaDesc metaCanon h1 noIndex}}fragment BrowseResultFragment on SearchInterface{title aggregatedCount...BreadCrumbFragment...ShelfDataFragment...DebugFragment...ItemStacksFragment...PageMetaDataFragment...PaginationFragment...RequestContextFragment...ErrorResponse modules{facetsV1 @skip(if:$enablePortableFacets){...FacetFragment}topNavFacets @include(if:$enablePortableFacets){...FacetFragment}allSortAndFilterFacets @include(if:$enablePortableFacets){...FacetFragment}pills{...PillsModuleFragment}}}fragment ModuleFragment on TempoModule{name version type moduleId schedule{priority}matchedTrigger{zone}}fragment LayoutFragment on ContentLayout{layouts{id layout}}fragment BreadCrumbFragment on SearchInterface{breadCrumb{id name url}}fragment ShelfDataFragment on SearchInterface{shelfData{shelfName shelfId}}fragment DebugFragment on SearchInterface{debug{sisUrl adsUrl}}fragment ItemStacksFragment on SearchInterface{itemStacks{displayMessage meta{adsBeacon{adUuid moduleInfo max_ads}query stackId stackType title layoutEnum totalItemCount totalItemCountDisplay viewAllParams{query cat_id sort facet affinityOverride recall_set min_price max_price}}itemsV2{...ItemFragment...InGridMarqueeAdFragment...TileTakeOverTileFragment}}}fragment ItemFragment on Product{__typename id usItemId fitmentLabel name checkStoreAvailabilityATC seeShippingEligibility brand type shortDescription weightIncrement imageInfo{...ProductImageInfoFragment}canonicalUrl externalInfo{url}itemType category{path{name url}}badges{flags{...on BaseBadge{key text type id}...on PreviouslyPurchasedBadge{id text key lastBoughtOn numBought}}tags{...on BaseBadge{key text type}}}classType averageRating numberOfReviews esrb mediaRating salesUnitType sellerId sellerName hasSellerBadge availabilityStatusV2{display value}groupMetaData{groupType groupSubType numberOfComponents groupComponents{quantity offerId componentType productDisplayName}}productLocation{displayValue aisle{zone aisle}}fulfillmentSpeed offerId preOrder{...PreorderFragment}priceInfo{...ProductPriceInfoFragment}variantCriteria{...VariantCriteriaFragment}snapEligible fulfillmentBadge fulfillmentTitle fulfillmentType brand manufacturerName showAtc sponsoredProduct{spQs clickBeacon spTags viewBeacon}showOptions showBuyNow rewards{eligible state minQuantity rewardAmt promotionId selectionToken cbOffer term expiry description}}fragment ProductImageInfoFragment on ProductImageInfo{thumbnailUrl size}fragment ProductPriceInfoFragment on ProductPriceInfo{priceRange{minPrice maxPrice}currentPrice{...ProductPriceFragment}comparisonPrice{...ProductPriceFragment}wasPrice{...ProductPriceFragment}unitPrice{...ProductPriceFragment}listPrice{...ProductPriceFragment}savingsAmount{...ProductSavingsFragment}shipPrice{...ProductPriceFragment}subscriptionPrice{priceString subscriptionString}priceDisplayCodes{priceDisplayCondition finalCostByWeight submapType}}fragment PreorderFragment on PreOrder{isPreOrder preOrderMessage preOrderStreetDateMessage}fragment ProductPriceFragment on ProductPrice{price priceString variantPriceString priceType currencyUnit priceDisplay}fragment ProductSavingsFragment on ProductSavings{amount percent priceString}fragment VariantCriteriaFragment on VariantCriterion{name type id isVariantTypeSwatch variantList{id images name rank swatchImageUrl availabilityStatus products selectedProduct{canonicalUrl usItemId}}}fragment InGridMarqueeAdFragment on MarqueePlaceholder{__typename type moduleLocation lazy}fragment TileTakeOverTileFragment on TileTakeOverProductPlaceholder{__typename type tileTakeOverTile{span title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}fragment PageMetaDataFragment on SearchInterface{pageMetadata{storeSelectionHeader{fulfillmentMethodLabel storeDislayName}title canonical description location{addressId}}}fragment PaginationFragment on SearchInterface{paginationV2{maxPage pageProperties}}fragment RequestContextFragment on SearchInterface{requestContext{vertical isFitmentFilterQueryApplied searchMatchType categories{id name}}}fragment ErrorResponse on SearchInterface{errorResponse{correlationId source errorCodes errors{errorType statusCode statusMsg source}}}fragment PillsModuleFragment on PillsSearchInterface{title url image:imageV1{src alt}}fragment BannerModuleFragment on TempoWM_GLASSWWWSearchBannerConfigs{moduleType viewConfig{title image imageAlt displayName description url urlAlt appStoreLink appStoreLinkAlt playStoreLink playStoreLinkAlt}}fragment PopularInModuleFragment on TempoWM_GLASSWWWPopularInBrowseConfigs{seoBrowseRelmData(id:$catId){relm{id name url}}}fragment CopyBlockModuleFragment on TempoWM_GLASSWWWCopyBlockConfigs{copyBlock(id:$catId marketSpecificParams:$marketSpecificParams){cwc}}fragment FacetFragment on Facet{title name type layout min max selectedMin selectedMax unboundedMax stepSize isSelected values{id title name description type itemCount @include(if:$enableFacetCount) isSelected baseSeoURL}}fragment FitmentFragment on Fitments{partTypeIDs result{status formId position quantityTitle extendedAttributes{...FitmentFieldFragment}labels{...LabelFragment}resultSubTitle notes suggestions{...FitmentSuggestionFragment}}labels{...LabelFragment}savedVehicle{vehicleType{...VehicleFieldFragment}vehicleYear{...VehicleFieldFragment}vehicleMake{...VehicleFieldFragment}vehicleModel{...VehicleFieldFragment}additionalAttributes{...VehicleFieldFragment}}fitmentFields{...VehicleFieldFragment}fitmentForms{id fields{...FitmentFieldFragment}title labels{...LabelFragment}}}fragment LabelFragment on FitmentLabels{ctas{...FitmentLabelEntityFragment}messages{...FitmentLabelEntityFragment}links{...FitmentLabelEntityFragment}images{...FitmentLabelEntityFragment}}fragment FitmentLabelEntityFragment on FitmentLabelEntity{id label}fragment VehicleFieldFragment on FitmentVehicleField{id label value}fragment FitmentFieldFragment on FitmentField{id displayName value extended data{value label}dependsOn}fragment FitmentSuggestionFragment on FitmentSuggestion{id position loadIndex speedRating searchQueryParam labels{...LabelFragment}cat_id fitmentSuggestionParams{id value}}fragment HeroPOVModuleFragment on TempoWM_GLASSWWWHeroPovConfigsV1{povCards{card{povStyle image{mobileImage{...TempoCommonImageFragment}desktopImage{...TempoCommonImageFragment}}heading{text textColor textSize}subheading{text textColor}detailsView{backgroundColor isTransparent}ctaButton{button{linkText clickThrough{value}uid}}legalDisclosure{regularText shortenedText textColor textColorMobile legalBottomSheetTitle legalBottomSheetDescription}logo{...TempoCommonImageFragment}links{link{linkText}}}}}fragment TempoCommonImageFragment on TempoCommonImage{src alt assetId uid clickThrough{value}}fragment InlineSearchModuleFragment on TempoWM_GLASSWWWInlineSearchConfigs{headingText placeholderText}fragment MarqueeDisplayAdConfigsFragment on TempoWM_GLASSWWWMarqueeDisplayAdConfigs{_rawConfigs ad{...DisplayAdFragment}}fragment DisplayAdFragment on Ad{...AdFragment adContent{type data{__typename...AdDataDisplayAdFragment}}}fragment AdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment AdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment SkylineDisplayAdConfigsFragment on TempoWM_GLASSWWWSkylineDisplayAdConfigs{_rawConfigs ad{...SkylineDisplayAdFragment}}fragment SkylineDisplayAdFragment on Ad{...SkylineAdFragment adContent{type data{__typename...SkylineAdDataDisplayAdFragment}}}fragment SkylineAdFragment on Ad{status moduleType platform pageId pageType storeId stateCode zipCode pageContext moduleConfigs adsContext adRequestComposite}fragment SkylineAdDataDisplayAdFragment on AdData{...on DisplayAd{json status}}fragment HorizontalChipModuleConfigsFragment on TempoWM_GLASSWWWHorizontalChipModuleConfigs{chipModuleSource:moduleSource chipModule{title url{linkText title clickThrough{type value}}}chipModuleWithImages{title url{linkText title clickThrough{type value}}image{alt clickThrough{type value}height src title width}}}fragment SkinnyBannerFragment on TempoWM_GLASSWWWSkinnyBannerConfigs{bannerType desktopBannerHeight bannerImage{src title alt}mobileBannerHeight mobileImage{src title alt}clickThroughUrl{clickThrough{value}}backgroundColor heading{title fontColor}subHeading{title fontColor}bannerCta{ctaLink{linkText clickThrough{value}}textColor ctaType}}fragment TileTakeOverProductFragment on TempoWM_GLASSWWWTileTakeOverProductConfigs{dwebSlots mwebSlots TileTakeOverProductDetails{pageNumber span dwebPosition mwebPosition title subtitle image{src alt}logoImage{src alt}backgroundColor titleTextColor subtitleTextColor tileCta{ctaLink{clickThrough{value}linkText title}ctaType ctaTextColor}}}\",\"variables\":{\"id\":\"\",\"affinityOverride\":\"default\",\"dealsId\":\"\",\"query\":\"\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"5427_133283_4720344\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"\",\"cat_id\":\"5427_133283_4720344\",\"prg\":\"desktop\",\"facet\":\"\"},\"fitmentFieldParams\":{\"powerSportEnabled\":true},\"fitmentSearchParams\":{\"id\":\"\",\"affinityOverride\":\"default\",\"dealsId\":\"\",\"query\":\"\",\"page\":1,\"prg\":\"desktop\",\"catId\":\"5427_133283_4720344\",\"facet\":\"\",\"sort\":\"best_match\",\"rawFacet\":\"\",\"seoPath\":\"\",\"ps\":40,\"ptss\":\"\",\"trsp\":\"\",\"beShelfId\":\"\",\"recall_set\":\"\",\"module_search\":\"\",\"min_price\":\"\",\"max_price\":\"\",\"storeSlotBooked\":\"\",\"additionalQueryParams\":{\"hidden_facet\":null,\"translation\":null},\"searchArgs\":{\"query\":\"\",\"cat_id\":\"5427_133283_4720344\",\"prg\":\"desktop\",\"facet\":\"\"},\"cat_id\":\"5427_133283_4720344\",\"_be_shelf_id\":\"\"},\"fetchMarquee\":true,\"fetchSkyline\":true,\"fetchSbaTop\":false,\"enablePortableFacets\":true,\"tenant\":\"WM_GLASS\",\"enableFacetCount\":true,\"pageType\":\"BrowsePage\"}}");
        writer.flush();
        writer.close();
        httpConn.getOutputStream().close();

        InputStream responseStream = httpConn.getResponseCode() / 100 == 2
                ? httpConn.getInputStream()
                : httpConn.getErrorStream();
        Scanner s = new Scanner(responseStream).useDelimiter("\\A");
        String response = s.hasNext() ? s.next() : "";

        JsonElement jsonElement = new JsonParser().parse(response);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(jsonElement);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
        });
        Object data = jsonMap.get("data");
        Map<String, Object> dataMap = objectMapper.convertValue(data, Map.class);
//        System.out.println(dataMap);

        // GETTING BRANDS

        Object contentLayout = dataMap.get("contentLayout");
        Map<String, Object> contentLayoutMap = objectMapper.convertValue(contentLayout, Map.class);

        Object modules = contentLayoutMap.get("modules");
        ArrayList<Object> modulesAL = objectMapper.convertValue(modules, ArrayList.class);

        Object chosenModule = modulesAL.get(4);
        Map<String, Object> moduleMap = objectMapper.convertValue(chosenModule, Map.class);

        Object configs = moduleMap.get("configs");
        Map<String, Object> configsMap = objectMapper.convertValue(configs, Map.class);

        Object topNavFacets = configsMap.get("topNavFacets");
        ArrayList<Object> topNavFacetsAL = objectMapper.convertValue(topNavFacets, ArrayList.class);

        Object chosenFacet = topNavFacetsAL.get(2);
        Map<String, Object> chosenFacetMap = objectMapper.convertValue(chosenFacet, Map.class);

        Object values = chosenFacetMap.get("values");
        ArrayList<Object> brandsAL = objectMapper.convertValue(values, ArrayList.class);

        HashMap<String, String> brands = new HashMap<String, String>();

        for (Object brand : brandsAL) {
            Map<String, String> brandInfo = objectMapper.convertValue(brand, Map.class);
            brands.put(brandInfo.get("name").toLowerCase(), brandInfo.get("name"));
        }

        // GET PRODUCTS
        Object search = dataMap.get("search");

        Map<String, Object> searchMap = objectMapper.convertValue(search, Map.class);
        Object searchResult = searchMap.get("searchResult");

        Map<String, Object> searchResultMap = objectMapper.convertValue(searchResult, Map.class);
        Object itemStacks = searchResultMap.get("itemStacks");

        ArrayList<Object> itemStacksAL = objectMapper.convertValue(itemStacks, ArrayList.class);
        Object firstInAL = itemStacksAL.get(0);

        Map<String, Object> itemsV2Map = objectMapper.convertValue(firstInAL, Map.class);
        Object itemsV2 = itemsV2Map.get("itemsV2");

        ArrayList<Object> itemsAL = objectMapper.convertValue(itemsV2, ArrayList.class);


        for (Object o : itemsAL) {

            HashMap<String, String> newProduct = new HashMap<>();

            Map<String, Object> item = objectMapper.convertValue(o, Map.class);

            // get id
            Object productID = item.get("usItemId");
            String productIDStr = objectMapper.convertValue(productID, String.class);
//			System.out.println("productID: " + productIDStr);
            products.put(productIDStr, newProduct);

            // get name
            Object name = item.get("name");
            String nameStr = objectMapper.convertValue(name, String.class).toLowerCase();
//			System.out.println("name: " + name);
            newProduct.put("name", nameStr);

            // get buyURL
            Object buyURL = item.get("canonicalUrl");
            String buyURLStr = objectMapper.convertValue(buyURL, String.class);
            int toIndex = buyURLStr.indexOf("?");
            if (toIndex <= buyURLStr.length() && toIndex >= 0) {
                buyURLStr = buyURLStr.substring(0, toIndex);
            }
//			System.out.println("buyURL: " + buyURLStr);
            newProduct.put("buyURL", buyURLStr);

            //get brand
            boolean foundBrand = false;

            for (String brand : brands.keySet()) {
                if (nameStr.contains(brand)) {
                    foundBrand = true;
//					System.out.println("brand: " + brands.get(brand));
                    newProduct.put("brand", brands.get(brand));
                    break;
                }
            }

            if (!foundBrand) {
                for (String brand : brands.keySet()) {
                    if (isBrand(nameStr, brand)) {
//						System.out.println("brand: " + brands.get(brand));
                        newProduct.put("brand", brands.get(brand));
                        break;
                    }
                }
            }

            // get availability
            Map<String, Object> itemAvailability = objectMapper.convertValue(item.get("availabilityStatusV2"), Map.class);
//			System.out.println("availability: " + itemAvailability.get("display"));
            Object inStockStatus = itemAvailability.get("display");
            String inStockStatusStr = objectMapper.convertValue(inStockStatus, String.class);
            String availabilityStr = "false";
            if (inStockStatusStr.equals("In stock")) {
                availabilityStr = "true";
            }
            newProduct.put("availability", availabilityStr);

            // get price
            Map<String, Object> itemPriceInfo = objectMapper.convertValue(item.get("priceInfo"), Map.class);
            if (itemPriceInfo.get("currentPrice") != null) {
                Map<String, Object> itemCurPrice = objectMapper.convertValue(itemPriceInfo.get("currentPrice"), Map.class);
                Object price = itemCurPrice.get("price");
                Double priceDouble = objectMapper.convertValue(price, Double.class);
                String priceStr = objectMapper.convertValue(priceDouble, String.class);
//				System.out.println("price: " + itemCurPrice.get("price"));
                newProduct.put("price", priceStr);
            } else if (itemPriceInfo.get("priceRange") != null) {
                Map<String, Object> itemPriceRange = objectMapper.convertValue(itemPriceInfo.get("priceRange"), Map.class);
                Object minPrice = itemPriceRange.get("minPrice");
                Double minPriceDouble = objectMapper.convertValue(minPrice, Double.class);
                Object maxPrice = itemPriceRange.get("maxPrice");
                Double maxPriceDouble = objectMapper.convertValue(maxPrice, Double.class);
                if (minPrice != maxPrice) {
//					System.out.println("min price: " + minPrice);
//					System.out.println("max price: " + maxPrice);
                    String maxPriceStr = objectMapper.convertValue(maxPrice, String.class);
                    newProduct.put("price", maxPriceStr);
                } else {
//					System.out.println("price: " + minPrice);
                    String maxPriceStr = objectMapper.convertValue(maxPrice, String.class);
                    newProduct.put("price", maxPriceStr);
                }
            }

            // get thumbnail image URL
            Object imageInfo = item.get("imageInfo");
            Map<String, Object> imageInfoMap = objectMapper.convertValue(imageInfo, Map.class);
            Object thumbnailURL = imageInfoMap.get("thumbnailUrl");
            String thumbnailURLStr = objectMapper.convertValue(thumbnailURL, String.class);
            newProduct.put("imageURL", thumbnailURLStr);
        }
        return products;
    }

    /////////////////////////////

//        for (Object o : itemsAL) {
//            // get name
//            Map<String,Object> item = objectMapper.convertValue(o, Map.class);
//            Object name = item.get("name");
//            String nameStr = objectMapper.convertValue(name, String.class);
//            System.out.println(name);
//
//            //get brand
//            for (String brand : brands.keySet()) {
////                System.out.println(brand);
//                if (isBrand(nameStr, brand)) {
//                    System.out.println(brand);
//                    break;
//                }
//            }
//
//            // get availability
//            Map<String,Object> itemAvailability = objectMapper.convertValue(item.get("availabilityStatusV2"), Map.class);
//            System.out.println(itemAvailability.get("display"));
//
//            // get price
//            Map<String,Object> itemPriceInfo = objectMapper.convertValue(item.get("priceInfo"), Map.class);
//            if (itemPriceInfo.get("currentPrice") != null) {
//                Map<String,Integer> itemCurPrice = objectMapper.convertValue(itemPriceInfo.get("currentPrice"), Map.class);
//                System.out.println(itemCurPrice.get("price"));
//            } else if (itemPriceInfo.get("priceRange") != null) {
//                Map<String,Integer> itemPriceRange = objectMapper.convertValue(itemPriceInfo.get("priceRange"), Map.class);
//                System.out.println(itemPriceRange.get("minPrice"));
//                System.out.println(itemPriceRange.get("maxPrice"));
//            }
//
//            // get thumbnail image URL
//            Object imageInfo = item.get("imageInfo");
//            Map<String,Object> imageInfoMap = objectMapper.convertValue(imageInfo, Map.class);
//            Object thumbnailURL = imageInfoMap.get("thumbnailUrl");
//            System.out.println(thumbnailURL);
//
//
//            System.out.println("--------------------------------------------------------");
//        }
//}
public static HashMap<String, HashMap> targetBabyFormula() throws IOException {
    HashMap<String, HashMap> productsInfo = new HashMap<>();
    ArrayList<String> productsTCINs = new ArrayList<>();

//    String query_param = query_str.replace(" ", "+");
    String query_param = "baby+formula";
    String zip_code = "43065";
    String latitude = "40.142"; // either user location or get store's lat long
    String longitude = "-83.094"; // either user location or get store's lat long
    String state = "OH";

    // write something to fetch this info based on user info from store id table in db
    String store_id = "2851";
    String closest_stores = "2851%2C666%2C1236%2C1978%2C1969"; // ArrayList of 5 closest store IDs, join them with %2C
    String store_name = "Powell".replace(" ", "%20");

    URL url1 = new URL("https://redsky.target.com/redsky_aggregations/v1/web/plp_search_v1?key=9f36aeafbe60771e321a7cc95a78140772ab3e96&category=5xtkh&channel=WEB&count=24&default_purchasability_filter=true&include_sponsored=true&offset=0&page=%2Fc%2F5xtkh&platform=desktop&pricing_store_id=" + store_id + "&scheduled_delivery_store_id=" + store_id + "&store_ids=" + closest_stores + "&useragent=Mozilla%2F5.0+%28Macintosh%3B+Intel+Mac+OS+X+10_15_7%29+AppleWebKit%2F537.36+%28KHTML%2C+like+Gecko%29+Chrome%2F103.0.0.0+Safari%2F537.36&visitor_id=0182A834AB4902018CB76982CD0AAFFC&zip=" + zip_code);
    HttpURLConnection httpConn1 = (HttpURLConnection) url1.openConnection();
    httpConn1.setRequestMethod("GET");

    httpConn1.setRequestProperty("authority", "redsky.target.com");
    httpConn1.setRequestProperty("accept", "application/json");
    httpConn1.setRequestProperty("accept-language", "en-US,en;q=0.9");
    httpConn1.setRequestProperty("cache-control", "no-cache");
    httpConn1.setRequestProperty("cookie", "TealeafAkaSid=AA2qZCey8WPhjd_8dDGOjZBdi4-COY-m; visitorId=0182A834AB4902018CB76982CD0AAFFC; sapphire=1; UserLocation=" + zip_code + "|" + latitude + "|" + longitude + "|" + state + "|US; egsSessionId=ad771178-a223-4dbe-b008-1f488f6b321d; accessToken=eyJraWQiOiJlYXMyIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiI0NDg5ZDJjMi1kNWY0LTQ5NTgtOGNkNS1hMDIxMzRmMjQ1NDgiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA3NjU4MDEsImlhdCI6MTY2MDY3OTQwMSwianRpIjoiVEdULmRiYzNlYWVlMWMxZDQ4YWM4MDY0YzhjZDZjZGUzZjMyLWwiLCJza3kiOiJlYXMyIiwic3V0IjoiRyIsImRpZCI6IjNlY2NkZTY0MjM4NjEyMzE0ODNkZTIyYTFjYjBjOWI5YzgzN2IzNThkNGUzOTY0MjQ5YjZjNGM0NmRkZTc0NmIiLCJzY28iOiJlY29tLm5vbmUsb3BlbmlkIiwiY2xpIjoiZWNvbS13ZWItMS4wLjAiLCJhc2wiOiJMIn0.f78pILkHPZKI4R63MWt8uE45LYzIiu4aOUwQbyLNgBBAa6tR1OvjuYZ6inVip1I1rrpqqc2Sh-CJu2GcUpntxYwoOkusOIsOyCBDjxfiWMRrZFy5kqjB714DtU1R_A42wm9nJ_xF2kDTRqtVH24qnMw-stIhnj2gzNlekpnRaJHzxSpO0YpjOi4ODNkLSZsn_-6h0slx_Pt9cxk0Efw0pNgHMkcZMlOan42M1AByOWJd871YzRk0GaStQCOhKi89H24jtLL2_NKY9rqccMtYD_pr9815m3Nd9xrXKnoV9fCHcpjEVmBAbbTb4yI_k_1ERNIXnDhrMUDv7pv1ATIFeA; idToken=eyJhbGciOiJub25lIn0.eyJzdWIiOiI0NDg5ZDJjMi1kNWY0LTQ5NTgtOGNkNS1hMDIxMzRmMjQ1NDgiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA3NjU4MDEsImlhdCI6MTY2MDY3OTQwMSwiYXNzIjoiTCIsInN1dCI6IkciLCJjbGkiOiJlY29tLXdlYi0xLjAuMCIsInBybyI6eyJmbiI6bnVsbCwiZW0iOm51bGwsInBoIjpmYWxzZSwibGVkIjpudWxsLCJsdHkiOmZhbHNlfX0.; refreshToken=8WaWo1FkOwUlQR0N8cWmcohigl3_puQ7C1Y11OV9Q8kpLX9lHgHdglL8ZQ0jQ9tpPw2I9kkQeOfQtpCJgLQhgA; __gads=ID=ddcb7a7fb07bf430:T=1660679401:S=ALNI_MbBY9l9mMReHROd2OESk1AIf5l-Kw; __gpi=UID=000007e931219333:T=1660679401:RT=1660679401:S=ALNI_MbSMgmPeS3kZEcXCUQVm48IX7o5eA; ffsession={%22sessionHash%22:%2217faa2889a8dcf1660679400791%22%2C%22prevPageName%22:%22home%20page%22%2C%22prevPageType%22:%22home%20page%22%2C%22prevPageUrl%22:%22https://www.target.com/%22%2C%22sessionHit%22:1}; ci_pixmgr=other; _mitata=MTAzMDkwNmM0MDE2MzUwNDZkZWE1MDI5YWM5NTgxYTVjYTUzNDU5OTlhNGJiYWRjOWJjNzYzMmQ5MDI0YmQ1Mw==_/@#/1660679461_/@#/ce3CLn8d1XeqGrsQ_/@#/MjViNzJkYmQyZjQ5ZTJkM2NhYmEzMTAyYzQ1YWQ2NjBkMWY0NDBkMjIxMmUwMTI5YTRlM2JkZDY2NTY4ODY4MA==_/@#/000; _uetsid=9de61fe01d9c11ed91c677ca6f35ae04; _uetvid=9de650501d9c11ed8fad434bdecf9a58; _gcl_au=1.1.2028319006.1660679402; fiatsCookie=DSI_" + store_id + "|DSN_" + store_name + "|DSZ_" + zip_code);
    httpConn1.setRequestProperty("origin", "https://www.target.com");
    httpConn1.setRequestProperty("pragma", "no-cache");
    httpConn1.setRequestProperty("referer", "https://www.target.com/c/formula-nursing-feeding-baby/-/N-5xtkh?lnk=snav_rd_formula");
    httpConn1.setRequestProperty("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
    httpConn1.setRequestProperty("sec-ch-ua-mobile", "?0");
    httpConn1.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
    httpConn1.setRequestProperty("sec-fetch-dest", "empty");
    httpConn1.setRequestProperty("sec-fetch-mode", "cors");
    httpConn1.setRequestProperty("sec-fetch-site", "same-site");
    httpConn1.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");

    InputStream responseStream1 = httpConn1.getResponseCode() / 100 == 2
            ? httpConn1.getInputStream()
            : httpConn1.getErrorStream();
    Scanner s1 = new Scanner(responseStream1).useDelimiter("\\A");
    String response1 = s1.hasNext() ? s1.next() : "";
//    System.out.println(response1);

    JsonElement jsonElement1 = new JsonParser().parse(response1);
    Gson gson1 = new GsonBuilder().setPrettyPrinting().create();
    String json1 = gson1.toJson(jsonElement1);

    ObjectMapper objectMapper = new ObjectMapper();
    Map<String, Object> jsonMap1 = objectMapper.readValue(json1, new TypeReference<Map<String, Object>>() {
    });
    Object data1 = jsonMap1.get("data");

    Map<String, Object> dataMap1 = objectMapper.convertValue(data1, Map.class);
    Object search1 = dataMap1.get("search");

    Map<String, Object> searchMap1 = objectMapper.convertValue(search1, Map.class);
    Object products1 = searchMap1.get("products");

    ArrayList<Object> productsAL1 = objectMapper.convertValue(products1, ArrayList.class);
    for (Object product : productsAL1) {
        HashMap<String, String> productInfo = new HashMap<>();

        Map<String, Object> productMap = objectMapper.convertValue(product, Map.class);

        // get title
        Object item = productMap.get("item");
        Map<String, Object> itemMap = objectMapper.convertValue(item, Map.class);
        Object productDescription = itemMap.get("product_description");
        Map<String, Object> productDescriptionMap = objectMapper.convertValue(productDescription, Map.class);
        Object title = productDescriptionMap.get("title");
        String titleStr = objectMapper.convertValue(title, String.class);
        productsInfo.put(titleStr, productInfo);

        // get tcin
        Object productTCIN = productMap.get("tcin");
        String productTCINStr = objectMapper.convertValue(productTCIN, String.class);
        productsTCINs.add(productTCINStr);

        // get brand name
        if (itemMap.get("primary_brand") != null) {
            Object brandInfo = itemMap.get("primary_brand");
            Map<String, Object> brandInfoMap = objectMapper.convertValue(brandInfo, Map.class);
            Object brandName = brandInfoMap.get("name");
            String brandNameStr = objectMapper.convertValue(brandName, String.class);
            productInfo.put("brand", brandNameStr);
        }

        // get price
        Object price = productMap.get("price");
        Map<String, Object> priceMap = objectMapper.convertValue(price, Map.class);
        Object curPrice = priceMap.get("formatted_current_price");
        String curPriceStr = objectMapper.convertValue(curPrice, String.class);
        productInfo.put("price", curPriceStr);

        // get primary image URL
        Object enrichment = itemMap.get("enrichment");
        Map<String, Object> enrichmentMap = objectMapper.convertValue(enrichment, Map.class);
        Object images = enrichmentMap.get("images");
        Map<String, Object> imagesMap = objectMapper.convertValue(images, Map.class);
        Object primaryImage = imagesMap.get("primary_image_url");
        String primaryImageStr = objectMapper.convertValue(primaryImage, String.class);
        productInfo.put("imageURL", primaryImageStr);
    }

    // CONVERT ARRAYLIST OF PRODUCT TCINS INTO STRING
    String productsTCINsStr = String.join("%2C", productsTCINs);

    // TO GET AVAILABILITY INFO AND BUY URL FROM FULFILLMENT DATA
    URL url = new URL("https://redsky.target.com/redsky_aggregations/v1/web_platform/product_summary_with_fulfillment_v1?key=9f36aeafbe60771e321a7cc95a78140772ab3e96&tcins=" + productsTCINsStr + "&store_id" + store_id + "&zip=" + zip_code + "&state=" + state + "&latitude=" + latitude + "&longitude=" + longitude + "&scheduled_delivery_store_id=" + store_id + "&required_store_id=" + store_id + "&has_required_store_id=true&channel=WEB&page=%2Fs%2F" + query_param);
    HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
    httpConn.setRequestMethod("GET");

    httpConn.setRequestProperty("authority", "redsky.target.com");
    httpConn.setRequestProperty("accept", "application/json");
    httpConn.setRequestProperty("accept-language", "en-US,en;q=0.9");
    httpConn.setRequestProperty("cache-control", "no-cache");
    httpConn.setRequestProperty("cookie", "TealeafAkaSid=tW3sNScU-pf8QSWSBc4Dx8IhHnSIJ3G_; visitorId=0182A3D3FCBE0201845B982AB4154957; sapphire=1; UserLocation=" + zip_code + "|" + latitude + "|" + longitude + "|" + state + "|US; egsSessionId=528e5218-c05f-4434-b220-0970933cd8dd; accessToken=eyJraWQiOiJlYXMyIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiIzMjBmNzg4MC04NGZlLTQ1NWItYWFkYi1mYjg1OGU1NWRhYTgiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA2OTIzNTYsImlhdCI6MTY2MDYwNTk1NiwianRpIjoiVEdULmNjNWUxYzUyNTUwNDRhNTFiY2Y4YzFkZDk2MGNiNDVmLWwiLCJza3kiOiJlYXMyIiwic3V0IjoiRyIsImRpZCI6IjQ4NTY1YWZiYWE2YzFiYjVmNjNkMTQ0ZTdmNjhlODYxMTU5YmQzZTczY2I0MjZjZTFjNTliOTI5MzI3ZDk5NGYiLCJzY28iOiJlY29tLm5vbmUsb3BlbmlkIiwiY2xpIjoiZWNvbS13ZWItMS4wLjAiLCJhc2wiOiJMIn0.QRCI69lR_mSLIsnADDBYTVzlViOX5DNO4d-QNDbExhC0mFmEpn0mUED8L1g-XbB-_RLd8FH3ea2oC6fiEvb-VDOArr23IxklKnwTczlFx-8ukshGWpSrLBLmzr85sk0NO61Gb6wwUNcKfylAfO2ddm6oAa0b_wKehZr4N6oP2QbdKAi__OQXyMftyftLBRAUSvpOM9_zXB1UKysyqKK9nBTyBKi-SjgEsQNweBBwkZwbGBpqMcp7Urt6s8xCB3ciwjOQSG29rzYn_6OCdwvZVusvTS6ND6K2uS7Ut8gBLstl37MDGkXpIALAsD_6sVGyFXjiVogykPhGaWLhuhUuQw; idToken=eyJhbGciOiJub25lIn0.eyJzdWIiOiIzMjBmNzg4MC04NGZlLTQ1NWItYWFkYi1mYjg1OGU1NWRhYTgiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA2OTIzNTYsImlhdCI6MTY2MDYwNTk1NiwiYXNzIjoiTCIsInN1dCI6IkciLCJjbGkiOiJlY29tLXdlYi0xLjAuMCIsInBybyI6eyJmbiI6bnVsbCwiZW0iOm51bGwsInBoIjpmYWxzZSwibGVkIjpudWxsLCJsdHkiOmZhbHNlfX0.; refreshToken=YOQvrhY0Rc1OTlqCUPaQmCAQYYDyJl1hHAp2OyABjfmzyRYUPgVA0XYZ3iY03WMIVq1JWQ3U8UGbLuno1pzIog; __gads=ID=fcacfef16f82cade-22572175a1d400ad:T=1660605956:S=ALNI_MaeY6UxbBLJ7EheMAjpRt-PO6D3Pw; __gpi=UID=000007e4b59ef825:T=1660605956:RT=1660605956:S=ALNI_MbwRsUmwavf2MpuVsBXPiDrT7vBBw; ffsession={%22sessionHash%22:%22a38d7281033411660605955675%22%2C%22prevPageName%22:%22home%20page%22%2C%22prevPageType%22:%22home%20page%22%2C%22prevPageUrl%22:%22https://www.target.com/%22%2C%22sessionHit%22:1}; ci_pixmgr=other; _uetsid=9d41daf01cf111ed8a934fc144aa43e9; _uetvid=9d41d7f01cf111edb1e87102d88101e0; _gcl_au=1.1.949956296.1660605957; fiatsCookie=DSI_" + store_id + "|DSN_" + store_name + "|DSZ_" + zip_code + "; _mitata=ZTJlZmJiYjVkYjUyOGQzZjM0YTM1NmVmZWY4ZTU2YzFiMzlkNzQ1OGZhOGZiNWUyZjc2YzkxNzhhNTkyNjY1ZQ==_/@#/1660606098_/@#/cyk4HguDDTbQp3kn_/@#/Y2EzMWVlZmQwMjA0NWVjOWZhMmRmMTgyMjc0M2MyMDYwZWRmOGMwY2M3ZTdkMzc3MzI3ZDk2ZDI2NDZmMDBkNg==_/@#/000");
    httpConn.setRequestProperty("origin", "https://www.target.com");
    httpConn.setRequestProperty("pragma", "no-cache");
    httpConn.setRequestProperty("referer", "https://www.target.com/s?searchTerm=" + query_param);
    httpConn.setRequestProperty("sec-ch-ua", "\".Not/A)Brand\";v=\"99\", \"Google Chrome\";v=\"103\", \"Chromium\";v=\"103\"");
    httpConn.setRequestProperty("sec-ch-ua-mobile", "?0");
    httpConn.setRequestProperty("sec-ch-ua-platform", "\"macOS\"");
    httpConn.setRequestProperty("sec-fetch-dest", "empty");
    httpConn.setRequestProperty("sec-fetch-mode", "cors");
    httpConn.setRequestProperty("sec-fetch-site", "same-site");
    httpConn.setRequestProperty("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/103.0.0.0 Safari/537.36");

    InputStream responseStream = httpConn.getResponseCode() / 100 == 2
            ? httpConn.getInputStream()
            : httpConn.getErrorStream();
    Scanner s = new Scanner(responseStream).useDelimiter("\\A");
    String response = s.hasNext() ? s.next() : "";

    JsonElement jsonElement = new JsonParser().parse(response);
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    String json = gson.toJson(jsonElement);

    Map<String, Object> jsonMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
    });
    Object data = jsonMap.get("data");

    Map<String, ArrayList<Object>> dataMap = objectMapper.convertValue(data, Map.class);
    ArrayList<Object> productSummaries = dataMap.get("product_summaries");

    for (Object product : productSummaries) {
        Map<String, Object> productMap = objectMapper.convertValue(product, Map.class);


        Object productItem = productMap.get("item");
        Map<String, Object> productItemMap = objectMapper.convertValue(productItem, Map.class);


        // get title
        Object productDescription = productItemMap.get("product_description");
        Map<String, Object> productDescriptionMap = objectMapper.convertValue(productDescription, Map.class);
        Object productTitle = productDescriptionMap.get("title");
//			System.out.println(productTitle);
        String productTitleStr = objectMapper.convertValue(productTitle, String.class);
        HashMap<String, String> selectedProduct = null;
        if (productsInfo.get(productTitleStr) != null) {
            selectedProduct = productsInfo.get(productTitleStr);
        } else {
            System.out.println("NOT FOUND! CHECK FOR ERRORS!");
            System.out.println(productTitleStr);
            continue;
        }

        // get brand

        // get purchase url
        Object enrichment = productItemMap.get("enrichment");
        Map<String, String> enrichmentMap = objectMapper.convertValue(enrichment, Map.class);
        String buyURL = enrichmentMap.get("buy_url");
        selectedProduct.put("buyURL", buyURL);

        // get image url

        // get price
        Object productPrice = productMap.get("price");
        Map<String, Double> productPriceMap = objectMapper.convertValue(productPrice, Map.class);
        Double productRTP = productPriceMap.get("current_retail");
        selectedProduct.put("price", productRTP.toString());

        // get availability
        Object productFulfillment = productMap.get("fulfillment");
        Map<String, Object> productFulfillmenteMap = objectMapper.convertValue(productFulfillment, Map.class);
        // all store locations
        boolean productInStock = objectMapper.convertValue(productFulfillmenteMap.get("is_out_of_stock_in_all_store_locations"), boolean.class);
        String inStockAtAllStores = objectMapper.convertValue(!(productInStock), String.class);
        selectedProduct.put("allStoresAvailability", inStockAtAllStores);
//			// primary store location
//			Object storeOptions = productFulfillmenteMap.get("store_options");
//			ArrayList<Object> storeOptionsAL = objectMapper.convertValue(storeOptions, ArrayList.class);
//			Object chosenStoreOption = storeOptionsAL.get(0);
//			Map<String, Object> chosenStoreOptionMap = objectMapper.convertValue(chosenStoreOption, Map.class);
//			Object inStoreOnly = chosenStoreOptionMap.get("in_store_only");
//			Map<String, String> inStoreOnlyMap = objectMapper.convertValue(inStoreOnly, Map.class);
//			String inStoreOnlyStatus = inStoreOnlyMap.get("availability_status");
//			Object orderPickup = chosenStoreOptionMap.get("order_pickup");
//			Map<String, String> orderPickupMap = objectMapper.convertValue(orderPickup, Map.class);
//			String orderPickupStatus = orderPickupMap.get("availability_status");
//			if (inStoreOnlyStatus.equals("IN_STOCK") || orderPickupStatus.equals("IN_STOCK")) {
//				selectedProduct.put("closestStoreAvailability", "true");
//			} else {
//				selectedProduct.put("closestStoreAvailability", "false");
//			}
//			System.out.println("-----------------------------------------------------------------------");
    }

    return productsInfo;
}


    public static boolean isBrand(String name, String brand) {
        String[] words = name.split(" ");
        for (String word : words) {
            if (word.length() > 1 && (oneLetterAway(word, brand) || oneLetterAway(brand, word))) {
                return true;
            }
        }
        return false;
    }

    public static boolean oneLetterAway(String word1, String word2) {
        if (word1.equals(word2.substring(1))) {
            return true;
        } else if (word1.equals(word2.substring(0, word2.length() - 1))) {
            return true;
        } else {
            for (int i = 1; i < word2.length() - 1; i++) {
                if (word1.contains(word2.substring(0, i) + word2.substring(i + 1))) {
                    return true;
                }
            }
        }
        return false;
    }

}
