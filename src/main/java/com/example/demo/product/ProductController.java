package com.example.demo.product;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    // maybe call the scraper script here?
    // postmapping / putmapping / deletemapping (we can call other methods this.xyz)
    // preferred: productService.addNewProduct(product) vs direct mapping method

    // AVOID!!! lololol - every 15 minutes? (need column to store last updated datetime?)

    // LET'S DO THIS ONE!!!
    // OR, clear everything and only put new stuff in there every get
    // might not need to save, dynamically return from scraper as list of Products
    @GetMapping
//    public List<Product> getProducts(@RequestBody HashMap<String, String> queryParams) {
    public List<Product> getProducts() {

        HashMap<String, HashMap> targetRes;
        List<Product> products = new ArrayList<>();
//
//        String searchStr = queryParams.get("searchStr");
        String searchStr = "65%20TV";
//
        try {
             targetRes = target(searchStr);
        } catch(IOException e) {
            System.out.println(e);
            return new ArrayList<>();
        }

        List<HashMap> productsList = new ArrayList<>();

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

    public static HashMap<String, HashMap> target(String query_param) throws IOException {

        HashMap<String, HashMap> productsInfo = new HashMap<>();
        ArrayList<String> productsTCINs = new ArrayList<>();

        // TO GET TITLE, BRAND, IMAGEURL, PRICE INFO
//        String query_param = "baby diapers".replace(" ", "%20");
        String store_id = "2851";
        String closest_stores = "2851%2C666%2C1236%2C1978%2C1969";
        String zip_code = "43065";
        String latitude = "40.142";
        String longitude = "-83.094";
        String state = "OH";
        String store_name = "Powell".replace(" ", "%20");

        URL url1 = new URL("https://redsky.target.com/redsky_aggregations/v1/web/plp_search_v1?key=9f36aeafbe60771e321a7cc95a78140772ab3e96&channel=WEB&count=24&default_purchasability_filter=true&include_sponsored=true&keyword=" + query_param + "&offset=0&page=%2Fs%2F" + query_param + "&platform=desktop&pricing_store_id=" + store_id + "&scheduled_delivery_store_id=" + store_id + "&store_ids=" + closest_stores + "&useragent=Mozilla%2F5.0+%28Macintosh%3B+Intel+Mac+OS+X+10_15_7%29+AppleWebKit%2F537.36+%28KHTML%2C+like+Gecko%29+Chrome%2F103.0.0.0+Safari%2F537.36&visitor_id=0182931DD1530201807DA42E454E876C&zip=" + zip_code);
        HttpURLConnection httpConn1 = (HttpURLConnection) url1.openConnection();
        httpConn1.setRequestMethod("GET");

        httpConn1.setRequestProperty("authority", "redsky.target.com");
        httpConn1.setRequestProperty("accept", "application/json");
        httpConn1.setRequestProperty("accept-language", "en-US,en;q=0.9");
        httpConn1.setRequestProperty("cache-control", "no-cache");
        httpConn1.setRequestProperty("cookie", "TealeafAkaSid=XPo04Lyra571aX8zsO4BkxEZJwg1IhEA; visitorId=0182931DD1530201807DA42E454E876C; sapphire=1; UserLocation=" + zip_code + "|" + latitude + "|" + longitude + "|" + state + "|US; egsSessionId=74636cb7-a895-4011-ba7b-b1585c9ecc17; accessToken=eyJraWQiOiJlYXMyIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiIyYTExOWM5OS0zN2M4LTRhYjctODkyMS0wNGU0NDk0YmNiOTEiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA0MTE5ODIsImlhdCI6MTY2MDMyNTU4MiwianRpIjoiVEdULjJiZTA3M2ExOTRmZDRhNGRiMzMyMWU2MTdiMzBkNjA3LWwiLCJza3kiOiJlYXMyIiwic3V0IjoiRyIsImRpZCI6ImQ1M2RlOWQyZWViOWQ2NDQ0N2EzZDRjMmEyNzUyMWVkYjkwMDgxMTRjNmY1NmYxODU5NzQyN2Q0MTQzZTRlNjciLCJzY28iOiJlY29tLm5vbmUsb3BlbmlkIiwiY2xpIjoiZWNvbS13ZWItMS4wLjAiLCJhc2wiOiJMIn0.vZ15BEok1eHRcjrF4BS5vYtvLtMXQn_4fJk_iOtO4cu3Mq2QQbngTT8GYBmcpMjh4EtnNvhetqfLgX_9aUDbkUaE9M5owfmOIlkSwtbBjtfMO3W23GJjpNaSJc_OUpdmkMsm501YwBcn0X-zWB0IflxrQkKio1WwkfYHiYRTTmiPqlrFMgfRM6wYJAcD0XxwVyLUwF0-HRlVbFOO6_wVLMv_1AJ9jc2IYevo-PW4GrIYd8gMEBn7egFfwovxCz0KzLUTzSZU8GyWySrLPHkFo4A30BvGQEjAd2wTNW2NfPTKYV-7suJaPr6iXeqF7ZJqX7R10GjDixHM6_rdtINDpA; idToken=eyJhbGciOiJub25lIn0.eyJzdWIiOiIyYTExOWM5OS0zN2M4LTRhYjctODkyMS0wNGU0NDk0YmNiOTEiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA0MTE5ODIsImlhdCI6MTY2MDMyNTU4MiwiYXNzIjoiTCIsInN1dCI6IkciLCJjbGkiOiJlY29tLXdlYi0xLjAuMCIsInBybyI6eyJmbiI6bnVsbCwiZW0iOm51bGwsInBoIjpmYWxzZSwibGVkIjpudWxsLCJsdHkiOmZhbHNlfX0.; refreshToken=CO85uM0VWY02spIC67J3xCDwrJY7rfmTHdm9HkW1NV_GfVjfHm6ILFYDSoG6b9DTFXQfgyk2z7C9dfhv5KytHg; __gads=ID=9368b73264399b1f:T=1660325582:S=ALNI_MbEA7Zhd_F_fSqgKI_mMyTx3UPCKg; __gpi=UID=000007d27b4acfcc:T=1660325582:RT=1660325582:S=ALNI_Maegt2baFi6rDfQs2-rWz40X7V1pw; ffsession={%22sessionHash%22:%22805435c5f35b21660325581765%22%2C%22prevPageName%22:%22home%20page%22%2C%22prevPageType%22:%22home%20page%22%2C%22prevPageUrl%22:%22https://www.target.com/%22%2C%22sessionHit%22:1}; ci_pixmgr=other; _gcl_au=1.1.803936005.1660325583; _mitata=YzA0MGU4Njc2MTE5ZmZlOTk2YmJjZThjYmFkZmRjZWNmMTk1YjFkZDNiYmFkYmVmZDY5Mzc0MDg5OWYxOTE0Nw==_/@#/1660325643_/@#/cgOMrZ7prkr7jS7Z_/@#/OWI4MzdmMmI1ZmZlNDYxYjNiN2Y0ZTc2ZGM0OWIwZGJjNmRiODFiMzc4ZGEwYWVjM2FmY2RmZDE5YmZiZDAzZQ==_/@#/000; _uetsid=d17b95401a6411eda234e96874e82e16; _uetvid=d17bd5401a6411ed89da29522b1058aa; fiatsCookie=DSI_" + store_id + "|DSN_" + store_name + "|DSZ_" + zip_code);
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
        Map<String, Object> jsonMap1 = objectMapper.readValue(json1, new TypeReference<Map<String, Object>>(){});
        Object data1 = jsonMap1.get("data");

//		System.out.println(data1);

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
            Object brandInfo = itemMap.get("primary_brand");
            Map<String, Object> brandInfoMap = objectMapper.convertValue(brandInfo, Map.class);
            Object brandName = brandInfoMap.get("name");
            String brandNameStr = objectMapper.convertValue(brandName, String.class);
            productInfo.put("brand", brandNameStr);

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
        URL url = new URL("https://redsky.target.com/redsky_aggregations/v1/web_platform/product_summary_with_fulfillment_v1?key=9f36aeafbe60771e321a7cc95a78140772ab3e96&tcins=" + productsTCINsStr + "&store_id"+ store_id +"&zip=" + zip_code + "&state=" + state + "&latitude=" + latitude + "&longitude=" + longitude + "&scheduled_delivery_store_id=" + store_id + "&required_store_id=" + store_id + "&has_required_store_id=true&channel=WEB&page=%2Fs%2F" + query_param);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setRequestMethod("GET");

        httpConn.setRequestProperty("authority", "redsky.target.com");
        httpConn.setRequestProperty("accept", "application/json");
        httpConn.setRequestProperty("accept-language", "en-US,en;q=0.9");
        httpConn.setRequestProperty("cache-control", "no-cache");
        httpConn.setRequestProperty("cookie", "TealeafAkaSid=XPo04Lyra571aX8zsO4BkxEZJwg1IhEA; visitorId=0182931DD1530201807DA42E454E876C; sapphire=1; UserLocation=" + zip_code + "|" + latitude + "|" + longitude + "|" + state + "|US; egsSessionId=74636cb7-a895-4011-ba7b-b1585c9ecc17; accessToken=eyJraWQiOiJlYXMyIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiIyYTExOWM5OS0zN2M4LTRhYjctODkyMS0wNGU0NDk0YmNiOTEiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA0MTE5ODIsImlhdCI6MTY2MDMyNTU4MiwianRpIjoiVEdULjJiZTA3M2ExOTRmZDRhNGRiMzMyMWU2MTdiMzBkNjA3LWwiLCJza3kiOiJlYXMyIiwic3V0IjoiRyIsImRpZCI6ImQ1M2RlOWQyZWViOWQ2NDQ0N2EzZDRjMmEyNzUyMWVkYjkwMDgxMTRjNmY1NmYxODU5NzQyN2Q0MTQzZTRlNjciLCJzY28iOiJlY29tLm5vbmUsb3BlbmlkIiwiY2xpIjoiZWNvbS13ZWItMS4wLjAiLCJhc2wiOiJMIn0.vZ15BEok1eHRcjrF4BS5vYtvLtMXQn_4fJk_iOtO4cu3Mq2QQbngTT8GYBmcpMjh4EtnNvhetqfLgX_9aUDbkUaE9M5owfmOIlkSwtbBjtfMO3W23GJjpNaSJc_OUpdmkMsm501YwBcn0X-zWB0IflxrQkKio1WwkfYHiYRTTmiPqlrFMgfRM6wYJAcD0XxwVyLUwF0-HRlVbFOO6_wVLMv_1AJ9jc2IYevo-PW4GrIYd8gMEBn7egFfwovxCz0KzLUTzSZU8GyWySrLPHkFo4A30BvGQEjAd2wTNW2NfPTKYV-7suJaPr6iXeqF7ZJqX7R10GjDixHM6_rdtINDpA; idToken=eyJhbGciOiJub25lIn0.eyJzdWIiOiIyYTExOWM5OS0zN2M4LTRhYjctODkyMS0wNGU0NDk0YmNiOTEiLCJpc3MiOiJNSTYiLCJleHAiOjE2NjA0MTE5ODIsImlhdCI6MTY2MDMyNTU4MiwiYXNzIjoiTCIsInN1dCI6IkciLCJjbGkiOiJlY29tLXdlYi0xLjAuMCIsInBybyI6eyJmbiI6bnVsbCwiZW0iOm51bGwsInBoIjpmYWxzZSwibGVkIjpudWxsLCJsdHkiOmZhbHNlfX0.; refreshToken=CO85uM0VWY02spIC67J3xCDwrJY7rfmTHdm9HkW1NV_GfVjfHm6ILFYDSoG6b9DTFXQfgyk2z7C9dfhv5KytHg; __gads=ID=9368b73264399b1f:T=1660325582:S=ALNI_MbEA7Zhd_F_fSqgKI_mMyTx3UPCKg; __gpi=UID=000007d27b4acfcc:T=1660325582:RT=1660325582:S=ALNI_Maegt2baFi6rDfQs2-rWz40X7V1pw; ci_pixmgr=other; _gcl_au=1.1.803936005.1660325583; fiatsCookie=DSI_" + store_id + "|DSN_" + store_name + "|DSZ_" + zip_code + "; ffsession={%22sessionHash%22:%22805435c5f35b21660325581765%22%2C%22prevPageName%22:%22home%20page%22%2C%22prevPageType%22:%22home%20page%22%2C%22prevPageUrl%22:%22https://www.target.com/%22%2C%22sessionHit%22:4%2C%22prevSearchTerm%22:%22" + query_param + "%22}; _uetsid=d17b95401a6411eda234e96874e82e16; _uetvid=d17bd5401a6411ed89da29522b1058aa; _mitata=NzhlZGE0NTQzNjU0NTU4ZWRhYjdmMzI1MGQ1ZDgwNzRmNzc5N2NiOTY2YmNlYzMzNjE2MWNhZWQwNmY0ZDA0YQ==_/@#/1660327519_/@#/cgOMrZ7prkr7jS7Z_/@#/ZmE0Njg0MzQ2NDVjNWM3MWMxMDdlNDI2ZmQxZmJhNDk5OTU5M2FlMzllMzMyYjkwYTc1MTNiZTMzMzMyODk4Mg==_/@#/000");
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

        Map<String, Object> jsonMap = objectMapper.readValue(json, new TypeReference<Map<String, Object>>(){});
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
//        Gson gsonFinal = new GsonBuilder().setPrettyPrinting().create();
//        String jsonFinal = gson.toJson(productsInfo);
//        System.out.println(jsonFinal);
    }
}
